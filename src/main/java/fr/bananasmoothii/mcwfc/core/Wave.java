package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.ImmutablePieceNeighborsSet;
import fr.bananasmoothii.mcwfc.core.util.PieceNeighborsSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Wave {
    private final VirtualSpace<Set<Piece>> wave = new VirtualSpace<>();
    private final ImmutablePieceNeighborsSet pieces;
    private final int pieceSize;
    private final long seed;
    private final List<@NotNull PieceCollapseListener> pieceCollapseListeners = new ArrayList<>();
    private final @NotNull Piece defaultPiece;

    private boolean isCollapsed = false;

    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds, Piece defaultPiece) {
        this(pieces, bounds, ThreadLocalRandom.current().nextLong(), defaultPiece);
    }

    public Wave(@NotNull PieceNeighborsSet pieces, @NotNull Bounds bounds, long seed, @NotNull Piece defaultPiece) {
        this.pieces = pieces.immutable();
        pieceSize = pieces.getAny().getCenterPiece().xSize; // assuming all pieces are cubic
        this.seed = seed;
        this.defaultPiece = Objects.requireNonNull(defaultPiece);
        if (defaultPiece.xSize != pieceSize || defaultPiece.ySize != pieceSize || defaultPiece.zSize != pieceSize)
            throw new IllegalArgumentException("default piece has not the same size, it needs to be a cube of edge " +
                    pieces);
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
