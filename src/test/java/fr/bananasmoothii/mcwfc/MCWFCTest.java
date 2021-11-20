package fr.bananasmoothii.mcwfc;

import org.junit.jupiter.api.Test;

import static fr.bananasmoothii.mcwfc.Piece.RotationAngle.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MCWFCTest {

    @Test
    void set() {
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
    void rotation() {
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
    void flip() {
        Piece piece = new Piece(2, 3, 4, BlockDataImpl.AIR);
        piece.set(BlockDataImpl.STONE, 0, 0, 0);
        assertEquals(piece,
                piece.flipX().flipY().flipZ().flipX().flipY().flipZ());
    }
}