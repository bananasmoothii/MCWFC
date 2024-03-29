package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.WeightedSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * @param <B> the type of the blocks in the {@link Piece}s. For example, in bukkit, this is {@link org.bukkit.block.data.BlockData}
 */
public class Sample<B> extends WeightedSet<PieceNeighbors.Locked<B>> {
    public Sample() {
    }

    public Sample(WeightedSet<PieceNeighbors.Locked<B>> other) {
        super(other);
    }

    /**
     * Filters this {@link Sample} and returns only the {@link PieceNeighbors} having this
     * {@link Piece} as {@link PieceNeighbors#getCenterPiece() center piece}.
     */
    public @NotNull Sample<B> getNeighborsFor(@NotNull Piece.Locked<B> piece) {
        Sample<B> result = new Sample<>();
        for (PieceNeighbors.Locked<B> neighbors : this) {
            if (neighbors.getCenterPiece().equals(piece)) {
                result.add(neighbors);
            }
        }
        return result;
    }

    /**
     * @return all centerpieces of all {@link PieceNeighbors}
     */
    @Contract(pure = true)
    public @NotNull WeightedSet<Piece.Locked<B>> getCenterPieces() {
        WeightedSet<Piece.Locked<B>> result = new WeightedSet<>();
        final Iterator<Map.Entry<PieceNeighbors.Locked<B>, Integer>> iter = elementsAndWeightsIterator();
        while (iter.hasNext()) {
            final Map.Entry<PieceNeighbors.Locked<B>, Integer> entry = iter.next();
            result.add(entry.getKey().getCenterPiece(), entry.getValue());
        }
        return result;
    }

    @Contract(pure = true)
    public boolean centerPiecesContains(Piece.Locked<B> piece) {
        for (PieceNeighbors.Locked<B> pieceNeighbors : this) {
            final Piece.Locked<B> centerPiece = pieceNeighbors.getCenterPiece();
            if (centerPiece.equals(piece)) {
                return true;
            }
        }
        return false;
    }

    public boolean retainAllWithCenterPiece(@NotNull Piece.Locked<B> centerPiece) {
        boolean changed = false;
        Iterator<PieceNeighbors.Locked<B>> iterator = iterator();
        while (iterator.hasNext()) {
            PieceNeighbors.Locked<B> neighbors = iterator.next();
            if (!neighbors.getCenterPiece().equals(centerPiece)) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    /**
     * @return true if there is at least one {@link PieceNeighbors} having this {@link Piece} at that {@link Face}
     */
    public boolean acceptsAt(@NotNull Face face, @NotNull Piece.Locked<B> piece) {
        for (PieceNeighbors.Locked<B> neighbors : this) {
            final Optional<Piece.Locked<B>> neighbor = neighbors.get(face);
            if (neighbor.isPresent() && neighbor.get().equals(piece)) return true;
        }
        return false;
    }

    public ImmutableSample<B> immutable() {
        return new ImmutableSample<>(this);
    }
}
