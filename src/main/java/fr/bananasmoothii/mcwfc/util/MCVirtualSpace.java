package fr.bananasmoothii.mcwfc.util;

import fr.bananasmoothii.mcwfc.Piece;
import fr.bananasmoothii.mcwfc.PieceNeighbors;
import fr.bananasmoothii.mcwfc.VirtualSpace;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link VirtualSpace} with minecraft blocks ({@link BlockData}. It provides some useful methods, mainly to generate
 * {@link Piece}s. This implementation does not allow {@code null} fills (see {@link VirtualSpace#setFill(Object)}.
 * @see #generatePieces(int, boolean)
 */
@SuppressWarnings("NullableProblems")
public class MCVirtualSpace extends VirtualSpace<@NotNull BlockData> {

    public MCVirtualSpace(@NotNull BlockData fill) {
        super();
        setFill(fill);
    }

    public MCVirtualSpace(@NotNull VirtualSpace<@NotNull BlockData> propertiesIndicator) {
        super(propertiesIndicator);
        if (propertiesIndicator.getFill() == null)
            throw new NullPointerException("The propertiesIndicator cannot have a null fill value");
    }

    public MCVirtualSpace(int xSize, int ySize, int zSize, @NotNull BlockData fill) {
        super(xSize, ySize, zSize);
        setFill(fill);
    }

    /**
     * Generates and reduces pieces along with their neighbors. This doesn't generate any edge or corner neighbors, only
     * cartesian faces as in {@link Face#isCartesian()}. This method doesn't allow putting pieces upside down.
     * You may use this method only if a fill was set with {@link #setFill(BlockData)}.
     * @param pieceSize the size of each {@link Piece} in x, y and z directions
     * @return a map with the {@link PieceNeighbors} as key and an integer representing the weight (or the number or time
     * this {@link PieceNeighbors} was seen)
     * @throws NullPointerException if there is no {@link #setFill(BlockData) fill}.
     */
    public Map<PieceNeighbors, Integer> generatePieces(final int pieceSize) {
        return generatePieces(pieceSize, false);
    }

    /**
     * Generates and reduces pieces along with their neighbors. This doesn't generate any edge or corner neighbors, only
     * cartesian faces as in {@link Face#isCartesian()}.
     * You may use this method only if a fill was set with {@link #setFill(BlockData)}.
     * @param pieceSize the size of each {@link Piece} in x, y and z directions
     * @param allowUpsideDown whether to allow putting pieces sideways or upside down. If this is set to false, it will
     *                        only allow x and z flipping, and rotating along the y-axis.
     * @return a map with the {@link PieceNeighbors} as key and an integer representing the weight (or the number or time
     * this {@link PieceNeighbors} was seen)
     * @throws NullPointerException if there is no {@link #setFill(BlockData) fill}.
     */
    public Map<PieceNeighbors, Integer> generatePieces(final int pieceSize, final boolean allowUpsideDown) {
        HashMap<PieceNeighbors, Integer> result = new HashMap<>();
        for (int x = xMin(); x <= xMax(); x++) {
            for (int y = yMin(); y < yMax(); y++) {
                for (int z = zMin(); z < zMax(); z++) {
                    //System.out.println("Generating " + x + ' ' + y + ' ' + z);
                    PieceNeighbors pieceNeighbors = new PieceNeighbors(getPieceAt(x, y, z, pieceSize, true));
                    pieceNeighbors.addNeighbor(Face.TOP, getPieceAt(x, y + pieceSize, z, pieceSize, true));
                    pieceNeighbors.addNeighbor(Face.BOTTOM, getPieceAt(x, y - pieceSize, z, pieceSize, true));
                    pieceNeighbors.addNeighbor(Face.WEST, getPieceAt(x - pieceSize, y, z, pieceSize, true));
                    pieceNeighbors.addNeighbor(Face.EAST, getPieceAt(x + pieceSize, y, z, pieceSize, true));
                    pieceNeighbors.addNeighbor(Face.NORTH, getPieceAt(x, y, z - pieceSize, pieceSize, true));
                    pieceNeighbors.addNeighbor(Face.SOUTH, getPieceAt(x, y, z + pieceSize, pieceSize, true));
                    for (PieceNeighbors sibling : pieceNeighbors.generateSiblings(allowUpsideDown)) {
                        // add 1 to the weight if that sibling already exists, else put it in the map with a weight of 1
                        result.merge(sibling, 1, Integer::sum);
                    }
                }
            }
        }
        return result;
    }


    // da big ting here
    public void generateWFC(final MCVirtualSpace sample, final int pieceSize, final boolean allowUpsideDown) {
        // TODO
    }


    /**
     * You may use this method only if a fill was set with {@link #setFill(BlockData)}.
     * @param useModuloCoords if some coordinates are out of bounds, it will take them back in the bounds. This means
     *                        that if you have a list of 3 elements, it will reformat your coordinates modulo (%) 3.
     *                        For exemple, if you call element of index 5 in that list, you will get the element of index
     *                        2 because {@code 5%3=2}. In a {@link VirtualSpace}, this means that if you get too far away
     *                        and cross one side, you will pop up at the opposite site. See
     *                        {@link VirtualSpace#xInBounds(int)}
     * @return the piece of size pieceSize from x, y, z to x+pieceSize, y+pieceSize, z+pieceSize
     * @throws NullPointerException if there is no {@link #setFill(BlockData) fill}.
     */
    public @NotNull Piece getPieceAt(final int x, final int y, final int z, final int pieceSize, final boolean useModuloCoords) {
        BlockData fill = getFill();

        if (useModuloCoords) {
            Piece piece = new Piece(pieceSize, fill);
            for (int ix = x; ix < x + pieceSize; ix++) {
                for (int iy = y; iy < y + pieceSize; iy++) {
                    for (int iz = z; iz < z + pieceSize; iz++) {
                        piece.set(getOrDefaultModuloCoords(ix, iy, iz, fill), ix - x, iy - y, iz - z);
                    }
                }
            }
            return piece;
        } else {
            if (x < xMin() || y < yMin() || z < zMin() || x + pieceSize > xMax() ||
                    y + pieceSize > yMax() || z + pieceSize > zMax())
                throw new IllegalArgumentException("invalid coordinates for piece: " + x + ' ' + y + ' ' + z +
                        " in VirtualSpace of size " + getPrettyCoordinates());

            Piece piece = new Piece(pieceSize, fill);
            for (int ix = x; ix < x + pieceSize; ix++) {
                for (int iy = y; iy < y + pieceSize; iy++) {
                    for (int iz = z; iz < z + pieceSize; iz++) {
                        piece.set(getOrDefault(ix, iy, iz, fill), ix - x, iy - y, iz - z);
                    }
                }
            }
            return piece;
        }
    }

    @Override
    public MCVirtualSpace select(int xFrom, int yFrom, int zFrom, int xTo, int yTo, int zTo) {
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
        MCVirtualSpace result = new MCVirtualSpace(getFill());
        result.setFill(getFill());
        for (int xi = xFrom; xi <= xTo; xi++) {
            for (int yi = yFrom; yi <= yTo; yi++) {
                for (int zi = zFrom; zi <= zTo; zi++) {
                    final BlockData element = getWithoutFill(xi, yi, zi);
                    if (element != null) {
                        result.set(element, xi, yi, zi);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void setFill(@NotNull BlockData fill) {
        super.setFill(Objects.requireNonNull(fill));
    }

    @Override
    public @NotNull BlockData getFill() {
        return Objects.requireNonNull(super.getFill());
    }
}
