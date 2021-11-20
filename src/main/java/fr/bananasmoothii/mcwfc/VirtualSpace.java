package fr.bananasmoothii.mcwfc;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A "visrtual" is a three-dimensional array list that allows negative indexes. There is no "append" because there is
 * no actual order. The Iterable implementation can be compared to three loops looping over X, Y and Z.
 * @param <T> the type of objects you will put inside
 */
public class VirtualSpace<T> implements Iterable<VirtualSpace.ObjectWithCoordinates<T>> {

    private int enlargeAtOnce = 5;

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

    private @Nullable T fill;

    public VirtualSpace() {
        this(10, 10, 10);
    }

    /**
     * Constructs a VirtualSpace with the same size as the one passed in parameter, with the same <i>enlargeAtOnce</i>
     * and with the same <i>fill</i> value (see {@link #setFill(Object)})
     */
    public VirtualSpace(@NotNull VirtualSpace<T> propertiesIndicator) {
        xArraySize = propertiesIndicator.xArraySize;
        yArraySize = propertiesIndicator.yArraySize;
        zArraySize = propertiesIndicator.zArraySize;

        //noinspection unchecked
        data = (T[][][]) new Object[xArraySize][yArraySize][zArraySize];

        xOffset = propertiesIndicator.xOffset;
        yOffset = propertiesIndicator.yOffset;
        zOffset = propertiesIndicator.zOffset;

        xMin = propertiesIndicator.xMin;
        yMin = propertiesIndicator.yMin;
        zMin = propertiesIndicator.zMin;

        xMax = propertiesIndicator.xMax;
        yMax = propertiesIndicator.yMax;
        zMax = propertiesIndicator.zMax;

        enlargeAtOnce = propertiesIndicator.enlargeAtOnce;

        fill = propertiesIndicator.fill;
    }

    /**
     * New instance with a given x, y and z starting size. It will grow after if needed. the sizes are from one edge to
     * the opposite.
     */
    public VirtualSpace(int xSize, int ySize, int zSize) {
        xArraySize = xSize;
        yArraySize = ySize;
        zArraySize = zSize;
        //noinspection unchecked
        data = (T[][][]) new Object[xArraySize][yArraySize][zArraySize];
        xOffset = xSize / 2; yOffset = ySize / 2; zOffset = zSize / 2;
    }

    /**
     * Replaces all null values by this. Note that this is more visual: the arrays won't change, but instead of returning
     * null, it will now return what you give in here. This means that if you set {@code null} at a certain coordinate
     * and get the element at the same coordinates, you won't get {@code null}, but {@code fill} (the argument you
     * provide here).
     */
    public void setFill(@Nullable T fill) {
        this.fill = fill;
    }

    public @Nullable T getFill() {
        return fill;
    }

    public @Nullable T get(int x, int y, int z) {
        return replaceNullByFill(getWithoutFill(x, y, z));
    }

    /**
     * @return {@code null} if there is no element at these coordinates, even if there is a <i>fill</i> set.
     * @see #setFill(Object)
     */
    public @Nullable T getWithoutFill(int x, int y, int z) {
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
            zMin += addingSpace;
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

    public T replaceNullByFill(@Nullable T element) {
        return element == null ? fill : element;
    }

    /**
     * when the arrays need to be enlarged, it will directly grow for example 5, so it hasn't to copy arrays each time.
     */
    public void setEnlargeAtOnce(int enlargeAtOnce) {
        if (enlargeAtOnce < 1) throw new IllegalArgumentException("enlargeAtOnce must be >= 1");
        this.enlargeAtOnce = enlargeAtOnce;
    }

    /**
     * @see #setEnlargeAtOnce(int)
     */
    public int getEnlargeAtOnce() {
        return enlargeAtOnce;
    }

    /**
     * This iterator won't return the <i>fill</i> value if the element at a certain position is {@code null}.
     * @see #setFill(Object)
     */
    public Iterator<ObjectWithCoordinates<T>> iteratorWithoutFill() {
        return new Iterator<>() {
            private int currentX = xMin, currentY = yMin, currentZ = zMin;

            @Override
            @Contract(pure = true)
            public boolean hasNext() {
                return currentZ <= zMax && currentY <= yMax && currentX <= xMax;
            }

            @Override
            public ObjectWithCoordinates<T> next() {
                ObjectWithCoordinates<T> element = new ObjectWithCoordinates<>(getWithoutFill(currentX, currentY, currentZ),
                                currentX, currentY, currentZ);
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
                return currentZ <= zMax && currentY <= yMax && currentX <= xMax;
            }

            @Override
            public ObjectWithCoordinates<T> next() {
                ObjectWithCoordinates<T> element = new ObjectWithCoordinates<>(get(currentX, currentY, currentZ),
                        currentX, currentY, currentZ);
                if (++currentZ > zMax) {
                    currentZ = zMin;
                    currentY++;
                    if (currentY > yMax) {
                        currentY = yMin;
                        currentX++;
                        if (currentX > xMax + 1 && currentY > yMax && currentZ > zMax)
                            throw new NoSuchElementException("indexes above maximum");
                    }
                }
                return element;
            }
        };
    }

    /**
     * An object of type T with x, y and z coordinates
     * @param <T> the type of the object
     */
    public static record ObjectWithCoordinates<T>(T object, int x, int y, int z) {}

    public VirtualSpace<T> copy() {
        VirtualSpace<T> copy = new VirtualSpace<>(this);
        for (int x = 0; x < xArraySize; x++) {
            for (int y = 0; y < yArraySize; y++) {
                System.arraycopy(data[x][y], 0, copy.data[x][y], 0, zArraySize);
            }
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VirtualSpace other) {
            if (!Objects.equals(fill, other.fill)) return false;
            Iterator<ObjectWithCoordinates<T>> thisIterator = iteratorWithoutFill();
            @SuppressWarnings("unchecked")
            Iterator<ObjectWithCoordinates<?>> objIterator = other.iteratorWithoutFill();
            while (thisIterator.hasNext() && objIterator.hasNext()) {
                if (!thisIterator.next().equals(objIterator.next())) return false;
            }
            return true;
        }
        return false;
    }

    /**
     * prints the layer zLayer
     */
    public void debugPrint(int zLayer) {
        System.out.println("z = " + zLayer + " ; xMin = " + xMin + " ;  xMax = " + xMax + " ;  yMin = " + yMin + " ;  yMax = " + yMax);
        for (int x = xMin + xOffset; x <= xMax + xOffset; x++) {
            for (int y = yMin + yOffset ; y <= yMax + yOffset; y++) {
                T element = data[x][y][zLayer + zOffset];
                System.out.print(element != null ? element : fill != null ? fill : ' ');
                System.out.print(' ');
            }
            System.out.print('\n');
        }
    }

    public void debugPrintAllLayers() {
        System.out.println('\n');
        for (int z = zMin; z <= zMax; z++) {
            debugPrint(z);
        }
        System.out.println('\n');
    }

}
