package fr.bananasmoothii.mcwfc.core.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Coords(int x, int y, int z) {
    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    @Contract(pure = true)
    public double distanceFrom(@NotNull Coords other) {
        return Math.sqrt(square(x - other.x) + square(y - other.y) + square(z - other.z));
    }

    public static int square(int x) {
        return x * x;
    }
}
