package fr.bananasmoothii.mcwfc.core.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the face of a block. This is a very similar copy of {@link org.bukkit.block.BlockFace} but with a few
 * additions, removed of unnessery items and added UP and DOWN variants.
 */
public enum Face {
    NORTH(0, 0, -1),     // 0 0 -1
    EAST(1, 0, 0),       // 1 0 0
    SOUTH(0, 0, 1),      // 0 0 1
    WEST(-1, 0, 0),      // -1 0 0
    TOP(0, 1, 0),        // 0 1 0
    BOTTOM(0, -1, 0),    // 0 -1 0
    NORTH_EAST(NORTH, EAST),               // 1 0 -1
    NORTH_WEST(NORTH, WEST),               // -1 0 -1
    SOUTH_EAST(SOUTH, EAST),               // 1 0 1
    SOUTH_WEST(SOUTH, WEST),               // -1 0 1
    NORTH_EAST_TOP(NORTH_EAST, TOP),       // 1 1 -1
    NORTH_EAST_BOTTOM(NORTH_EAST, BOTTOM), // 1 -1 -1
    NORTH_WEST_TOP(NORTH_WEST, TOP),       // -1 1 -1
    NORTH_WEST_BOTTOM(NORTH_WEST, BOTTOM), // -1 -1 -1
    SOUTH_EAST_TOP(SOUTH_EAST, TOP),       // 1 1 1
    SOUTH_EAST_BOTTOM(SOUTH_EAST, BOTTOM), // 1 -1 1
    SOUTH_WEST_TOP(SOUTH_WEST, TOP),       // -1 1 1
    SOUTH_WEST_BOTTOM(SOUTH_WEST, BOTTOM), // -1 -1 1
    NORTH_TOP(NORTH, TOP),                 // 0 1 -1
    NORTH_BOTTOM(NORTH, BOTTOM),           // 0 -1 -1
    EAST_TOP(EAST, TOP),                   // 1 1 0
    EAST_BOTTOM(EAST, BOTTOM),             // 1 -1 0
    SOUTH_TOP(SOUTH, TOP),                 // 0 1 1
    SOUTH_BOTTOM(SOUTH, BOTTOM),           // 0 -1 1
    WEST_TOP(WEST, TOP),                   // -1 1 0
    WEST_BOTTOM(WEST, BOTTOM);             // -1 -1 0

    private final byte modX;
    private final byte modY;
    private final byte modZ;

    Face(final byte modX, final byte modY, final byte modZ) {
        this.modX = modX;
        this.modY = modY;
        this.modZ = modZ;
    }

    Face(final int modX, final int modY, final int modZ) {
        this((byte) modX, (byte) modY, (byte) modZ);
    }

    Face(final @NotNull Face face1, final @NotNull Face face2) {
        this.modX = (byte) (face1.getModX() + face2.getModX());
        this.modY = (byte) (face1.getModY() + face2.getModY());
        this.modZ = (byte) (face1.getModZ() + face2.getModZ());
    }

    public static @NotNull Face getWithMods(final int modX, final int modY, final int modZ) {
        /*
        for (Face face : Face.values()) {
            if (face.modX == modX && face.modY == modY && face.modZ == modZ) return face;
        }
        */
        // making some tiny optimisation here to avoid iterating over the hole list of enum constants each time
        // I know it's huge, but it should be faster (not tested tho)
        switch (modX) {
            case -1 -> {
                switch (modY) {
                    case -1 -> {
                        switch (modZ) {
                            case -1 -> {
                                return NORTH_WEST_BOTTOM;
                            }
                            case 0 -> {
                                return WEST_BOTTOM;
                            }
                            case 1 -> {
                                return SOUTH_WEST_BOTTOM;
                            }
                        }
                    }
                    case 0 -> {
                        switch (modZ) {
                            case -1 -> {
                                return NORTH_WEST;
                            }
                            case 0 -> {
                                return WEST;
                            }
                            case 1 -> {
                                return  SOUTH_WEST;
                            }
                        }
                    }
                    case 1 -> {
                        switch (modZ) {
                            case -1 -> {
                                return NORTH_WEST_TOP;
                            }
                            case 0 -> {
                                return WEST_TOP;
                            }
                            case 1 -> {
                                return SOUTH_WEST_TOP;
                            }
                        }
                    }
                }
            }
            case 0 -> {
                switch (modY) {
                    case -1 -> {
                        switch (modZ) {
                            case -1 -> {
                                return NORTH_BOTTOM;
                            }
                            case 0 -> {
                                return BOTTOM;
                            }
                            case 1 -> {
                                return SOUTH_BOTTOM;
                            }
                        }
                    }
                    case 0 -> {
                        switch (modZ) {
                            case -1 -> {
                                return NORTH;
                            }
                            case 0 -> 
                                    throw new IllegalArgumentException("Self is not allowed (face coordinates where 0 0 0)");
                            case 1 -> {
                                return SOUTH;
                            }
                        }
                    }
                    case 1 -> {
                        switch (modZ) {
                            case -1 -> {
                                return NORTH_TOP;
                            }
                            case 0 -> {
                                return TOP;
                            }
                            case 1 -> {
                                return SOUTH_TOP;
                            }
                        }
                    }
                }
            }
            case 1 -> {
                switch (modY) {
                    case -1 -> {
                        switch (modZ) {
                            case -1 -> {
                                return NORTH_EAST_BOTTOM;
                            }
                            case 0 -> {
                                return EAST_BOTTOM;
                            }
                            case 1 -> {
                                return SOUTH_EAST_BOTTOM;
                            }
                        }
                    }
                    case 0 -> {
                        switch (modZ) {
                            case -1 -> {
                                return NORTH_EAST;
                            }
                            case 0 -> {
                                return EAST;
                            }
                            case 1 -> {
                                return  SOUTH_EAST;
                            }
                        }
                    }
                    case 1 -> {
                        switch (modZ) {
                            case -1 -> {
                                return NORTH_EAST_TOP;
                            }
                            case 0 -> {
                                return EAST_TOP;
                            }
                            case 1 -> {
                                return SOUTH_EAST_TOP;
                            }
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("Illegal block face coordinates: " + modX + ' ' + modY + ' ' + modZ);
    }

    /**
     * Get the amount of X-coordinates to modify to get the represented block
     *
     * @return Amount of X-coordinates to modify
     */
    public byte getModX() {
        return modX;
    }

    /**
     * Get the amount of Y-coordinates to modify to get the represented block
     *
     * @return Amount of Y-coordinates to modify
     */
    public byte getModY() {
        return modY;
    }

    /**
     * Get the amount of Z-coordinates to modify to get the represented block
     *
     * @return Amount of Z-coordinates to modify
     */
    public byte getModZ() {
        return modZ;
    }

    /**
     * Returns true if this face is aligned with one of the unit axes in 3D
     * Cartesian space (ie {@link #NORTH}, {@link #SOUTH}, {@link #EAST}, {@link #WEST}, {@link #TOP}, {@link #BOTTOM}).
     *
     * @return Cartesian status
     */
    public boolean isCartesian() {
        return switch (this) {
            case NORTH, SOUTH, EAST, WEST, TOP, BOTTOM -> true;
            default -> false;
        };
    }

    private static final List<Face> CARTESIAN_FACES = List.of(NORTH, SOUTH, EAST, WEST, TOP, BOTTOM);

    public static List<Face> getCartesianFaces() {
        return CARTESIAN_FACES;
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

