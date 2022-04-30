package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.RotationAngle;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static fr.bananasmoothii.mcwfc.core.util.RotationAngle.*;

/**
 * Represents the neighbors of a piece. It does not contain the centerpiece. It may only have zero or one neighbor per face.
 */
public class PieceNeighbors extends HashMap<Face, Piece> {

    public PieceNeighbors() {
        super(6);
    }
    
    public PieceNeighbors(@NotNull Map<? extends Face, ? extends Piece> m) {
        super(m);
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
        } else {
            PieceNeighbors r90 = rotateY(D90);
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
    public @NotNull PieceNeighbors rotateX(final @NotNull RotationAngle angle) {
        PieceNeighbors result = new PieceNeighbors();
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            result.put(entry.getKey().rotateX(angle), entry.getValue().rotateX(angle));
        }
        return result;
    }

    public @NotNull PieceNeighbors rotateY(final @NotNull RotationAngle angle) {
        PieceNeighbors result = new PieceNeighbors();
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            result.put(entry.getKey().rotateY(angle), entry.getValue().rotateY(angle));
        }
        return result;
    }

    public @NotNull PieceNeighbors rotateZ(final @NotNull RotationAngle angle) {
        PieceNeighbors result = new PieceNeighbors();
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            result.put(entry.getKey().rotateZ(angle), entry.getValue().rotateZ(angle));
        }
        return result;
    }

    public @NotNull PieceNeighbors flipX() {
        PieceNeighbors result = new PieceNeighbors();
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            result.put(entry.getKey().flipX(), entry.getValue().flipX());
        }
        return result;
    }

    public @NotNull PieceNeighbors flipY() {
        PieceNeighbors result = new PieceNeighbors();
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            result.put(entry.getKey().flipY(), entry.getValue().flipY());
        }
        return result;
    }

    public @NotNull PieceNeighbors flipZ() {
        PieceNeighbors result = new PieceNeighbors();
        for (Map.Entry<Face, Piece> entry : entrySet()) {
            result.put(entry.getKey().flipZ(), entry.getValue().flipZ());
        }
        return result;
    }
}
