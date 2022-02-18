package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.RotationAngle;
import fr.bananasmoothii.mcwfc.core.util.WeightedSet;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static fr.bananasmoothii.mcwfc.core.util.RotationAngle.*;

public class PieceNeighbors {
    private final @NotNull Piece centerPiece;
    private final @NotNull Map<Face, WeightedSet<Piece>> neighbors = new HashMap<>();

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
     * just increment "times" by one (see below). If there is already a piece at that face, it won't be replaced (that's
     * the hole point of this class), both pieces will remain present.
     * @param face the {@link Face} where the neighbor should be put
     * @param piece the {@link Piece} to put there
     * @param times an optional weight, useful for generation. Defaults to 1 in {@link #addNeighbor(Face, Piece)}. Must
     *              be greater or equal to 1.
     * @see #addNeighbor(Face, Piece)
     */
    public void addNeighbor(Face face, Piece piece, int times) {
        Objects.requireNonNull(face);
        Objects.requireNonNull(piece);
        if (times < 1) throw new IllegalArgumentException("Cannot add 0 or less times a piece");

        neighbors.computeIfAbsent(face, face1 -> new WeightedSet<>()) // create a new record for that face if not present
                .add(piece, times);
    }

    public void addAllNeighbors(Face face, @NotNull WeightedSet<Piece> pieces) {
        if (pieces.containsNonNormalWeights()) throw new IllegalArgumentException("Cannot add 0 or less times a piece");
        neighbors.computeIfAbsent(Objects.requireNonNull(face), face1 -> new WeightedSet<>())
                .addAll(pieces);
    }

    public @NotNull Piece getCenterPiece() {
        return centerPiece;
    }

    public void fill(@NotNull BlockData blockData) {
        centerPiece.fill(blockData);
        for (WeightedSet<Piece> pieceWeight : neighbors.values()) {
            for (Piece piece : pieceWeight) {
                piece.fill(blockData);
            }
        }
    }

    /**
     * @return The map used internally itself, that you may modify. Be careful with it. Integer is number of times that
     * piece was recorded for that Face.
     */
    public @NotNull Map<Face, WeightedSet<Piece>> getNeighbors() {
        return neighbors;
    }

    /**
     * @return a map with the pieces and the corresponding weights. It is the same as used internally, so be careful with
     * it.
     */
    public @NotNull WeightedSet<Piece> getNeighbors(@NotNull Face face) {
        return neighbors.get(face);
    }

    public PieceNeighbors copy() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.copy());
        copy.neighbors.putAll(neighbors);
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
    public @NotNull PieceNeighbors rotateX(final @NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateX(angle));
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().rotateX(angle), entry.getValue().mapElements(piece -> piece.rotateX(angle)));
        }
        return copy;
    }

    public @NotNull PieceNeighbors rotateY(final @NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateY(angle));
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().rotateY(angle), entry.getValue().mapElements(piece -> piece.rotateY(angle)));
        }
        return copy;
    }

    public @NotNull PieceNeighbors rotateZ(final @NotNull RotationAngle angle) {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.rotateZ(angle));
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().rotateZ(angle), entry.getValue().mapElements(piece -> piece.rotateZ(angle)));
        }
        return copy;
    }

    public @NotNull PieceNeighbors flipX() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.flipX());
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().flipX(), entry.getValue().mapElements(Piece::flipX));
        }
        return copy;
    }

    public @NotNull PieceNeighbors flipY() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.flipY());
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().flipY(), entry.getValue().mapElements(Piece::flipY));
        }
        return copy;
    }

    public @NotNull PieceNeighbors flipZ() {
        PieceNeighbors copy = new PieceNeighbors(centerPiece.flipZ());
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().flipZ(), entry.getValue().mapElements(Piece::flipZ));
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
}
