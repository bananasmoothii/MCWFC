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

/**
 * @param <B> the type of the blocks in the {@link Piece}s. For example, in bukkit, this is {@link org.bukkit.block.data.BlockData}
 */
public class Wave<B> {
    /**
     * This needs to be a virtual space of {@link Sample<B>} and not just {@link Set}<{@link Piece}>
     * because two {@link PieceNeighbors<B>} are different while their centerpiece might be the same.
     * At the begenning, every node is equal to {@link #sample}, but they are not the same object: each {@link Sample<B>}
     * in the {@link VirtualSpace} is unique.
     */
    private final VirtualSpace<Sample<B>> wave;
    private final ImmutableSample<B> sample;
    private final long seed;
    private final List<@NotNull PieceCollapseListener<B>> pieceCollapseListeners = new ArrayList<>();
    public final boolean useModuloCoords;
    private boolean isCollapsed = false;
    private boolean hasImpossibleStates = false;

    public Wave(@NotNull Sample<B> sample, @NotNull Bounds bounds) {
        this(sample, bounds, true);
    }

    public Wave(@NotNull Sample<B> sample, @NotNull Bounds bounds, boolean useModuloCoords) {
        this(sample, bounds, useModuloCoords, ThreadLocalRandom.current().nextLong());
    }

    public Wave(@NotNull Sample<B> sample, @NotNull Bounds bounds, boolean useModuloCoords, long seed) {
        wave = new VirtualSpace<>(bounds);
        this.sample = sample.immutable();
        this.useModuloCoords = useModuloCoords;
        this.seed = seed;
    }

    public boolean hasImpossibleStates() {
        return hasImpossibleStates;
    }

    public VirtualSpace<Sample<B>> getWave() {
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
        final PieceNeighbors<B> aPiece = sample.iterator().next();
        for (ObjectWithCoordinates<Sample<B>> node : wave) {
            wave.set(new Sample<>(sample), node.x(), node.y(), node.z(), useModuloCoords);
            if (isAlreadyCollapsed) {
                pieceCollapsedCallListeners(node.x(), node.y(), node.z(), aPiece);
            }
        }
    }

