package fr.bananasmoothii.mcwfc.util;

public record Bounds(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax) {
    public Bounds {
        if (xMin > xMax || yMin > yMax || zMin > zMax)
            throw new IllegalArgumentException("lower bounds must be lower than upper bounds");
    }

    public boolean isInBounds(int x, int y, int z) {
        return xMin <= x && x <= xMax && yMin <= y && y <= yMax && zMin <= z && z <= zMax;
    }


}
