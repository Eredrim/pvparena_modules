package net.slipcor.pvparena.modules.chestfiller;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.loadables.ArenaRegion.RegionType;
import net.slipcor.pvparena.regions.CuboidRegion;
import net.slipcor.pvparena.regions.SphericRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

public class ChestFiller extends ArenaModule {
    public ChestFiller() {
        super("ChestFiller");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!cf".equals(s) || s.startsWith("chestfiller");
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("chestfiller");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!cf");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"chest"});
        result.define(new String[]{"clear"});
        result.define(new String[]{"fillchest"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !cf clear | clear inventory definitions
        // !cf chest | set chestfiller chest
        if (!PVPArena.hasAdminPerms(sender)
                && !PVPArena.hasCreatePerms(sender, arena)) {
            arena.msg(
                    sender,
                    Language.parse(MSG.ERROR_NOPERM,
                            Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{2})) {
            return;
        }

        if ("chest".equals(args[1])) {
            if (!(sender instanceof Player)) {
                Arena.pmsg(sender, Language.parse(arena, MSG.ERROR_ONLY_PLAYERS));
                return;
            }
            Player player = (Player) sender;

            Block b = player.getTargetBlock((Set<Material>)null, 10);
            if (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST) {
                arena.msg(sender,
                        Language.parse(arena, MSG.ERROR_NO_CHEST));
                return;
            }
            PABlockLocation loc = new PABlockLocation(b.getLocation());

            arena.getArenaConfig().set(Config.CFG.MODULES_CHESTFILLER_CHESTLOCATION, loc.toString());
            arena.getArenaConfig().save();
            sender.sendMessage(Language.parse(arena, MSG.MODULE_CHESTFILLER_CHEST, loc.toString()));
            return;
        }

        if ("fillchest".equals(args[1])) {
            if (!(sender instanceof Player)) {
                Arena.pmsg(sender, Language.parse(arena, MSG.ERROR_ONLY_PLAYERS));
                return;
            }
            Player player = (Player) sender;

            Block b = player.getTargetBlock((Set<Material>)null, 10);
            if (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST) {
                arena.msg(sender,
                        Language.parse(arena, MSG.ERROR_NO_CHEST));
                return;
            }
            PABlockLocation loc = new PABlockLocation(b.getLocation());

            List<String> chests = arena.getArenaConfig().getStringList("inventories", new ArrayList<String>());

            chests.add(loc.toString());

            arena.getArenaConfig().setManually("inventories", chests);
            arena.getArenaConfig().save();
            sender.sendMessage(Language.parse(arena, MSG.MODULE_CHESTFILLER_FILLCHEST, loc.toString()));
            return;
        }



        if ("clear".equals(args[1])) {
            arena.getArenaConfig().setManually("inventories", null);
            arena.getArenaConfig().save();

            sender.sendMessage(Language.parse(MSG.MODULE_CHESTFILLER_CLEAR));
        }

    }

    @Override
    public void displayInfo(final CommandSender sender) {
        String content = arena.getArenaConfig().getString(Config.CFG.MODULES_CHESTFILLER_CHESTLOCATION);
        sender.sendMessage("items: " + (content.equals("none")?
                StringParser.getItems(arena.getArenaConfig().getItems(Config.CFG.MODULES_CHESTFILLER_ITEMS)) :
                content));
        sender.sendMessage("max: " + arena.getArenaConfig().getInt(Config.CFG.MODULES_CHESTFILLER_MAXITEMS)
                + " | " +
                "min: " + arena.getArenaConfig().getInt(Config.CFG.MODULES_CHESTFILLER_MINITEMS));

    }

    @Override
    public boolean needsBattleRegion() {
        return true;
    }

    @Override
    public void parseStart() {
        final ItemStack[] items;
        try {
            items = arena.getArenaConfig().getItems(Config.CFG.MODULES_CHESTFILLER_ITEMS);
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        final boolean clear;
        try {
            clear = arena.getArenaConfig().getBoolean(Config.CFG.MODULES_CHESTFILLER_CLEAR);
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        final int cmax = Integer.parseInt(String.valueOf(arena.getArenaConfig().getInt(Config.CFG.MODULES_CHESTFILLER_MAXITEMS)));
        final int cmin = Integer.parseInt(String.valueOf(arena.getArenaConfig().getInt(Config.CFG.MODULES_CHESTFILLER_MINITEMS)));

        String chest = arena.getArenaConfig().getDefinedString(Config.CFG.MODULES_CHESTFILLER_CHESTLOCATION);
        ItemStack[] contents = new ItemStack[0];
        if (chest != null) {
            try {
                PABlockLocation loc = new PABlockLocation(chest);
                Chest c = (Chest) loc.toLocation().getBlock().getState();
                List<ItemStack> list = new ArrayList<>();
                for (ItemStack item : c.getInventory().getContents()) {
                    if (item != null) {
                        list.add(item.clone());
                    }
                }
                contents = list.toArray(contents);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final ItemStack[] stacks = contents.length>0?contents:items;

        if (stacks.length < 1) {
            return;
        }


        // ----------------------------------------

        if (!arena.getArenaConfig().getStringList("inventories", new ArrayList<String>()).isEmpty()) {

            final List<String> tempList = arena.getArenaConfig()
                    .getStringList("inventories", null);

            debug("reading inventories");

            for (final String s : tempList) {
                final Location loc = parseStringToLocation(s);
                if (loc == null) {
                    PVPArena.getInstance().getLogger().warning("Invalid inventory in "+arena.getName()+": "+s);
                    continue;
                }
                fill(loc, clear, cmin, cmax, stacks);
            }

            return;
        }
        debug("NO inventories");

        final List<String> result = new ArrayList<>();

        for (final ArenaRegion bfRegion : arena.getRegionsByType(RegionType.BATTLE)) {
            final PABlockLocation min = bfRegion.getShape().getMinimumLocation();
            final PABlockLocation max = bfRegion.getShape().getMaximumLocation();

            debug("min: {}", min);
            debug("max: {}", max);

            final World world = Bukkit.getWorld(max.getWorldName());


            int z;
            int y;
            int x;
            if (bfRegion.getShape() instanceof CuboidRegion) {
                debug("cube!");

                for (x = min.getX(); x <= max.getX(); x++) {
                    for (y = min.getY(); y <= max.getY(); y++) {
                        for (z = min.getZ(); z <= max.getZ(); z++) {
                            final Location loc = saveBlock(world, x, y, z);
                            if (loc == null) {
                                continue;
                            }
                            debug("loc not null: {}", loc);
                            result.add(parseLocationToString(loc));
                            fill(loc, clear, cmin, cmax, stacks);
                        }
                    }
                }
            } else if (bfRegion.getShape() instanceof SphericRegion) {
                debug("sphere!");
                for (x = min.getX(); x <= max.getX(); x++) {
                    for (y = min.getY(); y <= max.getY(); y++) {
                        for (z = min.getZ(); z <= max.getZ(); z++) {
                            final Location loc = saveBlock(world, x, y, z);
                            if (loc == null) {
                                continue;
                            }
                            debug("loc not null: {}", loc);
                            result.add(parseLocationToString(loc));
                            fill(loc, clear, cmin, cmax, stacks);
                        }
                    }
                }
            }
        }


        arena.getArenaConfig().setManually("inventories", result);
        arena.getArenaConfig().save();

        // ----------------------------------------

    }

    private Location parseStringToLocation(final String loc) {
        // world:x,y,z
        final String[] first = loc.split(":");

        final String[] args = first[1].split(",");

        if (args.length < 3) {
            return null;
        }

        final World world = Bukkit.getWorld(first[0]);
        final int x = Integer.parseInt(args[0]);
        final int y = Integer.parseInt(args[1]);
        final int z = Integer.parseInt(args[2]);

        return new Location(world, x, y, z);
    }

    private void fill(final Location loc, final boolean clear, final int min, final int max, final ItemStack[] stacks) {
        final Chest c;

        try {
            c = (Chest) loc.getBlock().getState();
        } catch (final ClassCastException cce) {
            return;
        }

        if (clear) {
            c.getBlockInventory().clear();
        }

        final List<ItemStack> adding = new ArrayList<>();

        final Random r = new Random();

        int bound = max-min<0?0:max-min;

        // if min == max or min > max, use max
        final int count = bound == 0 ? max :r.nextInt(bound) + min;


        int i = 0;

        while (i++ < count) {
            final int d = r.nextInt(stacks.length);
            if (stacks[d] == null) {
                continue;
            }
            adding.add(stacks[d].clone());
        }

        for (final ItemStack it : adding) {
            c.getInventory().addItem(it);
        }
    }

    private Location saveBlock(final World world, final int x, final int y, final int z) {
        final Block b = world.getBlockAt(x, y, z);
        if (b.getType() == Material.CHEST) {
            return b.getLocation();
        }
        return null;
    }

    private String parseLocationToString(final Location loc) {
        return loc.getWorld().getName() + ':' + loc.getBlockX() + ','
                + loc.getBlockY() + ',' + loc.getBlockZ();
    }
}
