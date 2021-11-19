package fr.bananasmoothii.mcwfc;

import org.bukkit.Material;
import org.bukkit.SoundGroup;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"MethodDoesntCallSuperMethod", "ConstantConditions", "ClassCanBeRecord"})
public class BlockDataImpl implements BlockData {

    private final Material material;

    public BlockDataImpl(Material material) {
        this.material = material;
    }

    @NotNull
    @Override
    public Material getMaterial() {
        return material;
    }

    @NotNull
    @Override
    public String getAsString() {
        return material.name();
    }

    @NotNull
    @Override
    public String getAsString(boolean hideUnspecified) {
        return null;
    }

    @NotNull
    @Override
    public BlockData merge(@NotNull BlockData data) {
        return null;
    }

    @Override
    public boolean matches(@Nullable BlockData data) {
        return false;
    }

    @NotNull
    @Override
    public BlockData clone() {
        return this;
    }

    @NotNull
    @Override
    public SoundGroup getSoundGroup() {
        return null;
    }
}
