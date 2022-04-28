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

        // at the beginning, every piece is possible, the wave is not collapsed, so we add every possibility everywhere
        Set<Piece> possiblePieces = new HashSet<>();
        for (PieceNeighbors piece : pieces) {
            possiblePieces.add(piece.getCenterPiece());
        }
        wave.setFill(possiblePieces);
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

        // TODO

        isCollapsed = true;
        lastChangedEntropies = null;
    }

    private Stack<Coords> lastChangedEntropies = new Stack<>();

    /**
     * This searches a piece with a low entropy among the last changed entropies
     */
    private @Nullable Piece chooseLowEntropyPiece(int lastX, int lastY, int lastZ) {
        return chooseLowEntropyPiece(lastX, lastY, lastZ, false);
    }

    /**
     * This searches a piece with a low entropy among the last changed entropies
     * @param totalSearch if true, the search is done on the whole wave, otherwise only on the last changed entropies
     */
    private @Nullable Piece chooseLowEntropyPiece(int lastX, int lastY, int lastZ, boolean totalSearch) {
        Random random = getRandom(lastX, lastY, lastZ);
        if (totalSearch) {
            int lowestEntropy = Integer.MAX_VALUE;
            Piece lowestEntropyPiece = null;
            for (int x = 0; x < wave.getXSize(); x++) {
                for (int y = 0; y < wave.getYSize(); y++) {
                    for (int z = 0; z < wave.getZSize(); z++) {
                        Set<Piece> piece = wave.get(x, y, z);
                        if ()
                    }
                }
            }
        }
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
