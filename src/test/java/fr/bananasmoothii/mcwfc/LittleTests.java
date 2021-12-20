package fr.bananasmoothii.mcwfc;

import fr.bananasmoothii.mcwfc.util.Face;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static fr.bananasmoothii.mcwfc.util.RotationAngle.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LittleTests {

    @Test
    void virtualSpaceSet() {
        VirtualSpace<String> space = new VirtualSpace<>();
        space.set("a", 1, 0, 0);
        space.set("b", -3, 0, 0);
        space.set("c", 18, -20, 0);
        assertEquals("c", space.get(18, -20, 0));
        for (int x = -20; x <= 20; x++) {
            for (int y = -20; y <= 20; y++) {
                for (int z = -20; z <= 20; z++) {
                    space.set(String.valueOf(x + y + z), x, y, z);
                }
            }
        }
    }

    @Test
    void pieceRotation() {
        Piece piece1 = new Piece(2, 3, 1, BlockDataImpl.AIR);
        piece1.set(BlockDataImpl.STONE, 1, 2, 0);
        piece1.set(BlockDataImpl.STONE, 0, 1, 0);
        Piece piece2 = piece1.rotateZ(D270);
        assertEquals(BlockDataImpl.STONE, piece2.get(2, 0, 0));
        assertEquals(piece1, piece2.rotateZ(D90));
        assertEquals(piece2.rotateZ(D270), piece1.rotateZ(D180));

        Piece piece3 = new Piece(2, 3, 4, BlockDataImpl.AIR);
        piece3.set(BlockDataImpl.STONE, 0, 0, 0);
        assertEquals(
                piece3,
                piece3  .rotateX(D270)
                        .rotateY(D90)
                        .rotateZ(D180)
                        .rotateX(D90)
                        .rotateY(D180)
                        .rotateZ(D90));
    }

    @Test
    void pieceFlip() {
        Piece piece = new Piece(2, 3, 4, BlockDataImpl.AIR);
        piece.set(BlockDataImpl.STONE, 0, 0, 0);
        assertEquals(piece,
                piece.flipX().flipY().flipZ().flipX().flipY().flipZ());
    }

    @Test
    void pieceSiblings() {
        Piece piece;
        Set<@NotNull Piece> pieces;
        {
            piece = new Piece(2, 3, 4, BlockDataImpl.AIR);
            piece.set(BlockDataImpl.STONE, 0, 0, 0);
            piece.set(BlockDataImpl.STONE, 0, 1, 3);
            piece.debugPrint();
            pieces = piece.generateSiblings(true);
            assertEquals(48, pieces.size());
        }
        {
            piece = new Piece(3, 3, 3, BlockDataImpl.AIR);
            pieces = piece.generateSiblings(true);
            assertEquals(1, pieces.size());
        }
    }

    @Test
    void faceRotationFlip() {
        for (Face face : Face.values()) {
            System.out.println(face.name() + ": " + face.getModX() + ' ' + face.getModY() + ' ' + face.getModZ());
        }
        Face face1 = Face.NORTH_WEST;
        Face face2 = Face.DOWN;
        assertEquals(face1, face1
                .rotateX(D270)
                .rotateY(D90)
                .rotateZ(D180)
                .rotateX(D90)
                .rotateY(D180)
                .rotateZ(D90));
        assertEquals(face2, face2
                .rotateX(D270)
                .rotateY(D90)
                .rotateZ(D180)
                .rotateX(D90)
                .rotateY(D180)
                .rotateZ(D90));
        assertEquals(face1, face1
                .flipX().flipY().flipZ().flipX().flipY().flipZ());
        assertEquals(face2, face2
                .flipX().flipY().flipZ().flipX().flipY().flipZ());
    }
}