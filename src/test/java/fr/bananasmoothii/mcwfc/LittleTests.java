package fr.bananasmoothii.mcwfc;

import fr.bananasmoothii.mcwfc.core.*;
import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.Sample;
import fr.bananasmoothii.mcwfc.core.util.WeightedSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Set;

import static fr.bananasmoothii.mcwfc.BlockDataImpl.AIR;
import static fr.bananasmoothii.mcwfc.BlockDataImpl.STONE;
import static fr.bananasmoothii.mcwfc.core.util.RotationAngle.*;
import static org.junit.jupiter.api.Assertions.*;

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
        space = new VirtualSpace<>(new Bounds(4000, 100, -4010, 4010, 110, -4000));
        space.set("a", 4005, 105, -4005);
        assertEquals(4000, space.xMin());
        assertEquals(100, space.yMin());
        assertEquals(-4010, space.zMin());
        assertEquals(4010, space.xMax());
        assertEquals(110, space.yMax());
        assertEquals(-4000, space.zMax());
        assertEquals("a", space.get(4005, 105, -4005));
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
        PieceNeighborsPossibilities piece;
        Set<@NotNull PieceNeighborsPossibilities> pieces;
        Piece center = new Piece(2, 3, 4, AIR);
        center.set(STONE, 0, 0, 0);
        center.set(STONE, 0, 1, 3);
        PieceNeighbors neighbors = new PieceNeighbors();
        neighbors.put(Face.TOP, center);
        neighbors.put(Face.SOUTH_EAST_TOP, center);
        PieceNeighborsPossibilities piece0 = new PieceNeighborsPossibilities(center);
        piece0.add(neighbors);

        piece = new PieceNeighborsPossibilities(piece0);
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
        for (int x = -8; x <= 8; x++) {
            int xInBounds = space.xInBounds(x);
            assertTrue(space.xMin() <= xInBounds && xInBounds <= space.xMax());
        }
        space = new MCVirtualSpace(AIR);
        space.set(STONE, -4, 3, 8);
        assertEquals(STONE, space.getModuloCoords(1, -1, 26)); // 26 % 9 = 8

        space = new MCVirtualSpace(new Bounds(-285, 64, 88, -282, 65, 91), AIR);
        assertEquals(64, space.yInBounds(66));
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
        pieceSet = space.generatePieces(2);
        System.out.println("Generated a piece set with " + pieceSet.size() + " elements");
    }

    private static Sample pieceSet;

    @Test
    @Order(9)
    void waveFunctionCollapse() {
        if (pieceSet == null) generatePieces();
        final Bounds bounds = new Bounds(0, 0, 0, 10, 10, 10);
        Piece defaultPiece = new Piece(2, 2, 2, AIR);
        for (int i = 0; i < 4; i++) {
            try {
                Wave wave = new Wave(pieceSet, bounds, defaultPiece);
                System.out.println("Collapsing the wave with modulo coords, try " + i);
                wave.collapse();
                System.out.println("Yay, the wave has collapsed!");
                if (wave.hasImpossibleStates()) System.out.println("But it has impossible states...");
                return;
            } catch (Wave.GenerationFailedException e) {
                System.out.println("The wave has failed to collapse, retrying...");
            }
        }
        fail("The wave has failed to collapse after 4 attempts");
    }

    @Test
    @Order(10)
    void waveFunctionCollapseWithoutModuloCoords() {
        if (pieceSet == null) generatePieces();
        final Bounds bounds = new Bounds(0, 0, 0, 10, 10, 10);
        Piece defaultPiece = new Piece(2, 2, 2, AIR);
        for (int i = 0; i < 4; i++) {
            try {
                Wave wave = new Wave(pieceSet, bounds, defaultPiece, false);
                System.out.println("Collapsing the wave without modulo coords, try " + i);
                wave.collapse();
                System.out.println("Yay, the wave has collapsed!");
                if (wave.hasImpossibleStates()) System.out.println("But it has impossible states...");
                return;
            } catch (Wave.GenerationFailedException e) {
                System.out.println("The wave has failed to collapse, retrying...");
            }
        }
        fail("The wave has failed to collapse after 4 attempts");
    }

    @Test
    @Order(11)
    void gcd() {
        assertEquals(8, WeightedSet.gcd(8, 32));
        assertEquals(1, WeightedSet.gcd(8, 33));
        assertEquals(33, WeightedSet.gcd(0, 33));
        assertEquals(2, WeightedSet.gcd(228, 346));
        WeightedSet<String> hws = new WeightedSet<>();
        hws.add("10 times", 10);
        hws.add("30 times", 30);
        hws.add("100 times", 100);
        hws.simplify();
        assertEquals(14, hws.getTotalWeight());
    }
}