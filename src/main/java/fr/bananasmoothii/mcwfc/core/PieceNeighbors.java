package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.RotationAngle;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static fr.bananasmoothii.mcwfc.core.util.RotationAngle.*;

public class PieceNeighbors extends HashMap<Face, Piece> {
    private final @NotNull Piece centerPiece;

    public PieceNeighbors(@NotNull Piece centerPiece) {
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    public PieceNeighbors(Map<? extends Face, ? extends Piece> m, @NotNull Piece centerPiece) {
        super(m);
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    public @NotNull Piece getCenterPiece() {
        return centerPiece;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PieceNeighbors that)) return false;
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
    public @NotNull Set<@NotNull PieceNeighbors> generateSiblings(boolean allowUpsideDown) {
        Set<@NotNull PieceNeighbors> pieces = new HashSet<>();
        pieces.add(this);
        if (allowUpsideDown) {
            pieces.addAll(generateSiblings(false));
            pieces.addAll(rotateZ(D90).generateSiblings(false));
            pieces.addAll(rotateZ(D180).generateSiblings(false));
            pieces.addAll(rotateZ(D270).generateSiblings(false));
            pieces.addAll(rotateX(D90).generateSiblings(false));
            pieces.addAll(rotateX(D270).generateSiblings(false));
            pieces.addAll(flipY().generateSiblings(false));
        } else {
            final PieceNeighbors r90 = rotateY(D90);
            if (pieces.add(r90)) {
                pieces.add(r90.flipX());
                pieces.add(r90.flipZ());
            }
            final PieceNeighbors r180 = rotateY(D180);
            if (pieces.add(r180)) {
                pieces.add(r180.flipX());
                pieces.add(r180.flipZ());
            }
            final PieceNeighbors r270 = rotateY(D270);
            if (pieces.add(r270)) {
                pieces.add(r270.flipX());
                pieces.add(r270.flipZ());
            }
            pieces.add(flipX());
            pieces.add(flipZ());
        }
        return pieces;
    }

    /**
     * @return a rotated version by <i>angle</i> degrees along the X axis
     */
    @Contract(pure = true)
    public @NotNull PieceNeighbors rotateX(final @NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateX(angle));
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().rotateX(angle), entry.getValue().rotateX(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighbors rotateY(final @NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateY(angle));
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().rotateY(angle), entry.getValue().rotateY(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighbors rotateZ(final @NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateZ(angle));
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().rotateZ(angle), entry.getValue().rotateZ(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighbors flipX() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.flipX());
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().flipX(), entry.getValue().flipX());
        }
        return copy;
    }

    public @NotNull PieceNeighbors flipY() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.flipY());
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().flipY(), entry.getValue().flipY());
        }
        return copy;
    }

    public @NotNull PieceNeighbors flipZ() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.flipZ());
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            copy.put(entry.getKey().flipZ(), entry.getValue().flipZ());
        }
        return copy;
    }
}
