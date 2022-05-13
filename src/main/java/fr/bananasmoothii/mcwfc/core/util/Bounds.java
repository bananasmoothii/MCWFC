package fr.bananasmoothii.mcwfc.core.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public record Bounds(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax) implements Iterable<Coords> {
    public Bounds {
        if (xMin > xMax || yMin > yMax || zMin > zMax)
            throw new IllegalArgumentException("lower bounds must be lower than upper bounds");
    }

    public boolean contains(@NotNull Coords coords) {
        return contains(coords.x(), coords.y(), coords.z());
    }

    public boolean contains(int x, int y, int z) {
        return xMin <= x && x <= xMax && yMin <= y && y <= yMax && zMin <= z && z <= zMax;
    }

    @Contract(value = "_, _, _, _, _, _ -> new", pure = true)
    public static @NotNull Bounds fromTo(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (x1 > x2) {
            int temp = x1;
            x1 = x2;
            x2 = temp;
        }
        if (y1 > y2) {
            int temp = y1;
            y1 = y2;
            y2 = temp;
        }
        if (z1 > z2) {
            int temp = z1;
            z1 = z2;
            z2 = temp;
        }
        return new Bounds(x1, y1, z1, x2, y2, z2);
    }

    @NotNull
    @Override
    public Iterator<Coords> iterator() {
        return new Iterator<>() {
            private int currentX = xMin, currentY = yMin, currentZ = zMin;

            @Override
            @Contract(pure = true)
            public boolean hasNext() {
                return currentZ <= zMax && currentY <= yMax && currentX <= xMax;
            }

            @Override
            public Coords next() {
                Coords element = new Coords(currentX, currentY, currentZ);
                if (++currentZ > zMax) {
                    currentZ = zMin;
                    currentY++;
                    if (currentY > yMax) {
                        currentY = yMin;
                        currentX++;
                        if (currentX > xMax && currentY > yMax && currentZ > zMax)
                            throw new NoSuchElementException("indexes above maximum");
                    }
                }
                return element;
            }
        };
    }
}
