package fr.bananasmoothii.mcwfc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VirtualWorldTest {
    @Test
    void virtualWordTest() {
        VirtualWorld<String> world = new VirtualWorld<>();
        world.set("a", 1, 0, 0);
        world.set("b", -3, 0, 0);
        world.set("c", 18, -20, 0);
        assertEquals("c", world.get(18, -20, 0));
        for (int x = -20; x <= 20; x++) {
            for (int y = -20; y <= 20; y++) {
                for (int z = -20; z <= 20; z++) {
                    world.set(String.valueOf(x + y + z), x, y, z);
                }
            }
        }
    }
}