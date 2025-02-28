package net.slipcor.pvparena.modules.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
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
import net.slipcor.pvparena.commands.AbstractArenaCommand;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PAWE extends ArenaModule {
    private static WorldEditPlugin worldEdit;
    private boolean needsLoading = false;

    public PAWE() {
        super("WorldEdit");
    }

    private String loadPath = "";

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!we".equals(s) || "worldedit".equals(s);
    }

    @Override
    public List<String> getMain() {
        return List.of("worldedit");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!we");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);

        Set<String> regionNames = arena.getRegions().stream()
                .map(ArenaRegion::getRegionName)
                .collect(Collectors.toSet());
        regionNames.add("all");

        for (String regionName : regionNames) {
            result.define(new String[]{"list", "add", regionName});
            result.define(new String[]{"list", "remove", regionName});
            result.define(new String[]{"save", regionName});
            result.define(new String[]{"load", regionName});
        }

        result.define(new String[]{"list", "show"});
        result.define(new String[]{"autosave"});
        result.define(new String[]{"autoload"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pvparena.admin")) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (args.length < 2) {
            this.arena.msg(sender, MSG.ERROR_COMMAND_UNKNOWN);
            return;
        }

        if("!we".equalsIgnoreCase(args[0]) || "worldedit".equalsIgnoreCase(args[0])) {
            // /pa !we autosave
            if (args[1].equalsIgnoreCase("autosave")) {
                boolean b = this.arena.getConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOSAVE);
                this.arena.getConfig().set(CFG.MODULES_WORLDEDIT_AUTOSAVE, !b);
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.CFG_SET_DONE, CFG.MODULES_WORLDEDIT_AUTOSAVE.getNode(), String.valueOf(!b));
                return;
            }

            // /pa !we autoload
            if (args[1].equalsIgnoreCase("autoload")) {
                boolean b = this.arena.getConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOLOAD);
                this.arena.getConfig().set(CFG.MODULES_WORLDEDIT_AUTOLOAD, !b);
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.CFG_SET_DONE, CFG.MODULES_WORLDEDIT_AUTOLOAD.getNode(), String.valueOf(!b));
                return;
            }

            // /pa !we create
            if("create".equalsIgnoreCase(args[1]) && args.length == 3) {
                this.create((Player) sender, this.arena, args[2]);
                this.arena.msg(sender, MSG.MODULE_WORLDEDIT_CREATED, args[2]);
                return;
            }

            // /pa !we list show|add|remove <regionName>
            if("list".equalsIgnoreCase(args[1])) {
                if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3, 4})) {
                    return;
                }

                final List<String> regions = this.arena.getConfig().getStringList(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), new ArrayList<>());

                if ("show".equalsIgnoreCase(args[2])) {
                    if(regions.isEmpty()) {
                        this.arena.msg(sender, MSG.MODULE_WORLDEDIT_LIST_EMPTY);
                    } else {
                        this.arena.msg(sender, MSG.MODULE_WORLDEDIT_LIST_SHOW, String.join(", ", regions));
                    }
                    return;
                }

                final ArenaRegion ars = this.arena.getRegion(args[3]);
                if("add".equalsIgnoreCase(args[2])) {
                    if (!regions.contains(ars.getRegionName())) {
                        regions.add(ars.getRegionName());
                        this.arena.getConfig().setManually(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), regions);
                        this.arena.getConfig().save();
                    }
                    this.arena.msg(sender, MSG.MODULE_WORLDEDIT_LIST_ADDED, ars.getRegionName());
                    return;
                } else if("remove".equalsIgnoreCase(args[2])){
                    if (regions.contains(ars.getRegionName())) {
                        regions.remove(ars.getRegionName());
                        this.arena.getConfig().setManually(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), regions);
                        this.arena.getConfig().save();
                    }
                    this.arena.msg(sender, MSG.MODULE_WORLDEDIT_LIST_REMOVED, ars.getRegionName());
                    return;
                }
            }

            final ArenaRegion ars = this.arena.getRegion(args[2]);

            // /pa !we save <all|regionName>
            if("save".equalsIgnoreCase(args[1])) {
                if ("all".equalsIgnoreCase(args[2]) && ars != null) {
                    Set<ArenaRegion> regions = this.arena.getRegionsByType(RegionType.BATTLE);
                    for (ArenaRegion region : regions) {
                        this.save(region);
                        this.arena.msg(sender, MSG.MODULE_WORLDEDIT_SAVED, region.getRegionName());
                    }
                    return;
                } else if(ars == null) {
                    this.arena.msg(sender, MSG.ERROR_REGION_NOTFOUND, args[2]);
                    return;
                }
                this.save(ars);
                this.arena.msg(sender, MSG.MODULE_WORLDEDIT_SAVED, args[2]);
                return;
            }

            // /pa !we load <all|regionName>
            if("load".equalsIgnoreCase(args[1])) {
                if ("all".equalsIgnoreCase(args[2]) && ars != null) {
                    Set<ArenaRegion> regions = this.arena.getRegionsByType(RegionType.BATTLE);
                    for (ArenaRegion region : regions) {
                        this.load(region);
                        this.arena.msg(sender, MSG.MODULE_WORLDEDIT_LOADED, region.getRegionName());
                    }
                    return;
                } else if(ars == null) {
                    this.arena.msg(sender, MSG.ERROR_REGION_NOTFOUND, args[2]);
                    return;
                }
                this.load(ars);
                this.arena.msg(sender, MSG.MODULE_WORLDEDIT_LOADED, args[2]);
                return;
            }
        }

        this.arena.msg(sender, MSG.ERROR_COMMAND_UNKNOWN);
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
            Arena.pmsg(p, MSG.ERROR_REGION_SELECT_2);
            return;
        }

        final ArenaPlayer ap = ArenaPlayer.fromPlayer(p);
        ap.setSelection(this.calculateBukkitLocation(p, selection.getMinimumPoint()), false);
        ap.setSelection(this.calculateBukkitLocation(p, selection.getMaximumPoint()), true);

        final PAA_Region cmd = new PAA_Region();
        final String[] args = {regionName, regionShape};

        cmd.commit(arena, p, args);
    }

    @Override
    public void configParse(YamlConfiguration config) {
        this.initConfig();
    }

    @Override
    public void initConfig() {
        String defaultPath = PVPArena.getInstance().getDataFolder().getPath() + "/schematics";
        this.loadPath = this.arena.getConfig().getString(CFG.MODULES_WORLDEDIT_SCHEMATICPATH, defaultPath);
        if (this.loadPath.trim().isEmpty()) {
            this.loadPath = defaultPath;
        }
    }

    private void create(final Player p, final Arena arena, final String regionName) {
        this.create(p, arena, regionName, "CUBOID");
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage(StringParser.colorVar("autoload", this.arena.getConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOLOAD)) +
                " | " + StringParser.colorVar("autosave", this.arena.getConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOSAVE)));
    }

    private void load(final ArenaRegion ars) {
        this.load(ars, ars.getArena().getName() + '_' + ars.getRegionName());
    }

    private void load(final ArenaRegion ars, final String regionName) {

        try {
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

            try (ClipboardReader reader = format.getReader(Files.newInputStream(loadFile.toPath()))) {
                Clipboard clipboard = reader.read();
                reader.close();
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                BukkitWorld bukkitWorld = new BukkitWorld(ars.getWorld());

                final EditSession editSession = worldEdit.newEditSession(bukkitWorld);
                editSession.setReorderMode(EditSession.ReorderMode.MULTI_STAGE);
                BlockVector3 to = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());
                Operation operation = holder.createPaste(editSession)
                        .to(to)
                        .ignoreAirBlocks(!this.arena.getConfig().getBoolean(CFG.MODULES_WORLDEDIT_REPLACEAIR))
                        .build();

                Bukkit.getScheduler().runTask(PVPArena.getInstance(), () -> {
                    try {
                        Operations.complete(operation);
                        Bukkit.getScheduler().runTask(PVPArena.getInstance(), editSession::close);
                    } catch (WorldEditException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IllegalArgumentException e) {
            PVPArena.getInstance().getLogger().severe(e.getMessage());
        } catch (final IOException e) {
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
        if (this.arena.getConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOSAVE)) {
            List<String> regions = this.arena.getConfig().getStringList(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), new ArrayList<>());
            if (!regions.isEmpty()) {
                for (String regionName : regions) {
                    ArenaRegion region = this.arena.getRegion(regionName);
                    if (region != null) {
                        this.save(region);
                    }
                }
                return;
            }
            for (final ArenaRegion ars : this.arena.getRegionsByType(RegionType.BATTLE)) {
                this.save(ars);
            }
        }
        this.needsLoading = true;
    }

    @Override
    public void reset(final boolean force) {
        if (this.needsLoading && this.arena.getConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOLOAD)) {
            List<String> regions = this.arena.getConfig().getStringList(CFG.MODULES_WORLDEDIT_REGIONS.getNode(), new ArrayList<>());
            if (!regions.isEmpty()) {
                for (String regionName : regions) {
                    ArenaRegion region = this.arena.getRegion(regionName);
                    if (region != null) {
                        this.load(region);
                    }
                }
                return;
            }
            for (final ArenaRegion ars : this.arena.getRegionsByType(RegionType.BATTLE)) {
                this.load(ars);
            }
        }
        this.needsLoading = false;
    }

    private void save(final ArenaRegion ars) {
        this.save(ars, ars.getArena().getName() + '_' + ars.getRegionName());
    }

    private BlockVector3 getVector(org.bukkit.util.Vector vector) {
        return BlockVector3.at(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    private void save(final ArenaRegion arena, final String regionName) {
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(arena.getWorld());

        Region region = new CuboidRegion(world,
                this.getVector(arena.getShape().getMinimumLocation().toLocation().toVector()),
                this.getVector(arena.getShape().getMaximumLocation().toLocation().toVector()));

        final BlockArrayClipboard clipboard = new BlockArrayClipboard(region);


        final EditSession session = WorldEdit.getInstance().newEditSession(world);

        ForwardExtentCopy extentCopy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
        extentCopy.setCopyingEntities(true);

        try {
            Operations.complete(extentCopy);
            ClipboardFormat format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
            this.createSchematicDirIfNeeded();
            final File schemFile = new File(this.loadPath, regionName + ".schem");
            ClipboardWriter writer = format.getWriter(new FileOutputStream(schemFile));
            writer.write(clipboard);
            writer.close();
            session.close();
        } catch (WorldEditException | IOException e) {
            e.printStackTrace();
        }
    }

    private void createSchematicDirIfNeeded() {
        File file = new File(this.loadPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}
