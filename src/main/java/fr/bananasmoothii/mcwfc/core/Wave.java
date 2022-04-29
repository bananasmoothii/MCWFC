package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.VirtualSpace.ObjectWithCoordinates;
import fr.bananasmoothii.mcwfc.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Wave {
    private final VirtualSpace<Set<Piece>> wave = new VirtualSpace<>();
    private final ImmutablePieceNeighborsSet pieces;
    private final int pieceSize;
    private final long seed;
    private final List<@NotNull PieceCollapseListener> pieceCollapseListeners = new ArrayList<>();
    public final boolean useModuloCoords;
    private boolean isCollapsed = false;

    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds) {
        this(pieces, bounds, true);
    }

    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds, boolean useModuloCoords) {
        this(pieces, bounds, useModuloCoords, ThreadLocalRandom.current().nextLong());
    }

    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds, boolean useModuloCoords, long seed) {
        this.pieces = pieces.immutable();
        pieceSize = pieces.getAny().getCenterPiece().xSize; // assuming all pieces are cubic
        this.useModuloCoords = useModuloCoords;
        this.seed = seed;
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
        for (ObjectWithCoordinates<Set<Piece>> node : wave) {
            wave.set(pieces.getCenterPieces(), node.x(), node.y(), node.z());
        }

        final Random random0 = getRandom(0, 0, 0);
        int lastX = random0.nextInt(wave.xMin(), wave.xMax());
        int lastY = random0.nextInt(wave.yMin(), wave.yMax());
        int lastZ = random0.nextInt(wave.zMin(), wave.zMax());
        while (true) {
            // choose a random node
            final Coords node = chooseLowEntropyNode(lastX, lastY, lastZ);
            if (node == null) {
                isCollapsed = true;
                lastChangedEntropies = null;
                break;
            }
            collapse(node.x(), node.y(), node.z());
            propagateCollapseFrom(node.x(), node.y(), node.z());
        }
    }

    private void collapse(int x, int y, int z) throws GenerationFailedException {
        final WeightedSet<Piece> collapseCandidates = new WeightedSet<>();
        for (Piece piece : Objects.requireNonNull(wave.get(x, y, z, useModuloCoords))) {
            final PieceNeighbors neighbors = pieces.getNeighbors(piece);
            int pieceWeight = 0;
            boolean isValidPiece = true;
            for (Map.Entry<Face, WeightedSet<Piece>> faceEntry : neighbors.getNeighbors().entrySet()) {
                boolean foundMatchingNeighbors = false;
                final Set<Piece> actualNeighbors = wave.get(
                        x + faceEntry.getKey().getModX(),
                        y + faceEntry.getKey().getModY(),
                        z + faceEntry.getKey().getModZ(),
                        useModuloCoords);
                if (actualNeighbors == null) continue;
                for (Piece expectedNeighbor : faceEntry.getValue()) {
                    if (actualNeighbors.contains(expectedNeighbor)) {
                        foundMatchingNeighbors = true;
                        break;
                    }
                }
                if (!foundMatchingNeighbors) {
                    isValidPiece = false;
                    break;
                }
                for (Piece actualNeighbor : actualNeighbors) {
                    final int weight = pieces.getNeighbors(actualNeighbor)
                                    .getNeighbors(faceEntry.getKey().getOppositeFace())
                                    .getWeight(piece);
                    if (weight == 0) throw new IllegalStateException("A \"valid\" piece is not recognized by one of its neighbors");
                    pieceWeight += weight;
                }
            }
            if (isValidPiece) {
                collapseCandidates.add(piece, pieceWeight);
            }
        }
        if (collapseCandidates.isEmpty()) throw new GenerationFailedException("Encountered an impossible state");
        final Piece collapsed = Objects.requireNonNull(collapseCandidates.weightedChoose(), "weightedChoose() returned null");
        final SingleElementSet<Piece> set = new SingleElementSet<>(collapsed);
        wave.set(set, x, y, z);
        lastChangedEntropies.push(new ObjectWithCoordinates<>(set, x, y, z));
        pieceCollapsed(x, y, z, collapsed);
    }

    private void propagateCollapseFrom(int x, int y, int z) {
        propagateCollapse(x - 1, y, z);
        propagateCollapse(x + 1, y, z);
        propagateCollapse(x, y - 1, z);
        propagateCollapse(x, y + 1, z);
        propagateCollapse(x, y, z - 1);
        propagateCollapse(x, y, z + 1);
    }

    private void propagateCollapse(int x, int y, int z) {
        if (nodeIsCollapsed(x, y, z)) return;
        // TODO
    }

    @SuppressWarnings("ConstantConditions")
    public boolean nodeIsCollapsed(int x, int y, int z) {
        return wave.get(x, y, z).size() == 1;
    }

    private Stack<ObjectWithCoordinates<Set<Piece>>> lastChangedEntropies = new Stack<>();

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
        final ArrayList<Coords> lowestEntropyNodes = new ArrayList<>(); // this is a list so every piece has the same chance to be chosen
        final Iterator<ObjectWithCoordinates<Set<Piece>>> iterator;
        iterator = totalSearch ? wave.iterator() : lastChangedEntropies.iterator();
        while (iterator.hasNext()) {
            ObjectWithCoordinates<Set<Piece>> node = iterator.next();
            final Set<Piece> object = node.object();
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
        return lowestEntropyNodes.get(random.nextInt(lowestEntropyNodes.size()));
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
