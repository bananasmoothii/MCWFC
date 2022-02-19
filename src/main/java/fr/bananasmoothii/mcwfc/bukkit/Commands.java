package fr.bananasmoothii.mcwfc.bukkit;

import co.aikar.commands.BaseCommand;
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
import fr.bananasmoothii.mcwfc.core.Piece;
import fr.bananasmoothii.mcwfc.core.PieceNeighbors;
import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Face;
import fr.bananasmoothii.mcwfc.core.util.HashWeightedSet;
import fr.bananasmoothii.mcwfc.core.util.PieceNeighborsSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static fr.bananasmoothii.mcwfc.bukkit.MCWFCPlugin.sendMessage;

@SuppressWarnings("unused")
@CommandAlias("mcwfc")
@CommandPermission("mcwfc.use")
public class Commands extends BaseCommand {
    private static final Map<Player, PieceNeighborsSet> pieceSets = new WeakHashMap<>();

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
                sendMessage(player, "§cNot enough arguments, please provide <sample size>");
                return;
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
            sendMessage(player, "Generating dataset... (this may take a while)");
            for (int x = bounds.xMin(); x <= bounds.xMax(); x++) {
                for (int y = bounds.yMin(); y <= bounds.yMax(); y++) {
                    for (int z = bounds.zMin(); z <= bounds.zMax(); z++) {
                        space.set(playerWorld.getBlockData(x, y, z), x, y, z);
                    }
                }
            }

            final PieceNeighborsSet dataSet = space.generatePieces(pieceSize, allowUpsideDown);
            if (dataSet.size() > 0) {
                pieceSets.put(player, dataSet);
                sendMessage(player, "§aDone ! Dataset size: " + dataSet.size() + ". You can now run §n/mcwfc " +
                        "generate§a (Don't forget to move your selection if you don't want it to be modified)");
            } else {
                sendMessage(player, "§eThe dataset is generated, but it is empty so idk what happened");
            }
        });
    }

    @Subcommand("generate")
    @Description("Wipes out your current selection and replaces it with a generated \"map\" based on the pieces you " +
            "generated with /mcwfc generate pieces")
    @Syntax("[seed (default: random number)]")
    @CommandCompletion("@nothing")
    public static void generate(Player player, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(MCWFCPlugin.inst(), () -> {
            PieceNeighborsSet pieces = pieceSets.get(player);
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
            GeneratingWorld gen = new GeneratingWorld(seed, pieces);
            final LocalSession playerSession = bukkitPlayer.getSession();
            gen.onPieceChangeEvent((pieceX, pieceY, pieceZ, piece) ->
                    Bukkit.getScheduler().runTaskAsynchronously(MCWFCPlugin.inst(), () -> {
                        int xMin = pieceX * piece.xSize;
                        int yMin = pieceY * piece.ySize;
                        int zMin = pieceZ * piece.zSize;
                        int xMax = (pieceX + 1) * piece.xSize; // max coords are exclusive
                        int yMax = (pieceY + 1) * piece.ySize;
                        int zMax = (pieceZ + 1) * piece.zSize;
                        try (final EditSession editSession = playerSession.createEditSession(bukkitPlayer, "mcwfc generate")) {
                            for (int x = xMin, xInPiece = 0; x < xMax; x++, xInPiece++) {
                                for (int y = yMin, yInPiece = 0; y < yMax; y++, yInPiece++) {
                                    for (int z = zMin, zInPiece = 0; z < zMax; z++, zInPiece++) {
                                        editSession.setBlock(x, y, z, BukkitAdapter.adapt(piece.get(xInPiece, yInPiece, zInPiece)));
                                    }
                                }
                            }
                            playerSession.remember(editSession);
                        }
                    }));
            sendMessage(player, "Generating... (this may take a while)");
            gen.generateWFC(bounds);
            sendMessage(player, "§aDone with generating !");
        });
    }

    @Subcommand("dumppiece")
    @Description("copies the n-th piece from your dataset near you")
    @Syntax("<piece number>")
    @CommandCompletion("@range:0-10")
    public static void dumpPiece(Player player, int pieceNumber) {
        final PieceNeighborsSet dataSet = pieceSets.get(player);
        if (dataSet == null) {
            sendMessage(player, "§eYou have currently no piece set. You can generate one with /mcwfc generate " +
                    "dataset");
            return;
        }
        if (pieceNumber >= dataSet.size()) {
            sendMessage(player, "§cPiece number is too high. Try a number between 0 and " + (dataSet.size() - 1));
            return;
        }
        final Iterator<PieceNeighbors> iter = dataSet.iterator();
        for (int i = 0; i < pieceNumber; i++) {
            iter.next();
        }
        final PieceNeighbors piece = iter.next();
        final BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
        final LocalSession playerSession = bukkitPlayer.getSession();
        final Location playerLocation = player.getLocation();
        final int playerX = playerLocation.getBlockX();
        final int playerY = playerLocation.getBlockY();
        final int playerZ = playerLocation.getBlockZ();
        Bukkit.getScheduler().runTaskAsynchronously(MCWFCPlugin.inst(), () -> {
            try (final EditSession editSession = playerSession.createEditSession(bukkitPlayer, "mcwfc dumppiece")) {
                placePiece(piece.getCenterPiece(), editSession, playerX, playerY, playerZ);
                for (Map.Entry<Face, HashWeightedSet<Piece>> faceEntry : piece.getNeighbors().entrySet()) {
                    final Face face = faceEntry.getKey();
                    placePiece(faceEntry.getValue().weightedChoose(), editSession,
                            playerX + face.getModX(), playerY + face.getModY(), playerZ + face.getModZ());
                }
                playerSession.remember(editSession);
            }
            sendMessage(player, "§aDone !");
        });
    }

    @Contract(pure = true)
    private static void placePiece(final @NotNull Piece piece, final @NotNull EditSession editSession,
                                   final int x, final int y, final int z) {
        for (int x1 = x; x1 < x + piece.xSize; x1++) {
            for (int y1 = y; y1 < y + piece.ySize; y1++) {
                for (int z1 = z; z1 < z + piece.zSize; z1++) {
                    editSession.setBlock(x1, y1, z1, BukkitAdapter.adapt(piece.get(x1 - x, y1 - y, z1 - z)));
                }
            }
        }
    }

    private static @NotNull Bounds getSelection(@NotNull Player player) throws IncompleteRegionException {
        Region selection;
        selection = BukkitAdapter.adapt(player).getSession().getSelection();
        final BlockVector3 min = selection.getMinimumPoint();
        final BlockVector3 max = selection.getMaximumPoint();
        return new Bounds(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }
}
