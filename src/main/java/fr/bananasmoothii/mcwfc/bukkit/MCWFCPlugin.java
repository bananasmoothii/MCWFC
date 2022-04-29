package fr.bananasmoothii.mcwfc.bukkit;

import co.aikar.commands.PaperCommandManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class MCWFCPlugin extends JavaPlugin {

    private static MCWFCPlugin inst;
    public static Logger log;

    @Override
    public void onEnable() {
        inst = this;
        log = getLogger();
        PaperCommandManager manager = new PaperCommandManager(this);
        manager.registerCommand(new Commands());
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
