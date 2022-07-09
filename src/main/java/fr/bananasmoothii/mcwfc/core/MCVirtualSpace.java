package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Coords;
import fr.bananasmoothii.mcwfc.core.util.Face;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link VirtualSpace} with minecraft blocks ({@link B}). It provides some useful methods, mainly to generate
 * {@link Piece}s. This implementation does not allow {@code null} fills (see {@link VirtualSpace#setFill(Object)}).
 * @see #generatePieces(int, boolean, boolean)
 * @param <B> the type of blocks in this piece. In vanilla minecraft, this can be {@code BlockData}.
 */
@SuppressWarnings("NullableProblems")
public class MCVirtualSpace<B> extends VirtualSpace<@NotNull B> {

    public MCVirtualSpace(@NotNull B fill) {
        super();
        setFill(fill);
    }

    public MCVirtualSpace(@NotNull VirtualSpace<@NotNull B> propertiesIndicator) {
        super(propertiesIndicator);
        if (propertiesIndicator.getFill() == null)
            throw new NullPointerException("The propertiesIndicator cannot have a null fill value");
    }

    public MCVirtualSpace(int xSize, int ySize, int zSize, @NotNull B fill) {
        super(xSize, ySize, zSize);
        setFill(fill);
    }

    public MCVirtualSpace(@NotNull Bounds bounds, @NotNull B fill) {
        super(bounds);
        setFill(fill);
    }

    /**
     * Generates and reduces pieces along with their neighbors. This doesn't generate any edge or corner neighbors, only
     * cartesian faces as in {@link Face#isCartesian()}. This method doesn't allow putting pieces upside down.
     * @param pieceSize the size of each {@link Piece} in x, y and z directions
     * @return a {@link Sample}, the weight represents the number of times a piece was seen.
     * @throws NullPointerException if there is no {@link #setFill(Object) fill}.
     */
    public Sample<B> generatePieces(final int pieceSize) {
        return generatePieces(pieceSize, false, true);
    }

    /**
     * Generates and reduces pieces along with their neighbors. This doesn't generate any edge or corner neighbors, only
     * cartesian faces as in {@link Face#isCartesian()}.
     * @param pieceSize the size of each {@link Piece} in x, y and z directions
     * @param allowUpsideDown whether to allow putting pieces sideways or upside down. If this is set to false, it will
     *                        only allow x and z flipping, and rotating along the y-axis.
     * @param useModuloCoordsTopAndBottom when the generator is at the top of this {@link MCVirtualSpace}, should it consider that
     *                             adding 1 to the y-axis makes it go to the bottom of this {@link MCVirtualSpace} and
     *                             vice-versa ({@code true}), or not ({code false}) ? If {@code false}, it will be almost
     *                             impossible to generate something higher in the {@link Wave}
     * @return a {@link Sample}, the weights in the sample represents the number of times a piece was seen.
     * @throws NullPointerException if there is no {@link #setFill(Object) fill}.
     */
    public Sample<B> generatePieces(final int pieceSize, final boolean allowUpsideDown,
                                 final boolean useModuloCoordsTopAndBottom) {
        Sample<B> result = new Sample<>();
        HashMap<Coords, Optional<Piece.Locked<B>>> piecesCache = new HashMap<>(); // used to keep the same reference for pieces with the exact same coords
        for (int x = xMin(); x <= xMax(); x++) {
            for (int y = yMin(); y <= yMax(); y++) {
                for (int z = zMin(); z <= zMax(); z++) {
                    PieceNeighbors<B> pieceNeighbors = new PieceNeighbors<>(getPieceAt(new Coords(x, y, z), pieceSize, true, piecesCache).orElseThrow());
                    pieceNeighbors.put(Face.TOP, getPieceAt(new Coords(x, y + pieceSize, z), pieceSize, useModuloCoordsTopAndBottom, piecesCache));
                    pieceNeighbors.put(Face.BOTTOM, getPieceAt(new Coords(x, y - pieceSize, z), pieceSize, useModuloCoordsTopAndBottom, piecesCache));
                    pieceNeighbors.put(Face.WEST, getPieceAt(new Coords(x - pieceSize, y, z), pieceSize, true, piecesCache));
                    pieceNeighbors.put(Face.EAST, getPieceAt(new Coords(x + pieceSize, y, z), pieceSize, true, piecesCache));
                    pieceNeighbors.put(Face.NORTH, getPieceAt(new Coords(x, y, z - pieceSize), pieceSize, true, piecesCache));
                    pieceNeighbors.put(Face.SOUTH, getPieceAt(new Coords(x, y, z + pieceSize), pieceSize, true, piecesCache));
                    // add 1 to the weight if that sibling already exists, else put it in the map with a weight of 1
                    result.addAll(pieceNeighbors.lock().generateSiblingsLock(allowUpsideDown));
                }
            }
        }
        return result;
    }

    /**
     * You may use this method only if a fill was set with {@link #setFill(Object)}.
     * @param useModuloCoords if some coordinates are out of bounds, it will take them back in the bounds. This means
     *                        that if you have a list of 3 elements, it will reformat your coordinates modulo (%) 3.
     *                        For exemple, if you call element of index 5 in that list, you will get the element of index
     *                        2 because {@code 5%3=2}. In a {@link VirtualSpace}, this means that if you get too far away
     *                        and cross one side, you will pop up at the opposite site. See
     *                        {@link VirtualSpace#xInBounds(int)}
     * @return the piece of size pieceSize from x, y, z to x+pieceSize, y+pieceSize, z+pieceSize. It may only return
     * {@code null} if "useModuloCoords" is true
     * @throws NullPointerException if there is no {@link #setFill(Object) fill}.
     */
    public @Nullable Piece.Locked<B> getPieceAt(final int x, final int y, final int z, final int pieceSize, final boolean useModuloCoords) {
        B fill = getFill();

        if (useModuloCoords) {
            Piece<B> piece = new Piece<>(pieceSize, fill);
            for (int ix = x; ix < x + pieceSize; ix++) {
                for (int iy = y; iy < y + pieceSize; iy++) {
                    for (int iz = z; iz < z + pieceSize; iz++) {
                        piece.set(getOrDefaultModuloCoords(ix, iy, iz, fill), ix - x, iy - y, iz - z);
                    }
                }
            }
            return piece.lock();
        } else {
            if (x < xMin() || y < yMin() || z < zMin() || x + pieceSize > xMax() ||
                    y + pieceSize > yMax() || z + pieceSize > zMax())
                return null;
                //throw new IllegalArgumentException("invalid coordinates for piece: " + x + ' ' + y + ' ' + z +
                //        " in VirtualSpace of size " + getPrettyCoordinates());

            Piece<B> piece = new Piece<>(pieceSize, fill);
            for (int ix = x; ix < x + pieceSize; ix++) {
                for (int iy = y; iy < y + pieceSize; iy++) {
                    for (int iz = z; iz < z + pieceSize; iz++) {
                        piece.set(getOrDefault(ix, iy, iz, fill), ix - x, iy - y, iz - z);
                    }
                }
            }
            return piece.lock();
        }
    }

    /**
     * Useful to keep same references for same pieces.
     * @see #getPieceAt(int, int, int, int, boolean)
     */
    protected Optional<Piece.Locked<B>> getPieceAt(final @NotNull Coords coords, final int pieceSize, final boolean useModuloCoords,
                                                             @NotNull Map<Coords, Optional<Piece.Locked<B>>> pieceCache) {
        return pieceCache.computeIfAbsent(coords,
                coords1 -> Optional.ofNullable(getPieceAt(coords1.x(), coords1.y(), coords1.z(), pieceSize, useModuloCoords)));
    }

    @Override
    public MCVirtualSpace<B> select(int xFrom, int yFrom, int zFrom, int xTo, int yTo, int zTo) {
        if (xFrom > xTo) {
            int swapper = xFrom;
            xFrom = xTo;
            xTo = swapper;
        }
        if (yFrom > yTo) {
            int swapper = yFrom;
            yFrom = yTo;
            yTo = swapper;
        }
        if (zFrom > zTo) {
            int swapper = zFrom;
            zFrom = zTo;
            zTo = swapper;
        }
        MCVirtualSpace<B> result = new MCVirtualSpace<>(getFill());
        result.setFill(getFill());
        for (int xi = xFrom; xi <= xTo; xi++) {
            for (int yi = yFrom; yi <= yTo; yi++) {
                for (int zi = zFrom; zi <= zTo; zi++) {
                    final B element = getWithoutFill(xi, yi, zi);
                    if (element != null) {
                        result.set(element, xi, yi, zi);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void setFill(@NotNull B fill) {
        super.setFill(Objects.requireNonNull(fill));
    }

    @Override
    public @NotNull B getFill() {
        return Objects.requireNonNull(super.getFill());
    }
}
