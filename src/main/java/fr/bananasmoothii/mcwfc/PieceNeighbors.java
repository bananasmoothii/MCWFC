package fr.bananasmoothii.mcwfc;

import fr.bananasmoothii.mcwfc.util.Face;
import fr.bananasmoothii.mcwfc.util.RotationAngle;
import fr.bananasmoothii.mcwfc.util.Trio;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static fr.bananasmoothii.mcwfc.util.RotationAngle.*;

public class PieceNeighbors {
    private final @NotNull Piece centerPiece;
    private final @NotNull List<Trio<Face, Piece, Integer>> neighbors = new ArrayList<>();

    public PieceNeighbors(@NotNull Piece centerPiece) {
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    /**  
     * @param face the {@link Face} where the neighbor should be put
     * @param piece the {@link Piece} to put there
     * @see #addNeighbor(Face, Piece, int)
     */
    public void addNeighbor(Face face, Piece piece) {
        addNeighbor(face, piece, 1);
    }

    /**
     * Adds a given piece as neighbor at a specific face. If there is already the same piece at the same face, it will
     * just increment "times" by one (see below)
     * @param face the {@link Face} where the neighbor should be put
     * @param piece the {@link Piece} to put there
     * @param times an optional weight, useful for generation. Defaults to 1 in {@link #addNeighbor(Face, Piece)}
     * @see #addNeighbor(Face, Piece)
     */
    public void addNeighbor(Face face, Piece piece, int times) {
        Objects.requireNonNull(face);
        Objects.requireNonNull(piece);
        for (int i = 0, neighborsSize = neighbors.size(); i < neighborsSize; i++) {
            Trio<Face, Piece, Integer> neighbor = neighbors.get(i);
            if (face.equals(neighbor.a()) && piece.equals(neighbor.b())) {
                neighbors.set(i, new Trio<>(neighbor.a(), neighbor.b(), neighbor.c() + 1));
                return;
            }
        }
        neighbors.add(new Trio<>(face, piece, times));
    }

    public @NotNull Piece getCenterPiece() {
        return centerPiece;
    }

    public void fill(@NotNull BlockData blockData) {
        centerPiece.fill(blockData);
        for (Trio<Face, Piece, Integer> neighbor : neighbors) {
            neighbor.b().fill(blockData);
        }
    }

    public PieceNeighbors copy() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.copy());
        copy.neighbors.addAll(neighbors);
        return copy;
    }

    /**
     * @return A set containing all possible rotated and flipped versions of this (it also contains this)
     */
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
     * @return a rotated version by <i>angle</i> degrees along the Y axis
     */
    @Contract(pure = true)
    public @NotNull PieceNeighbors rotateX(@NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateX(angle));
        for (Trio<Face, Piece, Integer> neighbor : neighbors) {
            copy.addNeighbor(neighbor.a().rotateX(angle), neighbor.b().rotateX(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighbors rotateY(@NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateY(angle));
        for (Trio<Face, Piece, Integer> neighbor : neighbors) {
            copy.addNeighbor(neighbor.a().rotateY(angle), neighbor.b().rotateY(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighbors rotateZ(@NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateZ(angle));
        for (Trio<Face, Piece, Integer> neighbor : neighbors) {
            copy.addNeighbor(neighbor.a().rotateZ(angle), neighbor.b().rotateZ(angle));
        }
        return copy;
    }

    public @NotNull PieceNeighbors flipX() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.flipX());
        for (Trio<Face, Piece, Integer> neighbor : neighbors) {
            copy.addNeighbor(neighbor.a().flipX(), neighbor.b().flipX());
        }
        return copy;
    }

    public @NotNull PieceNeighbors flipY() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.flipY());
        for (Trio<Face, Piece, Integer> neighbor : neighbors) {
            copy.addNeighbor(neighbor.a().flipY(), neighbor.b().flipY());
        }
        return copy;
    }

    public @NotNull PieceNeighbors flipZ() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.flipZ());
        for (Trio<Face, Piece, Integer> neighbor : neighbors) {
            copy.addNeighbor(neighbor.a().flipZ(), neighbor.b().flipZ());
        }
        return copy;
    }
    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PieceNeighbors that = (PieceNeighbors) o;

        if (!centerPiece.equals(that.centerPiece)) return false;
        return neighbors.equals(that.neighbors);
    }

    @Override
    public int hashCode() {
        int result = centerPiece.hashCode();
        result = 31 * result + neighbors.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "";
    }
}
