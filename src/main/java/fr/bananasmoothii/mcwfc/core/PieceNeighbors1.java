package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.RotationAngle;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static fr.bananasmoothii.mcwfc.core.util.RotationAngle.*;

public class PieceNeighbors1 extends HashMap<Face, Piece> {
    private final @NotNull Piece centerPiece;

    public PieceNeighbors1(@NotNull Piece centerPiece) {
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    public PieceNeighbors1(Map<? extends Face, ? extends Piece> m, @NotNull Piece centerPiece) {
        super(m);
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    public @NotNull Piece getCenterPiece() {
        return centerPiece;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PieceNeighbors1 that)) return false;
        if (!super.equals(o)) return false;

        return centerPiece.equals(that.centerPiece);
    }

    @Override
    public int hashCode() {
        int copy = super.hashCode();
        copy = 31 * copy + centerPiece.hashCode();
        return copy;
    }

    /**
     * @return A set containing all possible rotated and flipped versions of this (it also contains this)
     */
    @Contract(pure = true)
    public @NotNull Set<@NotNull PieceNeighbors1> generateSiblings(boolean allowUpsideDown) {
        Set<@NotNull PieceNeighbors1> pieces = new HashSet<>();
        pieces.add(this);
        if (allowUpsideDown) {
            pieces.addAll(generateSiblings(false));
            pieces.addAll(rotateZ(D90).generateSiblings(false));
            pieces.addAll(rotateZ(D180).generateSiblings(false));
            pieces.addAll(rotateZ(D270).generateSiblings(false));
            pieces.addAll(rotateX(D90).generateSiblings(false));
            pieces.addAll(rotateX(D270).generateSiblings(false));
        } else {
            PieceNeighbors1 r90 = rotateY(D90);
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
    public @NotNull PieceNeighbors1 rotateX(final @NotNull RotationAngle angle) {
        PieceNeighbors1 copy = new PieceNeighbors1(centerPiece);
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().rotateX(angle), entry.getValue().rotateX(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighbors1 rotateY(final @NotNull RotationAngle angle) {
        PieceNeighbors1 copy = new PieceNeighbors1(centerPiece);
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().rotateY(angle), entry.getValue().rotateY(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighbors1 rotateZ(final @NotNull RotationAngle angle) {
        PieceNeighbors1 copy = new PieceNeighbors1(centerPiece);
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().rotateZ(angle), entry.getValue().rotateZ(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighbors1 flipX() {
        PieceNeighbors1 copy = new PieceNeighbors1(centerPiece);
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().flipX(), entry.getValue().flipX());
        }
        return copy;
    }

    public @NotNull PieceNeighbors1 flipY() {
        PieceNeighbors1 copy = new PieceNeighbors1(centerPiece);
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().flipY(), entry.getValue().flipY());
        }
        return copy;
    }

    public @NotNull PieceNeighbors1 flipZ() {
        PieceNeighbors1 copy = new PieceNeighbors1(centerPiece);
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().flipZ(), entry.getValue().flipZ());
        }
        return copy;
    }
}
