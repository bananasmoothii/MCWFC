package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.RotationAngle;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static fr.bananasmoothii.mcwfc.core.util.RotationAngle.*;

/**
 * @param <B> the type of blocks in this piece. In minecraft, this can be {@link BlockData}.
 */
public class PieceNeighbors<B> extends HashMap<Face, Optional<Piece.Locked<B>>> {
    private final @NotNull Piece.Locked<B> centerPiece;

    public PieceNeighbors(@NotNull Piece.Locked<B> centerPiece) {
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    public PieceNeighbors(Map<? extends Face, ? extends Optional<Piece.Locked<B>>> m, @NotNull Piece.Locked<B> centerPiece) {
        super(m);
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    public @NotNull Piece.Locked<B> getCenterPiece() {
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
    public @NotNull Set<PieceNeighbors<B>> generateSiblings(boolean allowUpsideDown) {
        Set<PieceNeighbors<B>> pieces = new HashSet<>();
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
            final PieceNeighbors<B> r90 = rotateY(D90);
            if (pieces.add(r90)) {
                pieces.add(r90.flipX());
                pieces.add(r90.flipZ());
            }
            final PieceNeighbors<B> r180 = rotateY(D180);
            if (pieces.add(r180)) {
                pieces.add(r180.flipX());
                pieces.add(r180.flipZ());
            }
            final PieceNeighbors<B> r270 = rotateY(D270);
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
    public @NotNull PieceNeighbors<B> rotateX(final @NotNull RotationAngle angle) {
        PieceNeighbors<B> copy = new PieceNeighbors<>(centerPiece.rotateX(angle));
        for (Map.Entry<Face, Optional<Piece.Locked<B>>> entry : entrySet()) {
            copy.put(entry.getKey().rotateX(angle), entry.getValue().map(piece -> piece.rotateX(angle)));
        }
        return copy;
    }

    public @NotNull PieceNeighbors<B> rotateY(final @NotNull RotationAngle angle) {
        PieceNeighbors<B> copy = new PieceNeighbors<>(centerPiece.rotateY(angle));
        for (Map.Entry<Face, Optional<Piece.Locked<B>>> entry : entrySet()) {
            copy.put(entry.getKey().rotateY(angle), entry.getValue().map(piece -> piece.rotateY(angle)));
        }
        return copy;
    }

    public @NotNull PieceNeighbors<B> rotateZ(final @NotNull RotationAngle angle) {
        PieceNeighbors<B> copy = new PieceNeighbors<>(centerPiece.rotateZ(angle));
        for (Map.Entry<Face, Optional<Piece.Locked<B>>> entry : entrySet()) {
            copy.put(entry.getKey().rotateZ(angle), entry.getValue().map(piece -> piece.rotateZ(angle)));
        }
        return copy;
    }

    public @NotNull PieceNeighbors<B> flipX() {
        PieceNeighbors<B> copy = new PieceNeighbors<>(centerPiece.flipX());
        for (Map.Entry<Face, Optional<Piece.Locked<B>>> entry : entrySet()) {
            copy.put(entry.getKey().flipX(), entry.getValue().map(Piece.Locked::flipX));
        }
        return copy;
    }

    public @NotNull PieceNeighbors<B> flipY() {
        PieceNeighbors<B> copy = new PieceNeighbors<>(centerPiece.flipY());
        for (Map.Entry<Face, Optional<Piece.Locked<B>>> entry : entrySet()) {
            copy.put(entry.getKey().flipY(), entry.getValue().map(Piece.Locked::flipY));
        }
        return copy;
    }

    public @NotNull PieceNeighbors<B> flipZ() {
        PieceNeighbors<B> copy = new PieceNeighbors<>(centerPiece.flipZ());
        for (Map.Entry<Face, Optional<Piece.Locked<B>>> entry : entrySet()) {
            copy.put(entry.getKey().flipZ(), entry.getValue().map(Piece.Locked::flipZ));
        }
        return copy;
    }

    public @NotNull Locked<B> lock() {
        return Locked.of(this);
    }

    public static final class Locked<B> extends PieceNeighbors<B> {

        private static final ArrayList<Locked<?>> instances = new ArrayList<>();

        private final int hashCode;

        private Locked(final @NotNull PieceNeighbors<B> pieceNeighbors) {
            super(pieceNeighbors, pieceNeighbors.centerPiece);
            hashCode = pieceNeighbors.hashCode();
        }

        @SuppressWarnings("unchecked")
        public static <B> @NotNull Locked<B> of(@NotNull PieceNeighbors<B> pieceNeighbors) {
            for (Locked<?> locked : instances) {
                if (pieceNeighbors.equals(locked)) return (Locked<B>) locked;
            }
            Locked<B> newInstance = new Locked<>(pieceNeighbors);
            instances.add(newInstance);
            return newInstance;
        }

        /**
         * In a {@link PieceNeighbors.Locked locked PieceNeighbors}, if two objects are equal, they are the same object. This is the hole
         * point of this class.
         */
        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public @NotNull Locked<B> lock() {
            return this;
        }

        public @NotNull Set<PieceNeighbors.Locked<B>> generateSiblingsLock(boolean allowUpsideDown) {
            Set<Locked<B>> pieces = new HashSet<>();
            pieces.add(this);
            if (allowUpsideDown) {
                pieces.addAll(generateSiblingsLock(false));
                pieces.addAll(rotateZ(D90).generateSiblingsLock(false));
                pieces.addAll(rotateZ(D180).generateSiblingsLock(false));
                pieces.addAll(rotateZ(D270).generateSiblingsLock(false));
                pieces.addAll(rotateX(D90).generateSiblingsLock(false));
                pieces.addAll(rotateX(D270).generateSiblingsLock(false));
                pieces.addAll(flipY().generateSiblingsLock(false));
            } else {
                final PieceNeighbors.Locked<B> r90 = rotateY(D90);
                if (pieces.add(r90)) {
                    pieces.add(r90.flipX());
                    pieces.add(r90.flipZ());
                }
                final PieceNeighbors.Locked<B> r180 = rotateY(D180);
                if (pieces.add(r180)) {
                    pieces.add(r180.flipX());
                    pieces.add(r180.flipZ());
                }
                final PieceNeighbors.Locked<B> r270 = rotateY(D270);
                if (pieces.add(r270)) {
                    pieces.add(r270.flipX());
                    pieces.add(r270.flipZ());
                }
                pieces.add(flipX());
                pieces.add(flipZ());
            }
            return pieces;
        }

        @Override
        public @NotNull PieceNeighbors.Locked<B> rotateX(@NotNull RotationAngle angle) {
            return super.rotateX(angle).lock();
        }

        @Override
        public @NotNull PieceNeighbors.Locked<B> rotateY(@NotNull RotationAngle angle) {
            return super.rotateY(angle).lock();
        }

        @Override
        public @NotNull PieceNeighbors.Locked<B> rotateZ(@NotNull RotationAngle angle) {
            return super.rotateZ(angle).lock();
        }

        @Override
        public @NotNull PieceNeighbors.Locked<B> flipX() {
            return super.flipX().lock();
        }

        @Override
        public @NotNull PieceNeighbors.Locked<B> flipY() {
            return super.flipY().lock();
        }

        @Override
        public @NotNull PieceNeighbors.Locked<B> flipZ() {
            return super.flipZ().lock();
        }
    }
}
