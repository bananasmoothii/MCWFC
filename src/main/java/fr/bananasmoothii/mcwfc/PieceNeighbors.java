package fr.bananasmoothii.mcwfc;

import fr.bananasmoothii.mcwfc.util.Pair;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PieceNeighbors {
    private final @NotNull Piece centerPiece;
    private final @NotNull List<Pair<BlockFace, Piece>> neighbors = new ArrayList<>();

    public PieceNeighbors(@NotNull Piece centerPiece) {
        this.centerPiece = centerPiece;
    }

    public void addNeighbor(BlockFace face, Piece piece) {
        neighbors.add(new Pair<>(face, piece));
    }

    public void fill(@NotNull BlockData blockData) {
        centerPiece.fill(blockData);
        for (Pair<BlockFace, Piece> neighbor : neighbors) {
            neighbor.b().fill(blockData);
        }
    }

    public PieceNeighbors copy() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.copy());
        copy.neighbors.addAll(neighbors);
        return copy;
    }
/*
    public @NotNull Set<@NotNull PieceNeighbors> generateSiblings(boolean allowUpsideDown) {

    }

    /**
     * @return a rotated version by <i>angle</i> degrees along the Y axis
     *
    @Contract(pure = true)
    public @NotNull PieceNeighbors rotateX(@NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateX(angle));
        for (Pair<BlockFace, Piece> neighbor : neighbors) {

        }
    }

    public @NotNull PieceNeighbors rotateY(@NotNull RotationAngle angle) {

    }

    public @NotNull PieceNeighbors rotateZ(@NotNull RotationAngle angle) {

    }

    public @NotNull PieceNeighbors flipX() {

    }

    public @NotNull PieceNeighbors flipY() {

    }

    public @NotNull PieceNeighbors flipZ() {

    }

 */

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
}
