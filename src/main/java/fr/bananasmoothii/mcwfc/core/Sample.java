package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.WeightedSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;

public class Sample extends WeightedSet<PieceNeighbors> {
    public Sample() {
    }

    public Sample(WeightedSet<PieceNeighbors> other) {
        super(other);
    }

    /**
     * Filters this {@link Sample} and returns only the {@link PieceNeighbors} having this
     * {@link Piece} as {@link PieceNeighbors#getCenterPiece() center piece}.
     */
    public @NotNull Sample getNeighborsFor(@NotNull Piece piece) {
        Sample result = new Sample();
        for (PieceNeighbors neighbors : this) {
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
    public @NotNull WeightedSet<Piece> getCenterPieces() {
        WeightedSet<Piece> result = new WeightedSet<>();
        final Iterator<Map.Entry<PieceNeighbors, Integer>> iter = elementsAndWeightsIterator();
        while (iter.hasNext()) {
            final Map.Entry<PieceNeighbors, Integer> entry = iter.next();
            result.add(entry.getKey().getCenterPiece(), entry.getValue());
        }
        return result;
    }

    @Contract(pure = true)
    public boolean centerPiecesContains(Piece piece) {
        for (PieceNeighbors pieceNeighbors : this) {
            final Piece centerPiece = pieceNeighbors.getCenterPiece();
            if (centerPiece.equals(piece)) {
                return true;
            }
        }
        return false;
    }

    public boolean retainAllWithCenterPiece(@NotNull Piece centerPiece) {
        boolean changed = false;
        Iterator<PieceNeighbors> iterator = iterator();
        while (iterator.hasNext()) {
            PieceNeighbors neighbors = iterator.next();
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
    public boolean acceptsAt(@NotNull Face face, @NotNull Piece piece) {
        for (PieceNeighbors neighbors : this) {
            if (neighbors.get(face).equals(piece)) {
                return true;
            }
        }
        return false;
    }

    public ImmutableSample immutable() {
        return new ImmutableSample(this);
    }
}
