package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.WeightedSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;

public class Sample1 extends WeightedSet<PieceNeighbors1> {
    public Sample1() {
    }

    public Sample1(WeightedSet<PieceNeighbors1> other) {
        super(other);
    }

    /**
     * Filters this {@link WeightedSet}<{@link PieceNeighbors1}> and returns only the {@link PieceNeighbors1} having this
     * {@link Piece} as {@link PieceNeighbors1#getCenterPiece() center piece}.
     */
    public @NotNull WeightedSet<PieceNeighbors1> getNeighborsFor(@NotNull Piece piece) {
        WeightedSet<PieceNeighbors1> result = new WeightedSet<>();
        for (PieceNeighbors1 neighbors : this) {
            if (neighbors.getCenterPiece() == piece) {
                result.add(neighbors);
            }
        }
        return result;
    }

    /**
     * @return all centerpieces of all {@link PieceNeighbors1}
     */
    @Contract(pure = true)
    public @NotNull WeightedSet<Piece> getCenterPieces() {
        WeightedSet<Piece> result = new WeightedSet<>();
        final Iterator<Map.Entry<PieceNeighbors1, Integer>> iter = elementsAndWeightsIterator();
        while (iter.hasNext()) {
            final Map.Entry<PieceNeighbors1, Integer> entry = iter.next();
            result.add(entry.getKey().getCenterPiece(), entry.getValue());
        }
        return result;
    }

    @Contract(pure = true)
    public boolean centerPiecesContains(Piece piece) {
        for (PieceNeighbors1 pieceNeighbors : this) {
            if (pieceNeighbors.getCenterPiece().equals(piece)) {
                return true;
            }
        }
        return false;
    }

    public ImmutableSample1 immutable() {
        return new ImmutableSample1(this);
    }
}
