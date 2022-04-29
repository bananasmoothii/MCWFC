package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.VirtualSpace.ObjectWithCoordinates;
import fr.bananasmoothii.mcwfc.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static fr.bananasmoothii.mcwfc.bukkit.MCWFCPlugin.log;

public class Wave {
    private final VirtualSpace<Set<Piece>> wave;
    private final ImmutablePieceNeighborsSet pieces;
    private final long seed;
    private final @Nullable Piece defaultPiece;
    private final List<@NotNull PieceCollapseListener> pieceCollapseListeners = new ArrayList<>();
    public final boolean useModuloCoords;
    private boolean isCollapsed = false;
    private boolean hasImpossibleStates = false;

    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds, @Nullable Piece defaultPiece) {
        this(pieces, bounds, defaultPiece, true);
    }

    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds, @Nullable Piece defaultPiece, boolean useModuloCoords) {
        this(pieces, bounds, defaultPiece, useModuloCoords, ThreadLocalRandom.current().nextLong());
    }

    /**
     * @param defaultPiece if not null, instead of throwing a {@link GenerationFailedException} when there is a piece
     *                     with an entropy of 0, it will be replaced by this piece.
     */
    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds, @Nullable Piece defaultPiece, boolean useModuloCoords, long seed) {
        wave = new VirtualSpace<>(bounds);
        this.pieces = pieces.immutable();
        this.defaultPiece = defaultPiece;
        int pieceSize = pieces.getAny().getCenterPiece().xSize; // assuming all pieces are cubic
        if (defaultPiece != null && (defaultPiece.xSize != pieceSize || defaultPiece.ySize != pieceSize
                || defaultPiece.zSize != pieceSize)) {
            throw new IllegalArgumentException("Default piece must have the same size as the others");
        }
        this.useModuloCoords = useModuloCoords;
        this.seed = seed;
    }

    public boolean hasImpossibleStates() {
        return hasImpossibleStates;
    }

    public VirtualSpace<Set<Piece>> getWave() {
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
        boolean isAlreadyCollapsed = pieces.getCenterPieces().size() <= 1;
        final Piece aPiece = pieces.getCenterPieces().iterator().next();
        for (ObjectWithCoordinates<Set<Piece>> node : wave) {
            wave.set(pieces.getCenterPieces(), node.x(), node.y(), node.z(), useModuloCoords);
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
            propagateCollapseFrom(node.x(), node.y(), node.z(), collapse(node.x(), node.y(), node.z()));
        }
    }

    /**
     * @return the collapsed {@link Piece}
     */
    private @NotNull Piece collapse(int x, int y, int z) throws GenerationFailedException {
        final WeightedSet<Piece> collapseCandidates = new WeightedSet<>();
        for (Piece piece : Objects.requireNonNull(wave.get(x, y, z, useModuloCoords))) {
            final @NotNull PieceNeighbors neighbors = Objects.requireNonNull(pieces.getNeighbors(piece));
            int pieceWeight = 0;
            boolean isValidPiece = true;
            for (Map.Entry<Face, WeightedSet<Piece>> faceEntry : neighbors.getNeighbors().entrySet()) {
                final Face face = faceEntry.getKey();
                final Set<Piece> actualNeighbors = wave.get(
                        x + face.getModX(),
                        y + face.getModY(),
                        z + face.getModZ(),
                        useModuloCoords);
                if (actualNeighbors == null) continue;
                if (!actualNeighbors.containsAll(faceEntry.getValue())) { // contains all expected neighbors
                    isValidPiece = false;
                }
                if (!isValidPiece) break;
                for (Piece actualNeighbor : actualNeighbors) {
                    @SuppressWarnings("ConstantConditions")
                    final WeightedSet<Piece> neighbors1 =
                            pieces.getNeighbors(actualNeighbor).getNeighbors(face.getOppositeFace());
                    if (neighbors1 == null) continue;
                    pieceWeight += neighbors1.getWeight(piece);
                }
                if (pieceWeight == 0) throw new IllegalStateException("A \"valid\" piece is not recognized by its neighbors");
            }
            if (isValidPiece) {
                collapseCandidates.add(piece, pieceWeight);
            }
        }
        final Piece collapsed;
        if (collapseCandidates.isEmpty()) {
            hasImpossibleStates = true;
            if (defaultPiece == null) throw new GenerationFailedException("Encountered an impossible state");
            else collapsed = defaultPiece;
        } else
            collapsed = Objects.requireNonNull(collapseCandidates.weightedChoose(), "weightedChoose() returned null");
        final SingleElementSet<Piece> set = new SingleElementSet<>(collapsed);
        wave.set(set, x, y, z, useModuloCoords);
        lastChangedEntropies.push(new ObjectWithCoordinates<>(set, x, y, z));
        pieceCollapsed(x, y, z, collapsed);
        return collapsed;
    }

    private void propagateCollapseFrom(int x, int y, int z, @NotNull Piece collapsed) throws GenerationFailedException {
        final @NotNull PieceNeighbors neighbors = Objects.requireNonNull(pieces.getNeighbors(collapsed));
        for (Map.Entry<Face, WeightedSet<Piece>> faceEntry : neighbors.getNeighbors().entrySet()) {
            final Face face = faceEntry.getKey();
            propagateCollapse(x + face.getModX(), y + face.getModY(), z + face.getModZ(), faceEntry.getValue());
        }
    }

    private void propagateCollapse(int x, int y, int z, Set<Piece> validNeighbors) throws GenerationFailedException {
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
            } case 1 -> pieceCollapsed(x, y, z, actualNeighbors.iterator().next());
        }

        for (Face cartesianFace : Face.getCartesianFaces()) {
            final Set<Piece> newValidNeighbors = new HashSet<>();
            for (Piece actualNeighbor : actualNeighbors) {
                //noinspection ConstantConditions
                final WeightedSet<Piece> neighbors = pieces.getNeighbors(actualNeighbor).getNeighbors(cartesianFace);
                if (neighbors == null) {
                    log.warning("neighbors == null");
                    continue;
                }
                newValidNeighbors.addAll(neighbors);
            }
            propagateCollapse(x + cartesianFace.getModX(), y + cartesianFace.getModY(), z + cartesianFace.getModZ(), newValidNeighbors);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public boolean nodeIsCollapsed(int x, int y, int z) {
        return wave.get(x, y, z).size() == 1;
    }

    private Stack<ObjectWithCoordinates<Set<Piece>>> lastChangedEntropies;

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
        final Iterator<ObjectWithCoordinates<Set<Piece>>> iterator;
        iterator = totalSearch ? wave.iterator() : lastChangedEntropies.iterator();
        while (iterator.hasNext()) {
            ObjectWithCoordinates<Set<Piece>> node = iterator.next();
            final Set<Piece> object = node.object();
            if (!totalSearch) {
                // using last changed entropies, that means some entropies might be wrong
                if (Objects.requireNonNull(wave.get(node.coords())).size() <= 1) continue;
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

    private void pieceCollapsed(int pieceX, int pieceY, int pieceZ, Piece piece) {
        for (PieceCollapseListener listener : pieceCollapseListeners) {
            listener.onCollapse(pieceX, pieceY, pieceZ, piece);
        }
    }

    /**
     * A {@link FunctionalInterface} whose method is {@link #onCollapse(int, int, int, Piece)}. It is called when a piece
     * of this {@link Wave} totally collapses, meaning there is only one state left. that piece is passed along with its
     * coordinates in the parameters.
     */
    @FunctionalInterface
    public interface PieceCollapseListener {
        void onCollapse(int pieceX, int pieceY, int pieceZ, Piece piece);
    }

    public static class GenerationFailedException extends Exception {
        public GenerationFailedException(String message) {
            super(message);
        }
    }
}
