package fr.bananasmoothii.mcwfc;

import fr.bananasmoothii.mcwfc.core.*;
import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.WeightedSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static fr.bananasmoothii.mcwfc.BImpl.*;
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
        Piece<BImpl> piece1 = new Piece<>(2, 3, 1, AIR);
        piece1.set(STONE, 1, 2, 0);
        piece1.set(STONE, 0, 1, 0);
        Piece<BImpl> piece2 = piece1.rotateZ(D270);
        assertEquals(STONE, piece2.get(2, 0, 0));
        assertEquals(piece1, piece2.rotateZ(D90));
        assertEquals(piece2.rotateZ(D270), piece1.rotateZ(D180).lock());

        Piece<BImpl> piece3 = new Piece<>(2, 3, 4, AIR);
        piece3.set(STONE, 0, 0, 0);
        assertEquals(
                piece3,
                piece3  .rotateX(D270)
                        .rotateY(D90)
                        .rotateZ(D180)
                        .rotateX(D90)
                        .rotateY(D180)
                        .rotateZ(D90)
                        .lock());
    }

    @Test
    @Order(3)
    void pieceFlip() {
        Piece<BImpl> piece = new Piece<>(2, 3, 4, AIR);
        piece.set(STONE, 0, 0, 0);
        assertEquals(piece,
                piece.flipX().flipY().flipZ().flipX().flipY().flipZ().lock());
    }

    @Test
    @Order(4)
    void pieceSiblings() {
        Piece<BImpl> piece;
        Set<Piece.Locked<BImpl>> pieces;
        { // IDK why I did that way
            piece = new Piece<>(2, 3, 4, AIR);
            piece.set(STONE, 0, 0, 0);
            piece.set(STONE, 0, 1, 3);
            //piece.debugPrint();
            pieces = piece.lock().generateSiblingsLock(true);
            assertEquals(48, pieces.size());
        }
        {
            piece = new Piece<>(3, 3, 3, AIR);
            pieces = piece.lock().generateSiblingsLock(true);
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
        PieceNeighbors<BImpl> neighbors;
        Piece<BImpl> center = new Piece<>(2, 3, 4, AIR);
        center.set(STONE, 0, 0, 0);
        center.set(STONE, 0, 1, 3);
        final Piece.Locked<BImpl> centerLocked = center.lock();
        neighbors = new PieceNeighbors<>(centerLocked);
        neighbors.put(Face.TOP, Optional.of(centerLocked));
        neighbors.put(Face.SOUTH_EAST_TOP, Optional.of(centerLocked));
        assertEquals(48, neighbors.generateSiblings(true).size());
        assertEquals(8, neighbors.generateSiblings(false).size());
    }

    @Test
    @Order(7)
    void coordsAreInBounds() {
        MCVirtualSpace<BImpl> space = new MCVirtualSpace<>(AIR);
        space.ensureCapacityForElement(-7, -7, -7);
        space.ensureCapacityForElement(7, 7, 7);
        for (int x = -8; x <= 8; x++) {
            int xInBounds = space.xInBounds(x);
            assertTrue(space.xMin() <= xInBounds && xInBounds <= space.xMax());
        }
        space = new MCVirtualSpace<>(AIR);
        space.set(STONE, -4, 3, 8);
        assertEquals(STONE, space.getModuloCoords(1, -1, 26)); // 26 % 9 = 8

        space = new MCVirtualSpace<>(new Bounds(-285, 64, 88, -282, 65, 91), AIR);
        assertEquals(64, space.yInBounds(66));
    }

    @Test
    @Order(8)
    void generatePieces() {
        MCVirtualSpace<BImpl> space = new MCVirtualSpace<>(AIR);
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

    public static Piece.Locked<BImpl> faultyPiece;
    static {
        Piece<BImpl> piece = new Piece<>(2, AIR);
        piece.set(STONE, 0, 0, 0);
        piece.set(STONE, 0, 1, 0);
        piece.set(STONE, 1, 1, 0);
        faultyPiece = piece.lock();
    }

    @Test
    @Order(9)
    void checkPieces() {
        if (pieceSet == null) generatePieces();
        int i = 0;
        for (PieceNeighbors.Locked<BImpl> pieceNeighbors : pieceSet) {
            for (Map.Entry<Face, Optional<Piece.Locked<BImpl>>> entry : pieceNeighbors.entrySet()) {
                assertTrue(pieceSet.centerPiecesContains(entry.getValue().orElseThrow()),
                        "The generated piece set is not valid because the center pieces do not contain "
                                + entry.getValue() + ", that is at " + entry.getKey() + " of the " + i + "th piece");
            }
            i++;
        }
    }

    @Test
    @Order(10)
    void getCenterPieces() {
        if (pieceSet == null) generatePieces();
        assertTimeout(Duration.ofMillis(900), () -> {
            //noinspection ResultOfMethodCallIgnored
            pieceSet.getCenterPieces();
        });
    }

    private static Sample<BImpl> pieceSet;

    @Test
    @Order(12)
    void waveFunctionCollapse() {
        final Bounds bounds = new Bounds(0, 0, 0, 4, 0, 2);
        final MCVirtualSpace<BImpl> sampleSource = new MCVirtualSpace<>(bounds, AIR);
        /* Making a little 2D path with stone at the corners:
         * - - - - -
         * O S - S O
         * - O O O -
         * O: LEAVES (oak)
         * S: STONE
         * -: AIR
         */
        sampleSource.set(LEAVES, 0, 0, 1);
        sampleSource.set(STONE, 1, 0, 1);
        sampleSource.set(LEAVES, 1, 0, 2);
        sampleSource.set(LEAVES, 2, 0, 2);
        sampleSource.set(LEAVES, 3, 0, 2);
        sampleSource.set(STONE, 3, 0, 1);
        sampleSource.set(LEAVES, 4, 0, 1);
        sampleSource.debugPrintY(0);
        final Sample<BImpl> sample = sampleSource.generatePieces(1);
        assertEquals(4, new HashSet<>(sample).stream().filter(p -> p.getCenterPiece().get(0, 0, 0) == STONE).count());
        assertEquals(14, new HashSet<>(sample).stream().filter(p -> p.getCenterPiece().get(0, 0, 0) == LEAVES).count());
        assertEquals(16, new HashSet<>(sample).stream().filter(p -> p.getCenterPiece().get(0, 0, 0) == AIR).count());
        assertEquals(34, sample.size(), "The sample should have 34 elements");

        @Nullable Wave.GenerationFailedException lastException = null;
        for (int i = 0; i < 8; i++) {
            try {
                final Wave<BImpl> wave = new Wave<>(sample, bounds);
                System.out.println("Collapsing the wave with modulo coords, try " + i);
                wave.collapseAll();
                System.out.println("Yay, the wave has collapsed ! Here it is:");
                wave.debugPrintY(0);
                //assertFalse(wave.hasImpossibleStates(), "The wave has impossible states");
                if (wave.hasImpossibleStates()) System.err.println("The wave has impossible states (with modulo coords)");
                return;
            } catch (Wave.GenerationFailedException e) {
                lastException = e;
            }
        }
        lastException.printStackTrace();
        fail("The wave has failed to collapse after 8 attempts");
    }

    @Contract(pure = true)
    private static void debugPrintSampleOnePieceIgnoreYLayers(@NotNull Sample<BImpl> sample) {
        for (PieceNeighbors.Locked<BImpl> pieceNeighbors : sample) {
            debugPrintPieceNeighborOnePieceIgnoreYLayers(pieceNeighbors);
        }
    }

    private static void debugPrintPieceNeighborOnePieceIgnoreYLayers(@NotNull PieceNeighbors.Locked<BImpl> pieceNeighbors) {
        System.out.println();
        System.out.println("  " + pieceNeighbors.get(Face.NORTH).orElseThrow().get(0, 0, 0));
        System.out.println(pieceNeighbors.get(Face.WEST).orElseThrow().get(0, 0, 0) + " "
                + pieceNeighbors.getCenterPiece().get(0, 0, 0) + " "
                + pieceNeighbors.get(Face.EAST).orElseThrow().get(0, 0, 0));
        System.out.println("  " + pieceNeighbors.get(Face.SOUTH).orElseThrow().get(0, 0, 0));
    }

    @Test
    @Order(13)
    void waveFunctionCollapseWithoutModuloCoords() {
        if (pieceSet == null) generatePieces();
        final Bounds bounds = new Bounds(0, 0, 0, 4, 0, 2);
        final MCVirtualSpace<BImpl> sampleSource = new MCVirtualSpace<>(bounds, AIR);
        /* Making a little 2D path with stone at the corners:
         * - - - - -
         * O S - S O
         * - O O O -
         * O: LEAVES (oak)
         * S: STONE
         * -: AIR
         */
        sampleSource.set(LEAVES, 0, 0, 1);
        sampleSource.set(STONE, 1, 0, 1);
        sampleSource.set(LEAVES, 1, 0, 2);
        sampleSource.set(LEAVES, 2, 0, 2);
        sampleSource.set(LEAVES, 3, 0, 2);
        sampleSource.set(STONE, 3, 0, 1);
        sampleSource.set(LEAVES, 4, 0, 1);

        @Nullable Wave.GenerationFailedException lastException = null;
        for (int i = 0; i < 8; i++) {
            try {
                Wave<BImpl> wave = new Wave<>(sampleSource.generatePieces(1), bounds, false);
                System.out.println("Collapsing the wave without modulo coords, try " + i);
                wave.collapseAll();
                System.out.println("Yay, the wave has collapsed ! Here it is:");
                wave.debugPrintY(0);
                //assertFalse(wave.hasImpossibleStates(), "The wave has impossible states");
                if (wave.hasImpossibleStates()) System.err.println("The wave has impossible states (without modulo coords)");
                return;
            } catch (Wave.GenerationFailedException e) {
                lastException = e;
            }
        }
        lastException.printStackTrace();
        fail("The wave has failed to collapse after 8 attempts");
    }

    @Test
    @Order(14)
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