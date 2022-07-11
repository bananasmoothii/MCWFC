package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.RotationAngle;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static fr.bananasmoothii.mcwfc.core.util.RotationAngle.*;

/**
 * @param <B> the type of blocks in this piece. In vanilla minecraft, this can be {@code BlockData}.
 */
public class Piece<B> {
    
    private final @NotNull B[][] @NotNull[] data;
    public final int xSize, ySize, zSize;

    public Piece(int size, @NotNull B fillBlock) {
        this(size, size, size, fillBlock);
    }

    public Piece(int xSize, int ySize, int zSize, @NotNull B fillBlock) {
        this(xSize, ySize, zSize);
        fill(fillBlock);
    }

    protected Piece(int xSize, int ySize, int zSize) {
        if (xSize < 1 || ySize < 1 || zSize < 1)
            throw new IllegalArgumentException("Piece size can't be below 1");
        //noinspection unchecked
        data = (B[][][]) new Object[xSize][ySize][zSize];
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
    }

    /**
     * @return the block at these positions
     * @throws ArrayIndexOutOfBoundsException if a coordinate is below 0 or above or equal to the size (see xSize, ySize
     * and zSize)
     */
    public @NotNull B get(int x, int y, int z) {
        return data[x][y][z];
    }

    public void set(@NotNull B block, int x, int y, int z) {
        data[x][y][z] = Objects.requireNonNull(block, "Piece cannot contain null blocks");
        hashCodeCache = null;
    }

