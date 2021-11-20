package fr.bananasmoothii.mcwfc;

import java.util.Random;

public class GeneratingWorld {
    private final VirtualSpace<Piece> world;
    private final Random random;

    public GeneratingWorld() {
        world = new VirtualSpace<>();
        random = new Random();
    }

    public GeneratingWorld(long seed) {
        world = new VirtualSpace<>();
        random = new Random(seed);
    }
}
