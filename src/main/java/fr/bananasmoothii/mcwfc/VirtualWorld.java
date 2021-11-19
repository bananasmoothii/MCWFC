package fr.bananasmoothii.mcwfc;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A "visrtual" is a three-dimensional array list that allows negative indexes. There is no "append" because there is
 * no actual order. The Iterable implementation can be compared to three loops looping over X, Y and Z.
 * @param <T> the type of objects you will put inside
 */
public class VirtualWorld<T> implements Iterable<VirtualWorld.ObjectWithCoordinates<T>> {

    private static int enlargeAtOnce = 5;

    private T[][][] data;

    /*
        The internal working is: when you set or get something at certains coordinates, the coordinates you give are
        offset by x, y and zOffset. x, y and zMin/Max are to determine when the arrays needs to be copied in larger arrays.
        It does not mean the array will be copied in a larger one, it just means it will call enlarge... methods. Then,
        the enlarge... methods will maybe copy the array(s) in larger ones, or do nothing apart saying "hey, you don't
        need to call me for this index", and this means raising ...Max fields or lowering ...Min fields.
     */
    private int xOffset, yOffset, zOffset,
            xMin, yMin, zMin, // inclusive
            xMax, yMax, zMax, // inclusive too
    // these are always equal to Objects.length, Objects[0].length and Objects[0][0].length
    xArraySize, yArraySize, zArraySize;

    public VirtualWorld() {
        this(32, 32, 32);
    }

    
    /**
     * New instance with a given x, y and z starting size. It will grow after if needed. the sizes are from one edge to
     * the opposite.
     */
    public VirtualWorld(int xSize, int ySize, int zSize) {
        xArraySize = xSize; yArraySize = ySize; zArraySize = zSize;
        //noinspection unchecked
        data = (T[][][]) new Object[xArraySize][yArraySize][zArraySize];
        xOffset = xSize / 2; yOffset = ySize / 2; zOffset = zSize / 2;
    }

