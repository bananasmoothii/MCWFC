package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.RotationAngle;
import fr.bananasmoothii.mcwfc.core.util.WeightedSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static fr.bananasmoothii.mcwfc.core.util.RotationAngle.*;

public class PieceNeighborsPossibilities extends WeightedSet<PieceNeighbors> {

    private final @NotNull Piece centerPiece;

    public PieceNeighborsPossibilities(@NotNull Piece centerPiece) {
        super();
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    public PieceNeighborsPossibilities(WeightedSet<PieceNeighbors> other, @NotNull Piece centerPiece) {
        super(other);
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    public PieceNeighborsPossibilities(PieceNeighborsPossibilities other) {
        super(other);
        this.centerPiece = other.centerPiece;
    }

    public @NotNull Piece getCenterPiece() {
        return centerPiece;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PieceNeighborsPossibilities)) return false;
        if (!super.equals(o)) return false;

        PieceNeighborsPossibilities that = (PieceNeighborsPossibilities) o;

        return centerPiece.equals(that.centerPiece);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + centerPiece.hashCode();
        return result;
    }

    /**
     * @return A set containing all possible rotated and flipped versions of this (it also contains this)
     */
    public @NotNull Set<@NotNull PieceNeighborsPossibilities> generateSiblings(boolean allowUpsideDown) {
        Set<@NotNull PieceNeighborsPossibilities> pieces = new HashSet<>();
        pieces.add(this);
        if (allowUpsideDown) {
            pieces.addAll(generateSiblings(false));
            pieces.addAll(rotateZ(D90).generateSiblings(false));
            pieces.addAll(rotateZ(D180).generateSiblings(false));
            pieces.addAll(rotateZ(D270).generateSiblings(false));
            pieces.addAll(rotateX(D90).generateSiblings(false));
            pieces.addAll(rotateX(D270).generateSiblings(false));
        } else {
            PieceNeighborsPossibilities r90 = rotateY(D90);
            if (pieces.add(r90)) {
                pieces.add(r90.flipX());
                pieces.add(r90.flipZ());
            }
            pieces.add(rotateY(D180));
            pieces.add(rotateY(D270));
            pieces.add(flipX());
            pieces.add(flipZ());
        }
        return pieces;
    }

    /**
     * @return a rotated version by <i>angle</i> degrees along the X axis
     */
    @Contract(pure = true)
    public @NotNull PieceNeighborsPossibilities rotateX(final @NotNull RotationAngle angle) {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.rotateX(angle));
        for (PieceNeighbors pieceNeighbors : this) {
            copy.add(pieceNeighbors.rotateX(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities rotateY(final @NotNull RotationAngle angle) {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.rotateY(angle));
        for (PieceNeighbors pieceNeighbors : this) {
            copy.add(pieceNeighbors.rotateY(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities rotateZ(final @NotNull RotationAngle angle) {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.rotateZ(angle));
        for (PieceNeighbors pieceNeighbors : this) {
            copy.add(pieceNeighbors.rotateZ(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities flipX() {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.flipX());
        for (PieceNeighbors pieceNeighbors : this) {
            copy.add(pieceNeighbors.flipX());
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities flipY() {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.flipY());
        for (PieceNeighbors pieceNeighbors : this) {
            copy.add(pieceNeighbors.flipY());
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities flipZ() {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.flipZ());
        for (PieceNeighbors pieceNeighbors : this) {
            copy.add(pieceNeighbors.flipZ());
        }
        return copy;
    }
}
