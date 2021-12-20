package fr.bananasmoothii.mcwfc.util;

import fr.bananasmoothii.mcwfc.Piece;
import fr.bananasmoothii.mcwfc.VirtualSpace;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link VirtualSpace} with minecraft blocks ({@link BlockData}. It provides some useful methods, mainly to generate
 * {@link Piece}s
 */
public class MCVirtualSpace extends VirtualSpace<BlockData> {

    public List<Piece> generatePieces(int pieceSize) {
        ArrayList result = new ArrayList<>();
        // TODO
        return null;
    }

    /**
     * @param fill the block to set when encountering a null value
     * @return the piece of size pieceSize from x, y, z to x+pieceSize, y+pieceSize, z+pieceSize
     */
    public @NotNull Piece getPieceAt(int x, int y, int z, int pieceSize, @NotNull BlockData fill) {
        Objects.requireNonNull(fill);
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
