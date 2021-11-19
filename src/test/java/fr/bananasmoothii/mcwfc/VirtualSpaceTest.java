package fr.bananasmoothii.mcwfc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VirtualSpaceTest {

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
        VirtualSpace<String> space = new VirtualSpace<>();
        space.setFill("0");
        space.set("18", 10, 8, 0);
        space.set("81", -8, -10, 0);
        /*
        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                space.set(String.valueOf(x + y), x, y, 0);
            }
        }

         */
        space.debugPrint(0);
        space = space.rotateZ(VirtualSpace.RotationAngle.A90);
        space.debugPrintAllLayers();
        //assertEquals("18", space.get(8, -10, 0));
    }
}