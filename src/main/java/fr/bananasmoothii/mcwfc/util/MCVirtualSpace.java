package fr.bananasmoothii.mcwfc.util;

import fr.bananasmoothii.mcwfc.Piece;
import fr.bananasmoothii.mcwfc.PieceNeighbors;
import fr.bananasmoothii.mcwfc.VirtualSpace;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A {@link VirtualSpace} with minecraft blocks ({@link BlockData}. It provides some useful methods, mainly to generate
 * {@link Piece}s
 */
public class MCVirtualSpace extends VirtualSpace<BlockData> {

    /**
     * Generates and reduces pieces along with their neighbors. This doesn't generate any borders (only cartesian faces
     * as in {@link Face#isCartesian()})
     * @param pieceSize the size of each {@link Piece} in x, y and z directions
     */
    public List<PieceNeighbors> generatePieces(final int pieceSize) {
        if (pieceSize < 1) throw new IllegalArgumentException("Piece size can't be below 1");
        for (int x = xMin(); x <= xMax(); x++) {
            for (int y = yMax(); y < yMax(); y++) {
                for (int z = zMax(); z < zMax(); z++) {

                }
            }
        }
    }

    /**
     * @param fill the block to set when encountering a null value
     * @param useModuloCoords if some coordinates are out of bounds, it will take them back in the bounds. This means
     *                        that if you have a list of 3 elements, it will reformat your coordinates modulo (%) 3.
     *                        For exemple, if you call element of index 5 in that list, you will get the element of index
     *                        2 because {@code 5%3=2}. In a {@link VirtualSpace}, this means that if you get too far away
     *                        and cross one side, you will pop up at the opposite site. See
     *                        {@link VirtualSpace#xInBounds(int)}
     * @return the piece of size pieceSize from x, y, z to x+pieceSize, y+pieceSize, z+pieceSize
     */
    public @NotNull Piece getPieceAt(int x, int y, int z, int pieceSize, @NotNull BlockData fill, boolean useModuloCoords) {
        Objects.requireNonNull(fill);

        if (useModuloCoords) {
            Piece piece = new Piece(pieceSize, fill);
            for (int ix = x; ix < x + pieceSize; ix++) {
                for (int iy = y; iy < y + pieceSize; iy++) {
                    for (int iz = z; iz < z + pieceSize; iz++) {
                        piece.set(getOrDefault(xInBounds(ix), yInBounds(iy), zInBounds(iz), fill),
                                xInBounds(ix - x), yInBounds(iy - y), zInBounds(iz - z));
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
}
