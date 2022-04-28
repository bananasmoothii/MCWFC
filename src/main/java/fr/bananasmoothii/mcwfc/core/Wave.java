package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Coords;
import fr.bananasmoothii.mcwfc.core.util.ImmutablePieceNeighborsSet;
import fr.bananasmoothii.mcwfc.core.util.PieceNeighborsSet;
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

    private boolean isCollapsed = false;

    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds) {
        this(pieces, bounds, ThreadLocalRandom.current().nextLong());
    }

    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds, long seed) {
        this.pieces = pieces.immutable();
        pieceSize = pieces.getAny().getCenterPiece().xSize; // assuming all pieces are cubic
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
    public void collapse() {
        if (isCollapsed) return;

        // fill the wave with all possible states for each piece
        for (VirtualSpace.ObjectWithCoordinates<Set<Piece>> node : wave) {
            wave.set(pieces.getCenterPieces(), node.x(), node.y(), node.z());
        }
        // TODO


        isCollapsed = true;
        lastChangedEntropies = null;
    }

    private Stack<VirtualSpace.ObjectWithCoordinates<Set<Piece>>> lastChangedEntropies = new Stack<>();

    /**
     * This searches a node (coordinates) a piece with a low entropy among the last changed entropies
     */
    private @Nullable Coords chooseLowEntropyNode(int lastX, int lastY, int lastZ) {
        return chooseLowEntropyNode(lastX, lastY, lastZ, false);
    }

    /**
     * This searches a node (coordinates) with a low entropy among the last changed entropies
     * @param totalSearch if true, the search is done on the whole wave, otherwise only on the last changed entropies
     */
    private @Nullable Coords chooseLowEntropyNode(int lastX, int lastY, int lastZ, boolean totalSearch) {
        Random random = getRandom(lastX, lastY, lastZ);
        int lowestEntropy = Integer.MAX_VALUE;
        final ArrayList<Coords> lowestEntropyNodes = new ArrayList<>(); // this is a list so every piece has the same chance to be chosen
        final Iterator<VirtualSpace.ObjectWithCoordinates<Set<Piece>>> iterator;
        iterator = totalSearch ? wave.iterator() : lastChangedEntropies.iterator();
        while (iterator.hasNext()) {
            VirtualSpace.ObjectWithCoordinates<Set<Piece>> node = iterator.next();
            final Set<Piece> object = node.object();
            if (object.size() < lowestEntropy) {
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
                throw new IllegalStateException("No available node");
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
}
