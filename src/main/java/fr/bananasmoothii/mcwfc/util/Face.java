package fr.bananasmoothii.mcwfc.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the face of a block. This is a very similar copy of {@link org.bukkit.block.BlockFace} but with a few
 * additions, removed of unnessery items and added UP and DOWN variants.
 */
public enum Face {
    NORTH(0, 0, -1),
    EAST(1, 0, 0),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0),
    NORTH_EAST(NORTH, EAST),
    NORTH_WEST(NORTH, WEST),
    SOUTH_EAST(SOUTH, EAST),
    SOUTH_WEST(SOUTH, WEST),
    NORTH_EAST_UP(NORTH_EAST, UP),
    NORTH_EAST_DOWN(NORTH_EAST, DOWN),
    NORTH_WEST_UP(NORTH_WEST, UP),
    NORTH_WEST_DOWN(NORTH_WEST, DOWN),
    SOUTH_EAST_UP(SOUTH_EAST, UP),
    SOUTH_EAST_DOWN(SOUTH_EAST, DOWN),
    SOUTH_WEST_UP(SOUTH_WEST, UP),
    SOUTH_WEST_DOWN(SOUTH_WEST, DOWN),
    NORTH_UP(NORTH, UP),
    NORTH_DOWN(NORTH, DOWN),
    EAST_UP(EAST, UP),
    EAST_DOWN(EAST, DOWN),
    SOUTH_UP(SOUTH, UP),
    SOUTH_DOWN(SOUTH, DOWN),
    WEST_UP(WEST, UP),
    WEST_DOWN(WEST, DOWN);

    private final int modX;
    private final int modY;
    private final int modZ;

    Face(final int modX, final int modY, final int modZ) {
        this.modX = modX;
        this.modY = modY;
        this.modZ = modZ;
    }

    Face(final @NotNull Face face1, final @NotNull Face face2) {
        this.modX = face1.getModX() + face2.getModX();
        this.modY = face1.getModY() + face2.getModY();
        this.modZ = face1.getModZ() + face2.getModZ();
    }

    public static @NotNull Face getWithMods(final int modX, final int modY, final int modZ) {
        for (Face face : Face.values()) {
            if (face.modX == modX && face.modY == modY && face.modZ == modZ) return face;
        }
        throw new IllegalArgumentException("Illegal block face: " + modX + ' ' + modY + ' ' + modZ);
    }

    /**
     * Get the amount of X-coordinates to modify to get the represented block
     *
     * @return Amount of X-coordinates to modify
     */
    public int getModX() {
        return modX;
    }

    /**
     * Get the amount of Y-coordinates to modify to get the represented block
     *
     * @return Amount of Y-coordinates to modify
     */
    public int getModY() {
        return modY;
    }

    /**
     * Get the amount of Z-coordinates to modify to get the represented block
     *
     * @return Amount of Z-coordinates to modify
     */
    public int getModZ() {
        return modZ;
    }

    /**
     * Returns true if this face is aligned with one of the unit axes in 3D
     * Cartesian space (ie NORTH, SOUTH, EAST, WEST, UP, DOWN).
     *
     * @return Cartesian status
     */
    public boolean isCartesian() {
        return switch (this) {
            case NORTH, SOUTH, EAST, WEST, UP, DOWN -> true;
            default -> false;
        };
    }

    public @NotNull Face getOppositeFace() {
        return getWithMods(-modX, -modY, -modZ);
    }

    /**
     * @return a rotated version by <i>angle</i> degrees along the X axis
     */
    @Contract(pure = true)
    public @NotNull Face rotateX(RotationAngle angle) {
        return switch (angle) {
            case D90 -> getWithMods(modX, modZ, -modY);
            case D180 -> getWithMods(modX, -modY, -modZ);
            case D270 -> getWithMods(modX, -modZ, modY);
        };
    }

    /**
     * @return a rotated version by <i>angle</i> degrees along the Y axis
     */
    @Contract(pure = true)
    public @NotNull Face rotateY(RotationAngle angle) {
        return switch (angle) {
            case D90 -> getWithMods(-modZ, modY, modX);
            case D180 -> getWithMods(-modX, modY, -modZ);
            case D270 -> getWithMods(modZ, modY, -modX);
        };
    }

    /**
     * @return a rotated version by <i>angle</i> degrees along the Z axis
     */
    @SuppressWarnings("SuspiciousNameCombination")
    @Contract(pure = true)
    public @NotNull Face rotateZ(RotationAngle angle) {
        return switch (angle) {
            case D90 -> getWithMods(modY, -modX, modZ);
            case D180 -> getWithMods(-modX, -modY, modZ);
            case D270 -> getWithMods(-modY, modX, modZ);
        };
    }

    /**
     * @return a flipped version of this piece, with the X coordinates becoming -X
     */
    @Contract(pure = true)
    public @NotNull Face flipX() {
        return getWithMods(-modX, modY, modZ);
    }

    /**
     * @return a flipped version of this piece, with the Y coordinates becoming -Y
     */
    @Contract(pure = true)
    public @NotNull Face flipY() {
        return getWithMods(modX, -modY, modZ);
    }

    /**
     * @return a flipped version of this piece, with the Z coordinates becoming -Z
     */
    @Contract(pure = true)
    public @NotNull Face flipZ() {
        return getWithMods(modX, modY, -modZ);
    }
}

