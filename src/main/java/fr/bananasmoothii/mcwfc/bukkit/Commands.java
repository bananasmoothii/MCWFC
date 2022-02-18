package fr.bananasmoothii.mcwfc.bukkit;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandManager;
import co.aikar.commands.CommandOperationContext;
import co.aikar.commands.annotation.*;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import fr.bananasmoothii.mcwfc.core.GeneratingWorld;
import fr.bananasmoothii.mcwfc.core.MCVirtualSpace;
import fr.bananasmoothii.mcwfc.core.PieceNeighbors;
import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.WeightedSet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static fr.bananasmoothii.mcwfc.bukkit.MCWFCPlugin.sendMessage;

@CommandAlias("mcwfc")
@CommandPermission("mcwfc.use")
public class Commands extends BaseCommand {
    private static final Map<Player, WeightedSet<PieceNeighbors>> pieceSets = new WeakHashMap<>();

    @Subcommand("generate dataset")
    @Syntax("<sample size> [allow upside down (default: false)]")
    @CommandCompletion("@range:1-20 true|false")
    @Description("Generates a data set of your current selection. This does NOT modify the world, it just generates a " +
            "data set that you can use later with /mcwfc generate. The \"allow upside down\" parameter specifies if " +
            "you want to have some terrain set sideways and upside-down or not. This command does not support block " +
            "rotating for now, so expect stairs, chest and other blocs to be in the wrong way.")
    public static void generatePieces(final Player player, final String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(MCWFCPlugin.inst(), () -> {
            if (args.length == 0) {
                sendMessage(player, "§c");
            }
            int pieceSize = Integer.parseInt(args[0]);
            if (pieceSize < 1) {
                sendMessage(player, "§cSample size must be greater than 0");
                return;
            }
            if (pieceSize > 20) {
                sendMessage(player, "§cWow, " + pieceSize + " is too big for me (please try something like 3 or 4)");
                return;
            }

            boolean allowUpsideDown = false;
            if (args.length >= 2) {
                allowUpsideDown = Boolean.parseBoolean(args[1]);
            }


            final Bounds bounds;
            try {
                bounds = getSelection(player);
            } catch (IncompleteRegionException | NullPointerException e) {
                sendMessage(player, "§cUnable to get your current selection");
                return;
            }
            final MCVirtualSpace space = new MCVirtualSpace(bounds, Material.AIR.createBlockData());
            final World playerWorld = player.getWorld();
            for (int x = bounds.xMin(); x <= bounds.xMax(); x++) {
                for (int y = bounds.yMin(); y <= bounds.yMax(); y++) {
                    for (int z = bounds.zMin(); z <= bounds.zMax(); z++) {
                        space.set(playerWorld.getBlockData(x, y, z), x, y, z);
                    }
                }
            }

            pieceSets.put(player, space.generatePieces(pieceSize, allowUpsideDown));

            sendMessage(player, "§aDone ! You can now run §n/mcwfc generate§a (Don't forget to move your " +
                    "selection if you don't want it to be modified)");
        });
    }

    @Subcommand("generate")
    @Description("Wipes out your current selection and replaces it with a generated \"map\" based on the pieces you " +
            "generated with /mcwfc generate pieces")
    @Syntax("[seed (default: random number)]")
    @CommandCompletion("@nothing")
    public static void generate(Player player, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(MCWFCPlugin.inst(), () -> {
            WeightedSet<PieceNeighbors> pieces = pieceSets.get(player);
            if (pieces == null) {
                sendMessage(player, "§eYou have currently no piece set. You can generate one with /mcwfc generate " +
                        "dataset");
                return;
            }

            long seed;
            if (args.length >= 1) {
                seed = Long.parseLong(args[0]);
            } else {
                seed = ThreadLocalRandom.current().nextLong();
            }

            final Bounds bounds;
            try {
                bounds = getSelection(player);
            } catch (IncompleteRegionException | NullPointerException e) {
                sendMessage(player, "§cUnable to get your current selection");
                return;
            }

            final BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
            final CommandOperationContext<?> commandCtx = CommandManager.getCurrentCommandOperationContext();
            final String command = commandCtx.getCommandLabel() + String.join(" ", commandCtx.getArgs());
            GeneratingWorld gen = new GeneratingWorld(seed, pieces);
            final LocalSession playerSession = bukkitPlayer.getSession();
            gen.onPieceChangeEvent((pieceX, pieceY, pieceZ, piece) ->
                    Bukkit.getScheduler().runTaskAsynchronously(MCWFCPlugin.inst(), () -> {
                        int xMin = pieceX * piece.xSize;
                        int yMin = pieceY * piece.ySize;
                        int zMin = pieceZ * piece.zSize;
                        int xMax = pieceX * (piece.xSize + 1); // max coords are exclusive
                        int yMax = pieceY * (piece.ySize + 1);
                        int zMax = pieceZ * (piece.zSize + 1);
                        try (final EditSession editSession = playerSession.createEditSession(bukkitPlayer, command)) {
                            for (int x = xMin; x < xMax; x++) {
                                for (int y = yMin; y < yMax; y++) {
                                    for (int z = zMin; z < zMax; z++) {
                                        editSession.setBlock(x, y, z, BukkitAdapter.adapt(piece.get(x, y, z)));
                                    }
                                }
                            }
                            playerSession.remember(editSession);
                        }
                    }));
            gen.generateWFC(bounds);
            
        });
    }

    private static @NotNull Bounds getSelection(@NotNull Player player) throws IncompleteRegionException {
        Region selection;
        selection = BukkitAdapter.adapt(player).getSession().getSelection();
        final BlockVector3 min = selection.getMinimumPoint();
        final BlockVector3 max = selection.getMaximumPoint();
        return new Bounds(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }
}