    public @Nullable T get(int x, int y, int z) {
        try {
            return data[x + xOffset][y + yOffset][z + zOffset];
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public void set(@Nullable T object, int x, int y, int z) {
        ensureCapacityForElement(x, y, z);
        data[x + xOffset][y + yOffset][z + zOffset] = object;
    }

    public void set(@NotNull ObjectWithCoordinates<T> object) {
        set(object.object, object.x, object.y, object.z);
    }

    /**
     * Ensures that an element can be set at the given coordinates
     */
    public void ensureCapacityForElement(int x, int y, int z) {
        if (x < xMin) {
            enlargeX(x - xMin); // = -(xMin - x)
        } else if (x > xMax) {
            enlargeX(x - xMax);
        }
        if (y < yMin) {
            enlargeY(y - yMin); // = -(yMin - y)
        } else if (y > yMax) {
            enlargeY(y - yMax);
        }
        if (z < zMin) {
            enlargeZ(z - zMin); // = -(zMin - z)
        } else if (z > zMax) {
            enlargeZ(z - zMax);
        }
    }

    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    void enlargeX(final int addingSpace) {
        final int totalAddingSpace = Math.abs(addingSpace) + enlargeAtOnce;
        if (addingSpace < 0) {
            if (xMin + addingSpace + xOffset < 0) {
                Object[][][] old = data;
                data = (T[][][]) new Object[xArraySize + totalAddingSpace][yArraySize][zArraySize];
                System.arraycopy(old, 0, data, totalAddingSpace, xArraySize);
                xArraySize += totalAddingSpace;
                xOffset += totalAddingSpace;
            }
            xMin += addingSpace; // so xMin -= abs(addingSpace)
        } else if (addingSpace > 0) {
            if (xMax + addingSpace + xOffset >= xArraySize) {
                Object[][][] old = data;
                data = (T[][][]) new Object[xArraySize + totalAddingSpace][yArraySize][zArraySize];
                System.arraycopy(old, 0, data, 0, xArraySize);
                xArraySize += totalAddingSpace;
            }
            xMax += addingSpace;
        }
    }

    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    void enlargeY(final int addingSpace) {
        final int totalAddingSpace = Math.abs(addingSpace) + enlargeAtOnce;
        if (addingSpace < 0) {
            if (yMin + addingSpace + yOffset < 0) {
                for (int x = 0; x < xArraySize; x++) {
                    Object[][] old = data[x];
                    data[x] = (T[][]) new Object[yArraySize + totalAddingSpace][zArraySize];
                    System.arraycopy(old, 0, data[x], totalAddingSpace, yArraySize);
                }
                yArraySize += totalAddingSpace;
                yOffset += totalAddingSpace;
            }
            yMin += addingSpace;
        } else if (addingSpace > 0) {
            if (yMax + addingSpace + yOffset >= yArraySize) {
                for (int x = 0; x < xArraySize; x++) {
                    Object[][] old = data[x];
                    data[x] = (T[][]) new Object[yArraySize + totalAddingSpace][zArraySize];
                    System.arraycopy(old, 0, data[x], 0, yArraySize);
                }
                yArraySize += totalAddingSpace;
            }
            yMax += addingSpace;
        }
    }

    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    void enlargeZ(final int addingSpace) {
        final int totalAddingSpace = Math.abs(addingSpace) + enlargeAtOnce;
        if (addingSpace < 0) {
            if (zMin + addingSpace + zOffset < 0) {
                for (int x = 0; x < xArraySize; x++) {
                    for (int y = 0; y < yArraySize; y++) {
                        Object[] old = data[x][y];
                        data[x][y] = (T[]) new Object[zArraySize + totalAddingSpace];
                        System.arraycopy(old, 0, data[x][y], totalAddingSpace, zArraySize);
                    }
                }
                zArraySize += totalAddingSpace;
                zOffset += totalAddingSpace;
            }
            zMin -= addingSpace;
        } else if (addingSpace > 0) {
            if (zMax + addingSpace + zOffset >= zArraySize) {
                for (int x = 0; x < xArraySize; x++) {
                    for (int y = 0; y < yArraySize; y++) {
                        Object[] old = data[x][y];
                        data[x][y] = (T[]) new Object[zArraySize + totalAddingSpace];
                        System.arraycopy(old, 0, data[x][y], 0, zArraySize);
                    }
                }
                zArraySize += totalAddingSpace;
            }
            zMax += addingSpace;
        }
    }

    /**
     * when the arrays need to be enlarged, it will directly grow for example 10, so it hasn't to copy arrays each time.
     */
    public static void setEnlargeAtOnce(int enlargeAtOnce) {
        if (enlargeAtOnce < 1) throw new IllegalArgumentException("enlargeAtOnce must be >= 1");
        VirtualWorld.enlargeAtOnce = enlargeAtOnce;
    }

    /**
     * @see #setEnlargeAtOnce(int)
     */
    public static int getEnlargeAtOnce() {
        return enlargeAtOnce;
    }

    /**
     * Iterates over all elements but wrapped int {@link ObjectWithCoordinates} so you can have the coordinates
     * along.
     */
    @NotNull
    @Override
    public Iterator<ObjectWithCoordinates<T>> iterator() {
        return new Iterator<>() {
            private int currentX = xMin, currentY = yMin, currentZ = zMin;

            @Override
            @Contract(pure = true)
            public boolean hasNext() {
                return currentZ < zMax || currentY < yMax || currentX < xMax;
            }

            @Override
            public ObjectWithCoordinates<T> next() {
                if (++currentZ >= zMax) {
                    currentZ = zMin;
                    currentY++;
                }
                if (currentY >= yMax) {
                    currentY = yMin;
                    currentX++;
                }
                if (currentX >= xMax) throw new NoSuchElementException();
                T c = get(currentX, currentY, currentZ);
                return new ObjectWithCoordinates<>(c, currentX, currentY, currentZ);
            }
        };
    }

    /**
     * An object of type T with x, y and z coordinates
     * @param <T> the type of the object
     */
    public static record ObjectWithCoordinates<T>(T object, int x, int y, int z) {}

    /**
     * prints the layer zLayer
     */
    public void debugPrint(int zLayer) {
        System.out.println("xMin = " + xMin + " ;  xMax = " + xMax + " ;  yMin = " + yMin + " ;  yMax = " + yMax);
        for (int x = xMin + xOffset; x <= xMax + xOffset; x++) {
            for (int y = yMin + yOffset ; y <= yMax + yOffset; y++) {
                T element = data[x][y][zLayer + zOffset];
                System.out.print(element != null ? element : ' ');
                System.out.print(' ');
            }
            System.out.print('\n');
        }
    }

}
