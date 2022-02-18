package fr.bananasmoothii.mcwfc;

import fr.bananasmoothii.mcwfc.util.Bounds;
import fr.bananasmoothii.mcwfc.util.Face;
import fr.bananasmoothii.mcwfc.util.WeightedSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Set;

import static fr.bananasmoothii.mcwfc.BlockDataImpl.AIR;
import static fr.bananasmoothii.mcwfc.BlockDataImpl.STONE;
import static fr.bananasmoothii.mcwfc.util.RotationAngle.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LittleTests {

    @Test
    @Order(1)
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
    @Order(2)
    void pieceRotation() {
        Piece piece1 = new Piece(2, 3, 1, AIR);
        piece1.set(STONE, 1, 2, 0);
        piece1.set(STONE, 0, 1, 0);
        Piece piece2 = piece1.rotateZ(D270);
        assertEquals(STONE, piece2.get(2, 0, 0));
        assertEquals(piece1, piece2.rotateZ(D90));
        assertEquals(piece2.rotateZ(D270), piece1.rotateZ(D180));

        Piece piece3 = new Piece(2, 3, 4, AIR);
        piece3.set(STONE, 0, 0, 0);
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
    @Order(3)
    void pieceFlip() {
        Piece piece = new Piece(2, 3, 4, AIR);
        piece.set(STONE, 0, 0, 0);
        assertEquals(piece,
                piece.flipX().flipY().flipZ().flipX().flipY().flipZ());
    }

    @Test
    @Order(4)
    void pieceSiblings() {
        Piece piece;
        Set<@NotNull Piece> pieces;
        { // IDK why I did that way
            piece = new Piece(2, 3, 4, AIR);
            piece.set(STONE, 0, 0, 0);
            piece.set(STONE, 0, 1, 3);
            //piece.debugPrint();
            pieces = piece.generateSiblings(true);
            assertEquals(48, pieces.size());
        }
        {
            piece = new Piece(3, 3, 3, AIR);
            pieces = piece.generateSiblings(true);
            assertEquals(1, pieces.size());
        }
    }

    @Test
    @Order(5)
    void faceRotationFlip() {
        Face face1 = Face.NORTH_WEST;
        Face face2 = Face.BOTTOM;
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

    @Test
    @Order(6)
    void pieceNeighborsSiblings() {
        PieceNeighbors piece;
        Set<@NotNull PieceNeighbors> pieces;
        PieceNeighbors piece0 = new PieceNeighbors(new Piece(2, 3, 4, AIR));
        piece0.getCenterPiece().set(STONE, 0, 0, 0);
        piece0.getCenterPiece().set(STONE, 0, 1, 3);
        piece0.addNeighbor(Face.TOP, piece0.getCenterPiece());
        piece0.addNeighbor(Face.SOUTH_EAST_TOP, piece0.getCenterPiece());

        piece = piece0.copy();
        pieces = piece.generateSiblings(true);
        assertEquals(48, pieces.size());
        assertEquals(8, piece.generateSiblings(false).size());
    }

    @Test
    @Order(7)
    void coordsAreInBounds() {
        MCVirtualSpace space = new MCVirtualSpace(AIR);
        space.ensureCapacityForElement(-7, -7, -7);
        space.ensureCapacityForElement(7, 7, 7);
        for (int x = -15; x <= 15; x++) {
            int xInBounds = space.xInBounds(x);
            assertTrue(space.xMin() <= xInBounds && xInBounds <= space.xMax());
        }
        space = new MCVirtualSpace(AIR);
        space.set(STONE, -4, 3, 8);
        assertEquals(STONE, space.getModuloCoords(1, -1, 26)); // 26 % 9 = 8
    }

    @Test
    @Order(8)
    void generatePieces() {
        MCVirtualSpace space = new MCVirtualSpace(AIR);
        space.setFill(AIR);
        // define the real sample size
        space.ensureCapacityForElement(-1, -1, 0); // min point
        space.ensureCapacityForElement(1, 2, 5);   // max point
        space.set(STONE, 0, 0, 0);
        space.set(STONE, 0, 0, 1);
        space.set(STONE, 0, 0, 2);
        space.set(STONE, 0, 0, 3); // that sample is too small but it doesn't matter
        space.set(STONE, 0, 1, 3);
        space.set(STONE, 0, -1, 3);
        space.set(STONE, 0, 0, 4);
        space.set(STONE, 0, 0, 5);
        pieceSet = space.generatePieces(2, true);
        System.out.println("Generated a piece set with " + pieceSet.size() + " elements");
    }

    private WeightedSet<PieceNeighbors> pieceSet;

    @Test
    @Order(9)
    void generateWorld() {
        if (pieceSet == null) generatePieces();
        GeneratingWorld world = new GeneratingWorld(pieceSet);
        world.generateWFC(new Bounds(-15, -15, -15, 15, 15, 15));
    }
}