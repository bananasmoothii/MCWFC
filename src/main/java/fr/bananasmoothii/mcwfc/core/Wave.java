package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.VirtualSpace.ObjectWithCoordinates;
import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Coords;
import fr.bananasmoothii.mcwfc.core.util.Face;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Wave {
    /**
     * This needs to be a virtual space of {@link Sample} and not just {@link Set}<{@link Piece}>
     * because two {@link PieceNeighbors} are different while their centerpiece might be the same.
     * At the begenning, every node is equal to {@link #sample}, but they are not the same object: each {@link Sample}
     * in the {@link VirtualSpace} is unique.
     */
    private final VirtualSpace<Sample> wave;
    private final ImmutableSample sample;
    private final long seed;
    private final @Nullable PieceNeighbors defaultPiece;
    private final List<@NotNull PieceCollapseListener> pieceCollapseListeners = new ArrayList<>();
    public final boolean useModuloCoords;
    private boolean isCollapsed = false;
    private boolean hasImpossibleStates = false;

    public Wave(@NotNull Sample sample, @NotNull Bounds bounds, @Nullable Piece defaultPiece) {
        this(sample, bounds, defaultPiece, true);
    }

    public Wave(@NotNull Sample sample, @NotNull Bounds bounds, @Nullable Piece defaultPiece, boolean useModuloCoords) {
        this(sample, bounds, defaultPiece, useModuloCoords, ThreadLocalRandom.current().nextLong());
    }

    /**
     * @param defaultPiece if not null, instead of throwing a {@link GenerationFailedException} when there is a piece
     *                     with an entropy of 0, it will be replaced by this piece.
     */
    public Wave(@NotNull Sample sample, @NotNull Bounds bounds, @Nullable Piece defaultPiece, boolean useModuloCoords, long seed) {
        wave = new VirtualSpace<>(bounds);
        this.sample = sample.immutable();
        if (defaultPiece == null) {
            this.defaultPiece = null;
        } else {
            this.defaultPiece = new PieceNeighbors(defaultPiece);
            /*
            for (Face cartesianFace : Face.getCartesianFaces()) {
                this.defaultPiece.put(cartesianFace, defaultPiece);
            }
            */
            int pieceSize = sample.peek().getCenterPiece().xSize; // assuming all pieces are cubic
            if (defaultPiece.xSize != pieceSize || defaultPiece.ySize != pieceSize || defaultPiece.zSize != pieceSize) {
                throw new IllegalArgumentException("Default piece must have the same size as the others");
            }
        }
        this.useModuloCoords = useModuloCoords;
        this.seed = seed;
    }

    public boolean hasImpossibleStates() {
        return hasImpossibleStates;
    }

    public VirtualSpace<Sample> getWave() {
        return wave;
    }

    public @NotNull Random getRandom(int pieceX, int pieceY, int pieceZ) {
        // yes, we are creating a new Random for each generating piece, but this is needed in order to have the same piece
        // for the same seed at the same coords
        final Random random = new Random();
        random.setSeed((long) pieceX * random.nextLong() ^ (long) pieceY * random.nextLong() ^ (long) pieceZ * random.nextLong() ^ seed);
        return random;
    }

    /**
     * Automatically collapses the hole {@link Wave}. Does nothing if the {@link Wave} is already collapsed.
     */
    public void collapse() throws GenerationFailedException {
        if (isCollapsed) return;

        fillWithPossibleStates();

        final Random random0 = getRandom(0, 0, 0);
        lastChangedEntropies = new Stack<>();
        int lastX = random0.nextInt(wave.xMin(), wave.xMax() + 1);
        int lastY = random0.nextInt(wave.yMin(), wave.yMax() + 1);
        int lastZ = random0.nextInt(wave.zMin(), wave.zMax() + 1);
        while (true) {
            // choose a random node
            final Coords node = chooseLowEntropyNode(lastX, lastY, lastZ);
            if (node == null) {
                // finished
                isCollapsed = true;
                lastChangedEntropies = null;
                break;
            }
            //final Random random = getRandom(node.x(), node.y(), node.z());
            collapse(node.x(), node.y(), node.z());
            propagateCollapseFrom(node.x(), node.y(), node.z());
            while (!propagationTasks.isEmpty()) {
                final Set<Coords> propagationTasksCopy = new HashSet<>(propagationTasks);
                propagationTasks.clear();
                for (Coords propagationTask : propagationTasksCopy) {
                    propagateCollapseTo(propagationTask.x(), propagationTask.y(), propagationTask.z());
                }
                /*
                final ArrayList<Face> faces = new ArrayList<>(Face.getCartesianFaces());
                Collections.shuffle(faces, random);
                for (Face cartesianFace : faces) {
                    for (Pair<Coords, Face> propagationTask : propagationTasksCopy) {
                        if (propagationTask.b() != cartesianFace) continue;
                        propagateCollapseFrom(propagationTask.a().x(), propagationTask.a().y(), propagationTask.a().z());
                    }
                }

                 */
            }
        }
    }

    /**
     * Fills the wave with all possible states for each piece. You probably want to use {@link #collapse()} instead.
     */
    public void fillWithPossibleStates() {
        boolean isAlreadyCollapsed = sample.size() <= 1;
        final PieceNeighbors aPiece = sample.iterator().next();
        for (ObjectWithCoordinates<Sample> node : wave) {
            wave.set(new Sample(sample), node.x(), node.y(), node.z(), useModuloCoords);
            if (isAlreadyCollapsed) {
                pieceCollapsed(node.x(), node.y(), node.z(), aPiece);
            }
        }
    }

    /**
     * @return the pieces that could be collapsed at that position
     */
    @Contract(pure = true)
    public @NotNull Sample getCollapseCandidatesAt(int x, int y, int z) {
        final Sample newCandidates = new Sample();
        final Sample currentCandidates = wave.get(x, y, z, useModuloCoords);
        if (currentCandidates == null) return newCandidates;
        final Iterator<Map.Entry<PieceNeighbors, Integer>> iter = currentCandidates.elementsAndWeightsIterator();
        while (iter.hasNext()) {
            final Map.Entry<PieceNeighbors, Integer> entry = iter.next();
            final PieceNeighbors currentCandidate = entry.getKey();
            final int currentCandidateWeight = entry.getValue();
            boolean isValidCandidate = true;
            for (Map.Entry<Face, Piece> faceEntry : currentCandidate.entrySet()) {
                final Face face = faceEntry.getKey();
                final Piece expectedPiece = faceEntry.getValue();
                final @Nullable Sample foundSample = wave.get(x + face.getModX(), y + face.getModY(), z + face.getModZ(), useModuloCoords);
                if (foundSample == null) continue;
                if (!foundSample.centerPiecesContains(expectedPiece)) {
                    isValidCandidate = false;
                    break;
                }
            }
            if (isValidCandidate) {
                newCandidates.add(currentCandidate, currentCandidateWeight);
            }
        }
        return newCandidates;
    }

    /**
     * @return the collapsed {@link PieceNeighbors}
     */
    private @NotNull PieceNeighbors collapse(int x, int y, int z) throws GenerationFailedException {
        final Sample collapseCandidates = getCollapseCandidatesAt(x, y, z);

        return collapseWithTheseCandidates(x, y, z, collapseCandidates);
    }

    /**
     * @return the collapsed {@link PieceNeighbors}
     */
    private @NotNull PieceNeighbors collapseWithTheseCandidates(int x, int y, int z, @NotNull Sample collapseCandidates) throws GenerationFailedException {
        final PieceNeighbors collapsed;
        if (collapseCandidates.isEmpty()) {
            hasImpossibleStates = true;
            if (defaultPiece == null) throw new GenerationFailedException("Encountered an impossible state");
            else collapsed = defaultPiece;
        } else
            collapsed = Objects.requireNonNull(collapseCandidates.weightedChoose(getRandom(x, y, z)),
                    "weightedChoose() returned null");
        final Sample newSample = new Sample(); // a sample with a size of 1
        newSample.add(collapsed);
        wave.set(newSample, x, y, z, useModuloCoords);
        lastChangedEntropies.push(new ObjectWithCoordinates<>(newSample, x, y, z));
        pieceCollapsed(x, y, z, collapsed);
        // collapsing a piece also forces the neighbors to have the right centerpiece
        /*
        for (Map.Entry<Face, Piece> faceEntry : collapsed.entrySet()) {
            final Face face = faceEntry.getKey();
            final Sample sampleAtThatFace = wave.get(x + face.getModX(), y + face.getModY(), z + face.getModZ(), useModuloCoords);
            if (sampleAtThatFace == null) continue;
            if (sampleAtThatFace.retainAllWithCenterPiece(faceEntry.getValue())) {
                if (sampleAtThatFace.isEmpty()) {
                    hasImpossibleStates = true;
                    if (defaultPiece == null) throw new GenerationFailedException("Encountered an impossible state");
                    else sampleAtThatFace.add(defaultPiece);
                } else if (sampleAtThatFace.size() == 1) {
                    pieceCollapsed(x + face.getModX(), y + face.getModY(), z + face.getModZ(), sampleAtThatFace.peek());
                }
            }
        }
         */
        return collapsed;
    }

    private void propagateCollapseFrom(int x, int y, int z) throws GenerationFailedException {
        for (Face cartesianFace : Face.getCartesianFaces()) {
            propagateCollapseTo(x + cartesianFace.getModX(), y + cartesianFace.getModY(), z + cartesianFace.getModZ());
        }
    }

    private void propagateCollapseTo(int x, int y, int z) throws GenerationFailedException {
        final Sample present = wave.getWithoutFill(x, y, z);
        if (present == null) return;
        final int sizeBefore = present.size();

        if (present.size() == 1) return;
        present.retainAll(getCollapseCandidatesAt(x, y, z));
        if (present.size() == sizeBefore) return; // nothing changed, no need to propagate
        if (present.isEmpty()) {
            hasImpossibleStates = true;
            if (defaultPiece == null) throw new GenerationFailedException("Encountered an impossible state");
            else pieceCollapsed(x, y, z, defaultPiece);
        } else if (present.size() == 1) {
            pieceCollapsed(x, y, z, present.peek());
        }
        lastChangedEntropies.push(new ObjectWithCoordinates<>(present, x, y, z));
        for (Face cartesianFace : Face.getCartesianFaces()) {
            propagationTasks.add(new Coords(x + cartesianFace.getModX(), y + cartesianFace.getModY(), z + cartesianFace.getModZ()));
        }
    }

    private final Set<Coords> propagationTasks = new HashSet<>();

    @SuppressWarnings("ConstantConditions")
    public boolean nodeIsCollapsed(int x, int y, int z) {
        return wave.get(x, y, z).size() == 1;
    }

    private Stack<ObjectWithCoordinates<Sample>> lastChangedEntropies;

    /**
     * This searches a node (coordinates) a piece with a low entropy (but strictly above 1, otherwise that means the wave
     * has collapsed) among the last changed entropies
     * @return null if the wave has collapsed
     */
    private @Nullable Coords chooseLowEntropyNode(int lastX, int lastY, int lastZ) {
        return chooseLowEntropyNode(lastX, lastY, lastZ, false);
    }

    /**
     * This searches a node (coordinates) with a low entropy (but strictly above 1, otherwise that means the wave
     * has collapsed) among the last changed entropies
     * @param totalSearch if true, the search is done on the whole wave, otherwise only on the last changed entropies
     * @return null if the wave has collapsed
     */
    private @Nullable Coords chooseLowEntropyNode(int lastX, int lastY, int lastZ, boolean totalSearch) {
        Random random = getRandom(lastX, lastY, lastZ);
        int lowestEntropy = Integer.MAX_VALUE;
        final Set<Coords> lowestEntropyNodes = new HashSet<>(); // this is a list so every piece has the same chance to be chosen
        final Iterator<ObjectWithCoordinates<Sample>> iterator;
        iterator = totalSearch ? wave.iterator() : lastChangedEntropies.iterator();
        while (iterator.hasNext()) {
            ObjectWithCoordinates<Sample> node = iterator.next();
            final Sample object = node.object();
            if (!totalSearch) {
                // using last changed entropies, that means some entropies might be wrong
                //noinspection ConstantConditions
                if (wave.get(node.coords()).size() <= 1) continue;
            }
            if (1 < object.size() && object.size() < lowestEntropy) {
                lowestEntropy = object.size();
                lowestEntropyNodes.clear();
                lowestEntropyNodes.add(node.coords());
            } else if (object.size() == lowestEntropy) {
                lowestEntropyNodes.add(node.coords());
            }
        }
        if (lowestEntropyNodes.isEmpty()) {
            if (!totalSearch) {
                return chooseLowEntropyNode(lastX, lastY, lastZ, true);
            } else {
                return null;
            }
        }
        // returning a random element
        final int index = random.nextInt(lowestEntropyNodes.size());
        final Iterator<Coords> iter = lowestEntropyNodes.iterator();
        for (int i = 0; i < index; i++) {
            iter.next();
        }
        return iter.next();
    }

    public void registerPieceCollapseListener(PieceCollapseListener listener) {
        pieceCollapseListeners.add(Objects.requireNonNull(listener));
    }

    private void pieceCollapsed(int pieceX, int pieceY, int pieceZ, PieceNeighbors piece) {
        for (PieceCollapseListener listener : pieceCollapseListeners) {
            listener.onCollapse(pieceX, pieceY, pieceZ, piece);
        }
    }

    /**
     * A {@link FunctionalInterface} whose method is {@link #onCollapse(int, int, int, PieceNeighbors)}. It is called when a piece
     * of this {@link Wave} totally collapses, meaning there is only one state left. that piece is passed along with its
     * coordinates in the parameters.
     */
    @FunctionalInterface
    public interface PieceCollapseListener {
        void onCollapse(int pieceX, int pieceY, int pieceZ, PieceNeighbors piece);
    }

    public static class GenerationFailedException extends Exception {
        public GenerationFailedException(String message) {
            super(message);
        }
    }
}
