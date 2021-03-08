package net.slipcor.pvparena.modules.worldedit;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;

public class PAWE extends ArenaModule {
    private static WorldEditPlugin worldEdit;
    private boolean needsLoading = false;

    public PAWE() {
        super("WorldEdit");
    }

    private String loadPath = "";

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "regload".equals(s) || "regsave".equals(s) || "regcreate".equals(s)
                || "!we".equals(s) || "worldedit".equals(s) || "regexlist".equals(s);
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("regload", "regsave", "regcreate", "worldedit", "regexlist");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!we");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }

        for (final ArenaRegion region : arena.getRegions()) {
            result.define(new String[]{region.getRegionName()});
        }
        result.define(new String[]{"all"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pvparena.admin")) {
            arena.msg(sender, Language.parse(MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        // /pa reg[save|load] [regionname] {filename}

        if (args.length < 2) {
            helpCommands(arena, sender);
            return;
        }

        final ArenaRegion ars = arena.getRegion(args[1]);

        if (args.length < 3) {

            if (args[0].endsWith("load")) {
                if (ars == null && !args[1].equalsIgnoreCase("all")) {
                    arena.msg(sender, Language.parse(MSG.ERROR_REGION_NOTFOUND, args[1]));
                    return;
                } else if (ars == null) {
                    Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.BATTLE);
                    for (ArenaRegion region : regions) {
                        load(region);
                        arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_LOADED, region.getRegionName()));
                    }
                    return;
                }
                load(ars);
                arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_LOADED, args[1]));
                return;
            }
            if (args[0].endsWith("save")) {
                if (ars == null && !args[1].equalsIgnoreCase("all")) {
                    arena.msg(sender, Language.parse(MSG.ERROR_REGION_NOTFOUND, args[1]));
                    return;
                } else if (ars == null) {
                    Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.BATTLE);
                    for (ArenaRegion region : regions) {
                        save(region);
                        arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_SAVED, region.getRegionName()));
                    }
                    return;
                }
                save(ars);
                arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_SAVED, args[1]));
                return;
            }
            if (args[0].endsWith("create")) {
                create((Player) sender, arena, args[1]);
                arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_CREATED, args[1]));
                return;
            }
            if (args[0].endsWith("regexlist")) {
                if (ars == null) {
                    arena.msg(sender, Language.parse(MSG.ERROR_REGION_NOTFOUND, args[1]));
                    return;
                }
                final List<String> regions = arena.getArenaConfig().getStringList(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), new ArrayList<String>());
                if (args.length < 2) {
                    arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_LIST_SHOW, StringParser.joinList(regions, ", ")));
                    return;
                }

                if (!regions.contains(ars.getRegionName()) || args.length > 2 && StringParser.positive.contains(args[2])) {
                    regions.add(ars.getRegionName());
                    arena.getArenaConfig().setManually(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), regions);
                    arena.getArenaConfig().save();
                    arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_LIST_ADDED, ars.getRegionName()));
                    return;
                }
                regions.remove(ars.getRegionName());
                arena.getArenaConfig().setManually(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), regions);
                arena.getArenaConfig().save();
                arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_LIST_REMOVED, ars.getRegionName()));
                return;
            }
            if (args[0].equals("!we")) {

                if (args[1].endsWith("save")) {
                    boolean b = arena.getArenaConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOSAVE);
                    arena.getArenaConfig().set(CFG.MODULES_WORLDEDIT_AUTOSAVE, !b);
                    arena.getArenaConfig().save();
                    arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_WORLDEDIT_AUTOSAVE.getNode(), String.valueOf(!b)));
                    return;
                }
                if (args[1].endsWith("load")) {
                    boolean b = arena.getArenaConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOLOAD);
                    arena.getArenaConfig().set(CFG.MODULES_WORLDEDIT_AUTOLOAD, !b);
                    arena.getArenaConfig().save();
                    arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_WORLDEDIT_AUTOLOAD.getNode(), String.valueOf(!b)));
                    return;
                }

                create((Player) sender, arena, args[1]);
                arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_CREATED, args[1]));
                return;
            }
            helpCommands(arena, sender);
        } else {
            if (args[0].endsWith("load")) {
                load(ars, args[2]);
                arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_LOADED, args[1]));
            } else if (args[0].endsWith("save")) {
                save(ars, args[2]);
                arena.msg(sender, Language.parse(MSG.MODULE_WORLDEDIT_SAVED, args[1]));
            } else {
                create((Player) sender, arena, args[1], args[2]);
            }
        }
    }

    private Location calculateBukkitLocation(Player p, BlockVector3 location) {
        return new Location(p.getWorld(), location.getX(), location.getY(), location.getZ());
    }

    private void create(final Player p, final Arena arena, final String regionName, final String regionShape) {
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(p.getWorld());
        Region selection = null;
        try {
            selection = worldEdit.getSession(p).getSelection(world);
        } catch (IncompleteRegionException e) {
            //
        }
        if (selection == null) {
            Arena.pmsg(p, Language.parse(MSG.ERROR_REGION_SELECT_2));
            return;
        }

        final ArenaPlayer ap = ArenaPlayer.fromPlayer(p);
        ap.setSelection(calculateBukkitLocation(p, selection.getMinimumPoint()), false);
        ap.setSelection(calculateBukkitLocation(p, selection.getMaximumPoint()), true);

        final PAA_Region cmd = new PAA_Region();
        final String[] args = {regionName, regionShape};

        cmd.commit(arena, p, args);
    }

    @Override
    public void configParse(YamlConfiguration config) {
        loadPath = config.getString(CFG.MODULES_WORLDEDIT_SCHEMATICPATH.getNode(), PVPArena.getInstance().getDataFolder().getAbsolutePath());
        if (loadPath.equals("")) {
            loadPath = PVPArena.getInstance().getDataFolder().getAbsolutePath();
        }
    }

    private void create(final Player p, final Arena arena, final String regionName) {
        create(p, arena, regionName, "CUBOID");
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage(StringParser.colorVar("autoload", arena.getArenaConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOLOAD)) +
                " | " + StringParser.colorVar("autosave", arena.getArenaConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOSAVE)));
    }

    private void helpCommands(final Arena arena, final CommandSender sender) {
        arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "/pa regsave [regionname] {filename}"));
        arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "/pa regload [regionname] {filename}"));
        arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "/pa regcreate [regionname]"));
        arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "/pa !we autoload"));
        arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "/pa !we autosave"));
        arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "/pa !we create [regionname]"));
    }

    private void load(final ArenaRegion ars) {
        load(ars, ars.getArena().getName() + '_' + ars.getRegionName());
    }

    private void load(final ArenaRegion ars, final String regionName) {

        try {
            final PABlockLocation min = ars.getShape().getMinimumLocation();
            final PABlockLocation max = ars.getShape().getMaximumLocation();
            final int size = (max.getX() + 2 - min.getX()) *
                    (max.getY() + 2 - min.getY()) *
                    (max.getZ() + 2 - min.getZ());
            final PABlockLocation loc = ars.getShape().getMinimumLocation();

            WorldEdit worldEdit = WorldEdit.getInstance();
            File loadFile = new File(this.loadPath, regionName + ".schem");
            if (!loadFile.exists()) {
                loadFile = new File(this.loadPath, regionName + ".schematic");
            }

            ClipboardFormat format = ClipboardFormats.findByFile(loadFile);

            if (format == null) {
                throw new IllegalArgumentException("Unrecognized WE format: " + loadFile.getName());
            }

            try (ClipboardReader reader = format.getReader(new FileInputStream(loadFile))) {
                Clipboard clipboard = reader.read();
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                BukkitWorld bukkitWorld = new BukkitWorld(ars.getWorld());

                final EditSession editSession = worldEdit.getEditSessionFactory().getEditSession(bukkitWorld, size);
                editSession.setReorderMode(EditSession.ReorderMode.FAST);
                editSession.setFastMode(true);
                BlockVector3 to = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());
                Operation operation=holder.createPaste(editSession)
                        .to(to)
                        .ignoreAirBlocks(!this.arena.getArenaConfig().getBoolean(CFG.MODULES_WORLDEDIT_REPLACEAIR))
                        .build();
                Operations.completeLegacy(operation);
                editSession.flushSession();
            }
        } catch (IllegalArgumentException e) {
            PVPArena.getInstance().getLogger().severe(e.getMessage());
        } catch (final IOException | MaxChangedBlocksException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean needsBattleRegion() {
        return true;
    }

    @Override
    public void onThisLoad() {
        final Plugin pwep = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (pwep != null && pwep.isEnabled() && pwep instanceof WorldEditPlugin) {
            worldEdit = (WorldEditPlugin) pwep;
        }
    }

    @Override
    public void parseStart() {
        if (arena.getArenaConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOSAVE)) {
            List<String> regions = arena.getArenaConfig().getStringList(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), new ArrayList<String>());
            if (regions.size() > 0) {
                for (String regionName : regions) {
                    ArenaRegion region = arena.getRegion(regionName);
                    if (region != null) {
                        save(region);
                    }
                }
                return;
            }
            for (final ArenaRegion ars : arena.getRegionsByType(RegionType.BATTLE)) {
                save(ars);
            }
        }
        needsLoading = true;
    }

    @Override
    public void reset(final boolean force) {
        if (needsLoading && arena.getArenaConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOLOAD)) {
            List<String> regions = arena.getArenaConfig().getStringList(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), new ArrayList<String>());
            if (regions.size() > 0) {
                for (String regionName : regions) {
                    ArenaRegion region = arena.getRegion(regionName);
                    if (region != null) {
                        load(region);
                    }
                }
                return;
            }
            for (final ArenaRegion ars : arena.getRegionsByType(RegionType.BATTLE)) {
                load(ars);
            }
        }
        needsLoading = false;
    }

    private void save(final ArenaRegion ars) {
        save(ars, ars.getArena().getName() + '_' + ars.getRegionName());
    }

    private BlockVector3 getVector(org.bukkit.util.Vector vector) {
        return BlockVector3.at(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    private void save(final ArenaRegion arena, final String regionName) {
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(arena.getWorld());

        Region region = new CuboidRegion(world,
                getVector(arena.getShape().getMinimumLocation().toLocation().toVector()),
                getVector(arena.getShape().getMaximumLocation().toLocation().toVector()));

        final PABlockLocation lmin = arena.getShape().getMinimumLocation();
        final PABlockLocation lmax = arena.getShape().getMaximumLocation();
        final int size = (lmax.getX() - lmin.getX()) *
                (lmax.getY() - lmin.getY()) *
                (lmax.getZ() - lmin.getZ());

        final BlockArrayClipboard clipboard = new BlockArrayClipboard(region);


        final EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, size);

        ForwardExtentCopy extentCopy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
        extentCopy.setCopyingEntities(true);

        try {
            Operations.complete(extentCopy);
            ClipboardFormat format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
            ClipboardWriter writer = format.getWriter(new FileOutputStream(new File(loadPath, regionName + ".schem")));
            writer.write(clipboard);
            writer.close();
        } catch (WorldEditException | IOException e) {
            e.printStackTrace();
        }
    }
}
