package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.VirtualSpace.ObjectWithCoordinates;
import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Coords;
import fr.bananasmoothii.mcwfc.core.util.Face;
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
    private final VirtualSpace<Sample1> wave;
    private final ImmutableSample1 sample;
    private final long seed;
    private final @Nullable PieceNeighbors1 defaultPiece;
    private final List<@NotNull PieceCollapseListener> pieceCollapseListeners = new ArrayList<>();
    public final boolean useModuloCoords;
    private boolean isCollapsed = false;
    private boolean hasImpossibleStates = false;

    public Wave(@NotNull Sample1 sample, @NotNull Bounds bounds, @Nullable Piece defaultPiece) {
        this(sample, bounds, defaultPiece, true);
    }

    public Wave(@NotNull Sample1 sample, @NotNull Bounds bounds, @Nullable Piece defaultPiece, boolean useModuloCoords) {
        this(sample, bounds, defaultPiece, useModuloCoords, ThreadLocalRandom.current().nextLong());
    }

    /**
     * @param defaultPiece if not null, instead of throwing a {@link GenerationFailedException} when there is a piece
     *                     with an entropy of 0, it will be replaced by this piece.
     */
    public Wave(@NotNull Sample1 sample, @NotNull Bounds bounds, @Nullable Piece defaultPiece, boolean useModuloCoords, long seed) {
        wave = new VirtualSpace<>(bounds);
        this.sample = sample.immutable();
        if (defaultPiece == null) {
            this.defaultPiece = null;
        } else {
            this.defaultPiece = new PieceNeighbors1(defaultPiece);
            for (Face cartesianFace : Face.getCartesianFaces()) {
                this.defaultPiece.put(cartesianFace, defaultPiece);
            }
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

    public VirtualSpace<Sample1> getWave() {
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

        // fill the wave with all possible states for each piece
        boolean isAlreadyCollapsed = sample.size() <= 1;
        final PieceNeighbors1 aPiece = sample.iterator().next();
        for (ObjectWithCoordinates<Sample1> node : wave) {
            wave.set(new Sample1(sample), node.x(), node.y(), node.z(), useModuloCoords);
            if (isAlreadyCollapsed) {
                pieceCollapsed(node.x(), node.y(), node.z(), aPiece);
            }
        }

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
            collapse(node.x(), node.y(), node.z());
            propagateCollapseFrom(node.x(), node.y(), node.z());
        }
    }

    private @NotNull Sample1 getCollapseCandidatesAt(int x, int y, int z) {
        final Sample1 newCandidates = new Sample1();
        final Sample1 currentCandidates = wave.get(x, y, z, useModuloCoords);
        if (currentCandidates == null) return newCandidates;
        for (PieceNeighbors1 currentCandidate : currentCandidates) {
            boolean isValidCandidate = true;
            for (Map.Entry<Face, Piece> faceEntry : currentCandidate.entrySet()) {
                final Face face = faceEntry.getKey();
                final Piece piece = faceEntry.getValue();
                final @Nullable Sample1 foundSample = wave.get(x + face.getModX(), y + face.getModY(), z + face.getModZ(), useModuloCoords);
                if (foundSample == null) continue;
                if (!foundSample.centerPiecesContains(piece)) {
                    isValidCandidate = false;
                    break;
                }
            }
            if (isValidCandidate) {
                newCandidates.add(currentCandidate);
            }
        }
        return newCandidates;
        /*
        final Sample newCandidates = new Sample();
        final Sample currentCandidates = wave.get(x, y, z, useModuloCoords);
        if (currentCandidates == null) return newCandidates;
        for (PieceNeighborsPossibilities currentCandidate : currentCandidates) {
            final PieceNeighborsPossibilities validCandidate = new PieceNeighborsPossibilities(currentCandidate.getCenterPiece());
            for (PieceNeighbors pieceNeighbors : currentCandidate) {
                boolean pieceNeighborsIsValid = true;
                for (Map.Entry<Face, Piece> faceEntry : pieceNeighbors.entrySet()) {
                    final Face face = faceEntry.getKey();
                    final @Nullable Sample foundSample = wave.get(x + face.getModX(), y + face.getModY(), z + face.getModZ(), useModuloCoords);
                    if (foundSample == null) continue;
                    final Set<Piece> foundPieces = foundSample.getCenterPieces();
                    if (!foundPieces.contains(faceEntry.getValue())) {
                        pieceNeighborsIsValid = false;
                        break;
                    }
                }
                if (pieceNeighborsIsValid) {
                    validCandidate.add(pieceNeighbors);
                }
            }
            if (!validCandidate.isEmpty())
                newCandidates.add(validCandidate);
        }
        return newCandidates;

         */
    }

    /**
     * @return the collapsed {@link Piece}
     */
    private @NotNull PieceNeighbors1 collapse(int x, int y, int z) throws GenerationFailedException {
        final Sample1 collapseCandidates = getCollapseCandidatesAt(x, y, z);

        /*
        final WeightedSet<Piece> collapseCandidates = new WeightedSet<>();
        for (Piece piece : Objects.requireNonNull(wave.get(x, y, z, useModuloCoords))) {
            final @NotNull PieceNeighborsPossibilities expectedNeighbors = Objects.requireNonNull(sample.getNeighborsFor(piece));
            boolean isValidPiece = false;
            int pieceWeight = 0;
            for (PieceNeighbors expectedNeighbor : expectedNeighbors) {
                boolean expectedNeighborIsValid = true;
                for (Map.Entry<Face, Piece> faceEntry : expectedNeighbor.entrySet()) {
                    final Face face = faceEntry.getKey();
                    final Piece expectedNeighborForFace = faceEntry.getValue();
                    final Set<Piece> actualNeighbors = wave.get(
                            x + face.getModX(),
                            y + face.getModY(),
                            z + face.getModZ(),
                            useModuloCoords);
                    if (actualNeighbors == null) continue;
                    if (!actualNeighbors.contains(expectedNeighborForFace)) {
                        expectedNeighborIsValid = false;
                        break;
                    }
                }
                if (expectedNeighborIsValid) {
                    isValidPiece = true;
                    pieceWeight += expectedNeighbors.getWeight(expectedNeighbor);
                    // not break, because we want to count the weight of all valid pieces
                }
            }
            if (isValidPiece) {
                collapseCandidates.add(piece, pieceWeight);
            }
        }
         */
        final PieceNeighbors1 collapsed;
        if (collapseCandidates.isEmpty()) {
            hasImpossibleStates = true;
            if (defaultPiece == null) throw new GenerationFailedException("Encountered an impossible state");
            else collapsed = defaultPiece;
        } else
            collapsed = Objects.requireNonNull(collapseCandidates.weightedChoose(), "weightedChoose() returned null");
        final Sample1 newSample = new Sample1(); // a sample with a size of 1
        newSample.add(collapsed);
        wave.set(newSample, x, y, z, useModuloCoords);
        lastChangedEntropies.push(new ObjectWithCoordinates<>(newSample, x, y, z));
        pieceCollapsed(x, y, z, collapsed);
        return collapsed;
    }

    private void propagateCollapseFrom(int x, int y, int z) throws GenerationFailedException {
        //final @NotNull PieceNeighborsPossibilities neighbors = Objects.requireNonNull(pieces.getNeighborsFor(collapsed));
        for (Face cartesianFace : Face.getCartesianFaces()) {
            /*
            final Set<Piece> validPieces = new HashSet<>();
            for (PieceNeighbors neighborPossibility : neighbors) {
                validPieces.add(neighborPossibility.get(cartesianFace));
            }
             */
            propagateCollapseTo(x + cartesianFace.getModX(), y + cartesianFace.getModY(), z + cartesianFace.getModZ());
        }
    }

    private void propagateCollapseTo(int x, int y, int z) throws GenerationFailedException {
        final Sample1 present = wave.getWithoutFill(x, y, z);
        if (present == null) return;
        final int sizeBefore = present.size();
        /*
        final Set<Piece> piecesToRemove = new HashSet<>();
        for (Piece presentPiece : present) {
            boolean isValidPiece = true;
            for (Face face : Face.getCartesianFaces()) {
                final Set<Piece> neighborsForFace = wave.get(x + face.getModX(), y + face.getModY(), z + face.getModZ(), useModuloCoords);
                if (neighborsForFace == null) continue;
                boolean foundAcceptingNeighborForFace = false;
                for (Piece aNeighbor : neighborsForFace) {
                    final PieceNeighborsPossibilities neighborsOfNeighbor = sample.getNeighborsFor(aNeighbor);
                    if (neighborsOfNeighbor == null) continue;
                    for (PieceNeighbors pieceNeighbors : neighborsOfNeighbor) {
                        if (pieceNeighbors.get(face).equals(presentPiece)) {
                            foundAcceptingNeighborForFace = true;
                            break;
                        }
                    }
                    if (foundAcceptingNeighborForFace) break;
                }
                if (!foundAcceptingNeighborForFace) {
                    isValidPiece = false;
                    break;
                }
            }
            if (!isValidPiece) piecesToRemove.add(presentPiece);
        }
         */

        present.retainAll(getCollapseCandidatesAt(x, y, z));
        if (present.size() == sizeBefore) return; // nothing changed, no need to propagate
        if (present.isEmpty()) {
            hasImpossibleStates = true;
            if (defaultPiece == null) throw new GenerationFailedException("Encountered an impossible state");
            else pieceCollapsed(x, y, z, defaultPiece);
        } else if (present.size() == 1) {
            pieceCollapsed(x, y, z, present.iterator().next());
        }
        lastChangedEntropies.push(new ObjectWithCoordinates<>(present, x, y, z));
        for (Face cartesianFace : Face.getCartesianFaces()) {
            propagateCollapseTo(x + cartesianFace.getModX(), y + cartesianFace.getModY(), z + cartesianFace.getModZ());
        }
    }

/*
    private void propagateCollapse(int x, int y, int z, @NotNull Set<Piece> validNeighbors) throws GenerationFailedException {
        // not using modulo coords here otherwise actualNeighbor will never be null and the loop will never end
        final Set<Piece> actualNeighbors = wave.getWithoutFill(x, y, z);
        if (actualNeighbors == null) return;
        final int sizeBefore = actualNeighbors.size();
        if (sizeBefore <= 1) return;
        // retain only valid neighbors
        actualNeighbors.retainAll(validNeighbors);
        if (actualNeighbors.size() == sizeBefore) return; // nothing changed, no need to propagate
        lastChangedEntropies.push(new ObjectWithCoordinates<>(actualNeighbors, x, y, z));
        switch (actualNeighbors.size()) {
            case 0 -> {
                hasImpossibleStates = true;
                if (defaultPiece == null) throw new GenerationFailedException("Encountered an impossible state");
                else pieceCollapsed(x, y, z, defaultPiece);
            }
            case 1 -> pieceCollapsed(x, y, z, actualNeighbors.iterator().next());
        }

        for (Face cartesianFace : Face.getCartesianFaces()) {
            final Set<Piece> newValidNeighbors = new HashSet<>();
            for (Piece actualNeighbor : actualNeighbors) {
                final PieceNeighborsPossibilities neighborPossibilitiesOfNeighbors = sample.getNeighborsFor(actualNeighbor);
                if (neighborPossibilitiesOfNeighbors == null) {
                    log.warning("neighborPossibilitiesOfNeighbors == null");
                    continue;
                }
                for (PieceNeighbors neighborPossibility : neighborPossibilitiesOfNeighbors) {
                    newValidNeighbors.add(neighborPossibility.get(cartesianFace));
                }
            }
            propagateCollapse(x + cartesianFace.getModX(), y + cartesianFace.getModY(), z + cartesianFace.getModZ(), newValidNeighbors);
        }
    }

 */

    @SuppressWarnings("ConstantConditions")
    public boolean nodeIsCollapsed(int x, int y, int z) {
        return wave.get(x, y, z).size() == 1;
    }

    private Stack<ObjectWithCoordinates<Sample1>> lastChangedEntropies;

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
        final Iterator<ObjectWithCoordinates<Sample1>> iterator;
        iterator = totalSearch ? wave.iterator() : lastChangedEntropies.iterator();
        while (iterator.hasNext()) {
            ObjectWithCoordinates<Sample1> node = iterator.next();
            final Sample1 object = node.object();
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

    private void pieceCollapsed(int pieceX, int pieceY, int pieceZ, PieceNeighbors1 piece) {
        for (PieceCollapseListener listener : pieceCollapseListeners) {
            listener.onCollapse(pieceX, pieceY, pieceZ, piece);
        }
    }

    /**
     * A {@link FunctionalInterface} whose method is {@link #onCollapse(int, int, int, PieceNeighbors1)}. It is called when a piece
     * of this {@link Wave} totally collapses, meaning there is only one state left. that piece is passed along with its
     * coordinates in the parameters.
     */
    @FunctionalInterface
    public interface PieceCollapseListener {
        void onCollapse(int pieceX, int pieceY, int pieceZ, PieceNeighbors1 piece);
    }

    public static class GenerationFailedException extends Exception {
        public GenerationFailedException(String message) {
            super(message);
        }
    }
}
