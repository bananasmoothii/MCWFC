package fr.bananasmoothii.mcwfc.bukkit;

import co.aikar.commands.PaperCommandManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MCWFCPlugin extends JavaPlugin {

    private static MCWFCPlugin inst;

    @Override
    public void onEnable() {
        inst = this;
        PaperCommandManager manager = new PaperCommandManager(this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static MCWFCPlugin inst() {
        return inst;
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage("§7[§6MCWFC§7]§r " + message);
    }
}