    /**
     * @return the pieces that could be collapsed at that position
     */
    @Contract(pure = true)
    public @NotNull Sample<B> getCollapseCandidatesAt(int x, int y, int z) {
        final Sample<B> newCandidates = new Sample<>();
        final Sample<B> currentCandidates = wave.get(x, y, z, useModuloCoords);
        if (currentCandidates == null) return newCandidates;
        final Iterator<Map.Entry<PieceNeighbors<B>, Integer>> iter = currentCandidates.elementsAndWeightsIterator();
        while (iter.hasNext()) {
            final Map.Entry<PieceNeighbors<B>, Integer> entry = iter.next();
            final PieceNeighbors<B> currentCandidate = entry.getKey();
            final int currentCandidateWeight = entry.getValue();
            boolean isValidCandidate = true;
            for (Map.Entry<Face, Piece<B>> faceEntry : currentCandidate.entrySet()) {
                final Face face = faceEntry.getKey();
                final Piece<B> expectedPiece = faceEntry.getValue();
                final @Nullable Sample<B> foundSample = wave.get(x + face.getModX(), y + face.getModY(), z + face.getModZ(), useModuloCoords);
                if (foundSample == null) continue;
                if (!(foundSample.centerPiecesContains(expectedPiece) && foundSample.acceptsAt(face.getOppositeFace(), currentCandidate.getCenterPiece()))) {
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
    private @NotNull PieceNeighbors<B> collapse(int x, int y, int z) throws GenerationFailedException {
        final Sample<B> collapseCandidates = getCollapseCandidatesAt(x, y, z);

        return collapseWithTheseCandidates(x, y, z, collapseCandidates);
    }

    /**
     * @return the collapsed {@link PieceNeighbors}
     */
    private @NotNull PieceNeighbors<B> collapseWithTheseCandidates(int x, int y, int z, @NotNull Sample<B> collapseCandidates) throws GenerationFailedException {
        final PieceNeighbors<B> collapsed;
        if (collapseCandidates.isEmpty()) {
            hasImpossibleStates = true;
            throw new GenerationFailedException("Encountered an impossible state");
        } else
            collapsed = Objects.requireNonNull(collapseCandidates.weightedChoose(getRandom(x, y, z)),
                    "weightedChoose() returned null");
        final Sample<B> newSample = new Sample<>(); // a sample with a size of 1
        newSample.add(collapsed);
        wave.set(newSample, x, y, z, useModuloCoords);
        lastChangedEntropies.push(new ObjectWithCoordinates<>(newSample, x, y, z));
        pieceCollapsed(x, y, z, collapsed);
        return collapsed;
    }

    /**
     * Should be called everytime an entropy is changed to 1.
     */
    private void pieceCollapsed(int x, int y, int z, @NotNull PieceNeighbors<B> collapsed) throws GenerationFailedException {
        // collapsing a piece also forces the neighbors to have the right centerpiece
        for (Map.Entry<Face, Piece<B>> faceEntry : collapsed.entrySet()) {
            final Face face = faceEntry.getKey();
            final Sample<B> sampleAtThatFace = wave.get(x + face.getModX(), y + face.getModY(), z + face.getModZ(), useModuloCoords);
            if (sampleAtThatFace == null) continue;
            if (sampleAtThatFace.retainAllWithCenterPiece(faceEntry.getValue())) {
                if (sampleAtThatFace.isEmpty()) {
                    hasImpossibleStates = true;
                    throw new GenerationFailedException("Encountered an impossible state");
                } else if (sampleAtThatFace.size() == 1) {
                    pieceCollapsed(x + face.getModX(), y + face.getModY(), z + face.getModZ(), sampleAtThatFace.peek());
                }
            }
        }
        pieceCollapsedCallListeners(x, y, z, collapsed);
    }

    private void propagateCollapseFrom(int x, int y, int z) throws GenerationFailedException {
        for (Face cartesianFace : Face.getCartesianFaces()) {
            propagateCollapseTo(x + cartesianFace.getModX(), y + cartesianFace.getModY(), z + cartesianFace.getModZ());
        }
    }

    private void propagateCollapseTo(int x, int y, int z) throws GenerationFailedException {
        final Sample<B> present = wave.getWithoutFill(x, y, z);
        if (present == null) return;
        final int sizeBefore = present.size();

        if (present.size() == 1) return;
        present.retainAll(getCollapseCandidatesAt(x, y, z));
        if (present.size() == sizeBefore) return; // nothing changed, no need to propagate
        if (present.isEmpty()) {
            hasImpossibleStates = true;
            throw new GenerationFailedException("Encountered an impossible state");
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

    private Stack<ObjectWithCoordinates<Sample<B>>> lastChangedEntropies;

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
        final Iterator<ObjectWithCoordinates<Sample<B>>> iterator;
        iterator = totalSearch ? wave.iterator() : lastChangedEntropies.iterator();
        while (iterator.hasNext()) {
            ObjectWithCoordinates<Sample<B>> node = iterator.next();
            final Sample<B> object = node.object();
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

    public void registerPieceCollapseListener(PieceCollapseListener<B> listener) {
        pieceCollapseListeners.add(Objects.requireNonNull(listener));
    }

    private void pieceCollapsedCallListeners(int pieceX, int pieceY, int pieceZ, PieceNeighbors<B> piece) {
        for (PieceCollapseListener<B> listener : pieceCollapseListeners) {
            listener.onCollapse(pieceX, pieceY, pieceZ, piece);
        }
    }

    /**
     * A {@link FunctionalInterface} whose method is {@link #onCollapse(int, int, int, PieceNeighbors)}. It is called when a piece
     * of this {@link Wave} totally collapses, meaning there is only one state left. that piece is passed along with its
     * coordinates in the parameters.
     */
    @FunctionalInterface
    public interface PieceCollapseListener<B> {
        void onCollapse(int pieceX, int pieceY, int pieceZ, PieceNeighbors<B> piece);
    }

    public static class GenerationFailedException extends Exception {
        // TODO: make GenerationFailedException specify some improvements that could be made on the dataset (adding
        //       one or more (probably just one since the generation stops at the first impossible state)
        //       PieceNeighbors<B>) to make it easier to collapse the wave.
        public GenerationFailedException(String message) {
            super(message);
        }
    }
}
