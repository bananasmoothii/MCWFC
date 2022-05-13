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
import fr.bananasmoothii.mcwfc.core.*;
import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Face;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
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
    private static final Map<Player, Sample<BlockData>> pieceSets = new WeakHashMap<>();
    private static boolean INCREMENTAL_GENERATION = false; // CAUTION: very laggy when true

    @Subcommand("generate dataset")
    @Syntax("<sample size> [allow upside down (default: false)] [use modulo coords for top and bottom (default: false)]")
    @CommandCompletion("@range:1-20 true|false true|false")
    @Description("Generates a data set of your current selection. This does NOT modify the world, it just generates a " +
            "data set that you can use later with /mcwfc generate. The \"allow upside down\" parameter specifies if " +
            "you want to have some terrain set sideways and upside-down or not. This command does not support block " +
            "rotating for now, so expect stairs, chest and other blocs to be in the wrong way. The parameter \"use " +
            "modulo coords for top and bottom\" means that when the generator is at the top of this sample (your " +
            "current selection, should it consider that adding 1 to the y-axis makes it go to the bottom of this " +
            "selection and vice-versa ? If \"false\", it will be almost impossible to generate something taller than " +
            "your current selection.")
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

            boolean useModuloCoordsTopAndBottom = true;
            if (args.length >= 3) {
                useModuloCoordsTopAndBottom = Boolean.parseBoolean(args[2]);
            }


            final Bounds bounds;
            try {
                bounds = getSelection(player);
            } catch (IncompleteRegionException | NullPointerException e) {
                sendMessage(player, "§cUnable to get your current selection");
                return;
            }
            final MCVirtualSpace<BlockData> space = new MCVirtualSpace<>(bounds, Material.AIR.createBlockData());
            final World playerWorld = player.getWorld();
            sendMessage(player, "Generating dataset... (this may take a while)");
            for (int x = bounds.xMin(); x <= bounds.xMax(); x++) {
                for (int y = bounds.yMin(); y <= bounds.yMax(); y++) {
                    for (int z = bounds.zMin(); z <= bounds.zMax(); z++) {
                        space.set(playerWorld.getBlockData(x, y, z), x, y, z);
                    }
                }
            }

            final Sample<BlockData> dataSet = space.generatePieces(pieceSize, allowUpsideDown, useModuloCoordsTopAndBottom);
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
            "generated with /mcwfc generate pieces. The parameter \"use modulo coords\" is used when the generator " +
            "comes to an edge. If it is set to true (the default), instead of saying \"oh, there is an edge here\", " +
            "it will go to the opposite side of your selection.")
    @Syntax("[seed (default: random number)] [use modulo coords (default: true)]")
    @CommandCompletion("@nothing")
    public static void generate(Player player, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(MCWFCPlugin.inst(), () -> {
            Sample<BlockData> pieces = pieceSets.get(player);
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

            boolean useModuloCoords = true;
            if (args.length >= 2) {
                useModuloCoords = Boolean.parseBoolean(args[1]);
            }

            final Bounds bounds;
            try {
                bounds = getSelection(player);
            } catch (IncompleteRegionException | NullPointerException e) {
                sendMessage(player, "§cUnable to get your current selection");
                return;
            }

            final BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
            Wave<BlockData> wave = new Wave<>(pieces, bounds, useModuloCoords, seed);
            final LocalSession playerSession = bukkitPlayer.getSession();
            if (INCREMENTAL_GENERATION)
                wave.registerPieceCollapseListener((pieceX, pieceY, pieceZ, piece) ->
                        Bukkit.getScheduler().runTaskAsynchronously(MCWFCPlugin.inst(), () -> {
                            final Piece.Locked<BlockData> centerPiece = piece.getCenterPiece();
                            int xMin = pieceX * centerPiece.xSize;
                            int yMin = pieceY * centerPiece.ySize;
                            int zMin = pieceZ * centerPiece.zSize;
                            int xMax = (pieceX + 1) * centerPiece.xSize; // max coords are exclusive
                            int yMax = (pieceY + 1) * centerPiece.ySize;
                            int zMax = (pieceZ + 1) * centerPiece.zSize;
                            try (final EditSession editSession = playerSession.createEditSession(bukkitPlayer, "mcwfc generate")) {
                                for (int x = xMin, xInPiece = 0; x < xMax; x++, xInPiece++) {
                                    for (int y = yMin, yInPiece = 0; y < yMax; y++, yInPiece++) {
                                        for (int z = zMin, zInPiece = 0; z < zMax; z++, zInPiece++) {
                                            editSession.setBlock(x, y, z, BukkitAdapter.adapt(centerPiece.get(xInPiece, yInPiece, zInPiece)));
                                        }
                                    }
                                }
                                //playerSession.remember(editSession);
                            }
                        }));

            sendMessage(player, "Generating... (this may take a while)");
            try {
                wave.collapse();
                if (!INCREMENTAL_GENERATION) {
                    for (VirtualSpace.ObjectWithCoordinates<Sample<BlockData>> node : wave.getWave()) {
                        Sample<BlockData> piecesAtNode = node.object();
                        if (piecesAtNode.isEmpty()) continue;
                        Piece.Locked<BlockData> piece = piecesAtNode.peek().getCenterPiece();
                        int xMin = node.x() * piece.xSize;
                        int yMin = node.y() * piece.ySize;
                        int zMin = node.z() * piece.zSize;
                        int xMax = (node.x() + 1) * piece.xSize; // max coords are exclusive
                        int yMax = (node.y() + 1) * piece.ySize;
                        int zMax = (node.z() + 1) * piece.zSize;
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
                    }
                }
                sendMessage(player, "§aDone with generating !");
            } catch (Wave.GenerationFailedException e) {
                sendMessage(player, "§cSorry, the generation failed. This can happen sometimes if your " +
                        "dataset is too complex. But this happens randomly, so just try again (if you set a seed, make sure " +
                        "to change it otherwise it will encounter the same problem). If you still get this error, please " +
                        "try with a less complex dataset, that means a dataset with a low variety (using less different " +
                        "blocs). You can also use a dataset with modulo coords (the default parameter)");
            } catch (OutOfMemoryError e) {
                sendMessage(player, "§cOops, your current selection is too big for this server, because " +
                        "it just ran out of memory. Please try again with a smaller selection. If you get this error " +
                        "even with a smaller generation area, this may be a bug.");
            }
        });
    }

    @Subcommand("dumppiece")
    @Description("copies the n-th piece from your dataset near you")
    @Syntax("<piece number>")
    @CommandCompletion("@range:0-20")
    public static void dumpPiece(Player player, int pieceNumber) {
        final Sample<BlockData> dataSet = pieceSets.get(player);
        if (dataSet == null) {
            sendMessage(player, "§eYou have currently no piece set. You can generate one with /mcwfc generate " +
                    "dataset");
            return;
        }
        if (pieceNumber >= dataSet.size()) {
            sendMessage(player, "§cPiece number is too high. Try a number between 0 and " + (dataSet.size() - 1));
            return;
        }
        final @NotNull Iterator<PieceNeighbors.Locked<BlockData>> iter = dataSet.iterator();
        for (int i = 0; i < pieceNumber; i++) {
            iter.next();
        }
        final PieceNeighbors.Locked<BlockData> piece = iter.next();
        final BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
        final LocalSession playerSession = bukkitPlayer.getSession();
        final Location playerLocation = player.getLocation();
        final int playerX = playerLocation.getBlockX();
        final int playerY = playerLocation.getBlockY();
        final int playerZ = playerLocation.getBlockZ();
        final int pieceSize = piece.getCenterPiece().xSize;
        Bukkit.getScheduler().runTaskAsynchronously(MCWFCPlugin.inst(), () -> {
            try (final EditSession editSession = playerSession.createEditSession(bukkitPlayer, "mcwfc dumppiece")) {
                placePiece(piece.getCenterPiece(), editSession, playerX, playerY, playerZ);
                for (Map.Entry<Face, Piece.Locked<BlockData>> faceEntry : piece.entrySet()) {
                    final Face face = faceEntry.getKey();
                    placePiece(faceEntry.getValue(), editSession,
                            playerX + face.getModX() * pieceSize, playerY + face.getModY() * pieceSize, playerZ + face.getModZ() * pieceSize);
                }
                playerSession.remember(editSession);
            }
            sendMessage(player, "§aDone !");
        });
    }

    @Contract(pure = true)
    private static void placePiece(final @NotNull Piece.Locked<BlockData> piece, final @NotNull EditSession editSession,
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
