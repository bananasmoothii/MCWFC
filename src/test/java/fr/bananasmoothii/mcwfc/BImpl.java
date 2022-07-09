package fr.bananasmoothii.mcwfc;

/**
 * Some easy blocks for {@link fr.bananasmoothii.mcwfc.core.Piece}
 */
public class BImpl {

    public static final BImpl AIR = new BImpl(Material.AIR);
    public static final BImpl STONE = new BImpl(Material.STONE);
    public static final BImpl LEAVES = new BImpl(Material.OAK_LEAVES);

    private final Material material;

    public BImpl(Material material) {
        this.material = material;
    }

    @Override
    public String toString() {
        if (this == AIR) return "-";
        return String.valueOf(material.name().charAt(0));
    }

    public enum Material {
        AIR, STONE, OAK_LEAVES
    }
}