    /**
     * replaces every element with this {@link B}
     */
    public void fill(@NotNull B block) {
        Objects.requireNonNull(block, "fill block must be non-null");
        hashCodeCache = null;
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    data[x][y][z] = block;
                }
            }
        }
    }

    /**
     * @return A set containing all possible rotated and flipped versions of this (it also contains this)
     */
    @Contract(pure = true)
    public @NotNull Set<Piece<B>> generateSiblings(boolean allowUpsideDown) {
        Set<Piece<B>> pieces = new HashSet<>();
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
            final Piece<B> r90 = rotateY(D90);
            if (pieces.add(r90)) {
                pieces.add(r90.flipX());
                pieces.add(r90.flipZ());
            }
            final Piece<B> r180 = rotateY(D180);
            if (pieces.add(r180)) {
                pieces.add(r180.flipX());
                pieces.add(r180.flipZ());
            }
            final Piece<B> r270 = rotateY(D270);
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
    public @NotNull Piece<B> rotateX(@NotNull RotationAngle angle) {
        return switch (angle) {
            case D90 -> {
                Piece<B> copy = new Piece<>(xSize, zSize, ySize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        for (int z = 0; z < zSize; z++) {
                            copy.data[x][zSize - 1 - z][y] = data[x][y][z];
                        }
                    }
                }
                yield copy;
            }
            case D270 -> {
                Piece<B> copy = new Piece<>(xSize, zSize, ySize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        for (int z = 0; z < zSize; z++) {
                            copy.data[x][z][ySize - 1 - y] = data[x][y][z];
                        }
                    }
                }
                yield copy;
            }
            case D180 -> {
                Piece<B> copy = new Piece<>(xSize, ySize, zSize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        for (int z = 0; z < zSize; z++) {
                            copy.data[x][ySize - 1 -  y][zSize - 1 - z] = data[x][y][z];
                        }
                    }
                }
                yield copy;
            }
        };
    }

    /**
     * @return a rotated version by <i>angle</i> degrees along the Y axis
     */
    @Contract(pure = true)
    public @NotNull Piece<B> rotateY(@NotNull RotationAngle angle) {
        return switch (angle) {
            case D90 -> {
                Piece<B> copy = new Piece<>(zSize, ySize, xSize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        for (int z = 0; z < zSize; z++) {
                            copy.data[zSize - 1 - z][y][x] = data[x][y][z];
                        }
                    }
                }
                yield copy;
            }
            case D270 -> {
                Piece<B> copy = new Piece<>(zSize, ySize, xSize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        for (int z = 0; z < zSize; z++) {
                            copy.data[z][y][xSize - 1 - x] = data[x][y][z];
                        }
                    }
                }
                yield copy;
            }
            case D180 -> {
                Piece<B> copy = new Piece<>(xSize, ySize, zSize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        for (int z = 0; z < zSize; z++) {
                            copy.data[xSize - 1 - x][y][zSize - 1 - z] = data[x][y][z];
                        }
                    }
                }
                yield copy;
            }
        };
    }

    /**
     * @return a rotated version by <i>angle</i> degrees along the Z axis
     */
    @SuppressWarnings("SuspiciousNameCombination")
    @Contract(pure = true)
    public @NotNull Piece<B> rotateZ(@NotNull RotationAngle angle) {
        return switch (angle) {
            case D90 -> {
                Piece<B> copy = new Piece<>(ySize, xSize, zSize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        System.arraycopy(data[x][y], 0, copy.data[ySize - 1 - y][x], 0, zSize);
                    }
                }
                yield copy;
            }
            case D270 -> {
                Piece<B> copy = new Piece<>(ySize, xSize, zSize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        System.arraycopy(data[x][y], 0, copy.data[y][xSize - 1 - x], 0, zSize);
                    }
                }
                yield copy;
            }
            case D180 -> {
                Piece<B> copy = new Piece<>(xSize, ySize, zSize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        System.arraycopy(data[x][y], 0, copy.data[xSize - 1 - x][ySize - 1 - y], 0, zSize);
                    }
                }
                yield copy;
            }
        };
    }

    /**
     * @return a flipped version of this piece, with the X coordinates becoming -X
     */
    @Contract(pure = true)
    public @NotNull Piece<B> flipX() {
        Piece<B> copy = new Piece<>(xSize, ySize, zSize);
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                System.arraycopy(data[x][y], 0, copy.data[xSize - 1 - x][y], 0, zSize);
            }
        }
        return copy;
    }

    /**
     * @return a flipped version of this piece, with the Y coordinates becoming -Y
     */
    @Contract(pure = true)
    public @NotNull Piece<B> flipY() {
        Piece<B> copy = new Piece<>(xSize, ySize, zSize);
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                System.arraycopy(data[x][y], 0, copy.data[x][ySize - 1 - y], 0, zSize);
            }
        }
        return copy;
    }

    /**
     * @return a flipped version of this piece, with the Z coordinates becoming -Z
     */
    @Contract(pure = true)
    public @NotNull Piece<B> flipZ() {
        Piece<B> copy = new Piece<>(xSize, ySize, zSize);
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    copy.data[x][y][zSize - 1 - z] = data[x][y][z];
                }
            }
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Piece piece && hashCode() == piece.hashCode())
            return Arrays.deepEquals(data, piece.data);
        return false;
    }

    private @Nullable Integer hashCodeCache = null;

    @Override
    public int hashCode() {
        if (hashCodeCache == null) hashCodeCache = Arrays.deepHashCode(data);
        return hashCodeCache;
    }

    public Locked<B> lock() {
        return Locked.of(this);
    }

    /**
     * An immutable version of {@link Piece}
     * @param <B> the type of blocks in this piece. In minecraft, this can be {@link BlockData}.
     */
    public static final class Locked<B> extends Piece<B> {

        private static final ArrayList<Locked<?>> instances = new ArrayList<>();

        private final int hashCode;

        private Locked(@NotNull Piece<B> piece) {
            super(piece.xSize, piece.ySize, piece.zSize);
            for (int x = 0; x < xSize; x++) {
                for (int y = 0; y < ySize; y++) {
                    System.arraycopy(piece.data[x][y], 0, super.data[x][y], 0, zSize);
                }
            }
            hashCode = Arrays.deepHashCode(super.data);
        }

        @SuppressWarnings("unchecked")
        public static <B> @NotNull Locked<B> of(@NotNull Piece<B> piece) {
            for (Locked<?> locked : instances) {
                if (piece.equals(locked)) return (Locked<B>) locked;
            }
            Locked<B> newInstance = new Locked<>(piece);
            instances.add(newInstance);
            return newInstance;
        }

        @Override
        public void set(@NotNull B block, int x, int y, int z) {
            throw new UnsupportedOperationException("This piece is locked");
        }

        @Override
        public void fill(@NotNull B block) {
            throw new UnsupportedOperationException("This piece is locked");
        }

        /**
         * In a {@link Locked locked Piece}, if two objects are equal, they are the same object. This is the hole
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
        public Locked<B> lock() {
            return this;
        }
        
        public @NotNull Set<Piece.Locked<B>> generateSiblingsLock(boolean allowUpsideDown) {
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
                final Locked<B> r90 = rotateY(D90);
                if (pieces.add(r90)) {
                    pieces.add(r90.flipX());
                    pieces.add(r90.flipZ());
                }
                final Locked<B> r180 = rotateY(D180);
                if (pieces.add(r180)) {
                    pieces.add(r180.flipX());
                    pieces.add(r180.flipZ());
                }
                final Locked<B> r270 = rotateY(D270);
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
        public @NotNull Locked<B> rotateX(@NotNull RotationAngle angle) {
            return super.rotateX(angle).lock();
        }

        @Override
        public @NotNull Locked<B> rotateY(@NotNull RotationAngle angle) {
            return super.rotateY(angle).lock();
        }

        @Override
        public @NotNull Locked<B> rotateZ(@NotNull RotationAngle angle) {
            return super.rotateZ(angle).lock();
        }

        @Override
        public @NotNull Locked<B> flipX() {
            return super.flipX().lock();
        }

        @Override
        public @NotNull Locked<B> flipY() {
            return super.flipY().lock();
        }

        @Override
        public @NotNull Locked<B> flipZ() {
            return super.flipZ().lock();
        }
    }

    @Contract(pure = true)
    public void debugPrint(int zLayer) {
        for (int y = 0; y < ySize; y++) {
            for (int x = 0; x < xSize; x++) {
                System.out.print(data[x][y][zLayer]);
                System.out.print(' ');
            }
            System.out.print('\n');
        }
        System.out.print('\n');
    }

    @Contract(pure = true)
    public void debugPrint() {
        for (int y = 0; y < ySize; y++) {
            for (int z = 0; z < zSize; z++) {
                for (int x = 0; x < xSize; x++) {
                    System.out.print(data[x][y][z]);
                    System.out.print(' ');
                }
                System.out.print("   ");
            }
            System.out.print('\n');
        }
        System.out.print('\n');
    }
}
