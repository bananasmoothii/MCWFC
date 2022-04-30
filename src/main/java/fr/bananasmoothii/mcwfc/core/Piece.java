package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.RotationAngle;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static fr.bananasmoothii.mcwfc.bukkit.MCWFCPlugin.log;
import static fr.bananasmoothii.mcwfc.core.util.RotationAngle.*;

public class Piece/*<B>*/ {

    // TODO: change BlockData to B
    private final @NotNull BlockData[][] @NotNull[] data;
    public final int xSize, ySize, zSize;

    public Piece(int size, @NotNull BlockData fillBlock) {
        this(size, size, size, fillBlock);
    }

    public Piece(int xSize, int ySize, int zSize, @NotNull BlockData fillBlock) {
        this(xSize, ySize, zSize);
        fill(fillBlock);
    }

    protected Piece(int xSize, int ySize, int zSize) {
        if (xSize < 1 || ySize < 1 || zSize < 1)
            throw new IllegalArgumentException("Piece size can't be below 1");
        data = new BlockData[xSize][ySize][zSize];
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
    }

    /**
     * @return the block at these positions
     * @throws ArrayIndexOutOfBoundsException if a coordinate is below 0 or above or equal to the size (see xSize, ySize
     * and zSize)
     */
    public @NotNull BlockData get(int x, int y, int z) {
        return data[x][y][z];
    }

    public void set(@NotNull BlockData blockData, int x, int y, int z) {
        data[x][y][z] = Objects.requireNonNull(blockData, "Piece cannot contain null blocks");
    }

    /**
     * replaces every element with this {@link BlockData}
     */
    public void fill(@NotNull BlockData blockData) {
        Objects.requireNonNull(blockData, "fill block must be non-null");
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    data[x][y][z] = blockData;
                }
            }
        }
    }

    public Piece copy() {
        Piece copy = new Piece(xSize, ySize, zSize);
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                System.arraycopy(data[x][y], 0, copy.data[x][y], 0, zSize);
            }
        }
        return copy;
    }

    /**
     * @return A set containing all possible rotated and flipped versions of this (it also contains this)
     */
    public @NotNull Set<@NotNull Piece> generateSiblings(boolean allowUpsideDown) {
        Set<@NotNull Piece> pieces = new HashSet<>();
        pieces.add(this);
        if (allowUpsideDown) {
            pieces.addAll(generateSiblings(false));
            pieces.addAll(rotateZ(D90).generateSiblings(false));
            pieces.addAll(rotateZ(D180).generateSiblings(false));
            pieces.addAll(rotateZ(D270).generateSiblings(false));
            pieces.addAll(rotateX(D90).generateSiblings(false));
            pieces.addAll(rotateX(D270).generateSiblings(false));
        } else {
            Piece r90 = rotateY(D90);
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
    public @NotNull Piece rotateX(@NotNull RotationAngle angle) {
        return switch (angle) {
            case D90 -> {
                Piece copy = new Piece(xSize, zSize, ySize);
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
                Piece copy = new Piece(xSize, zSize, ySize);
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
                Piece copy = new Piece(xSize, ySize, zSize);
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
    public @NotNull Piece rotateY(@NotNull RotationAngle angle) {
        return switch (angle) {
            case D90 -> {
                Piece copy = new Piece(zSize, ySize, xSize);
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
                Piece copy = new Piece(zSize, ySize, xSize);
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
                Piece copy = new Piece(xSize, ySize, zSize);
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
    public @NotNull Piece rotateZ(@NotNull RotationAngle angle) {
        return switch (angle) {
            case D90 -> {
                Piece copy = new Piece(ySize, xSize, zSize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        System.arraycopy(data[x][y], 0, copy.data[ySize - 1 - y][x], 0, zSize);
                    }
                }
                yield copy;
            }
            case D270 -> {
                Piece copy = new Piece(ySize, xSize, zSize);
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        System.arraycopy(data[x][y], 0, copy.data[y][xSize - 1 - x], 0, zSize);
                    }
                }
                yield copy;
            }
            case D180 -> {
                Piece copy = new Piece(xSize, ySize, zSize);
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
    public @NotNull Piece flipX() {
        Piece copy = new Piece(xSize, ySize, zSize);
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
    public @NotNull Piece flipY() {
        Piece copy = new Piece(xSize, ySize, zSize);
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
    public @NotNull Piece flipZ() {
        Piece copy = new Piece(xSize, ySize, zSize);
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
        if (o instanceof Piece piece)
            return Arrays.deepEquals(data, piece.data);
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(data);
    }

    @Contract(pure = true)
    public void debugPrint(int zLayer) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < ySize; y++) {
            for (int x = 0; x < xSize; x++) {
                sb.append(data[x][y][zLayer]);
                sb.append(' ');
            }
            sb.append('\n');
        }
        sb.append('\n');
        log.info(sb.toString());
    }

    @Contract(pure = true)
    public void debugPrint() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < ySize; y++) {
            for (int z = 0; z < zSize; z++) {
                for (int x = 0; x < xSize; x++) {
                    sb.append(data[x][y][z]);
                    sb.append(' ');
                }
                sb.append("   ");
            }
            sb.append('\n');
        }
        sb.append('\n');
        log.info(sb.toString());
    }
}
