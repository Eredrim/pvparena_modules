package net.slipcor.pvparena.modules.blockrestore;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.regions.CuboidRegion;
import net.slipcor.pvparena.regions.SphericRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.slipcor.pvparena.config.Debugger.debug;

class RestoreContainer {
    private final Blocks blocks;
    private final ArenaRegion bfRegion;

    private final HashMap<Location, ItemStack[]> chests = new HashMap<>();
    private final HashMap<Location, ItemStack[]> furnaces = new HashMap<>();
    private final HashMap<Location, ItemStack[]> dispensers = new HashMap<>();

    public RestoreContainer(final Blocks b, final ArenaRegion r) {
        blocks = b;
        bfRegion = r;
    }

    void restoreChests() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(),
                new RestoreRunner(blocks, chests, furnaces, dispensers));
    }

    static ItemStack[] cloneIS(final ItemStack[] contents) {
        final ItemStack[] result = new ItemStack[contents.length];

        for (int i = 0; i < result.length; i++) {
            if (contents[i] != null) {
                final ItemStack is = contents[i];
                result[i] = new ItemStack(is);
            }
        }
        debug(result.toString());

        return result;
    }

    public void saveChests() {

        if (!blocks.getArena().getArenaConfig().getStringList("inventories", new ArrayList<String>()).isEmpty()) {

            final List<String> tempList = blocks.getArena().getArenaConfig()
                    .getStringList("inventories", null);

            debug("reading inventories");

            for (final String s : tempList) {
                final Location loc = parseStringToLocation(s);

                saveBlock(loc.getWorld(), loc.getBlockX(), loc.getBlockY(),
                        loc.getBlockZ());
            }

            return;
        }
        debug("NO inventories");

        chests.clear();
        furnaces.clear();
        dispensers.clear();

        final PABlockLocation min = bfRegion.getShape().getMinimumLocation();
        final PABlockLocation max = bfRegion.getShape().getMaximumLocation();

        debug("min: {}", min);
        debug("max: {}", max);

        final World world = Bukkit.getWorld(max.getWorldName());

        final List<String> result = new ArrayList<>();

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
                    }
                }
            }
        }
        blocks.getArena().getArenaConfig().setManually("inventories", result);
        blocks.getArena().getArenaConfig().save();
    }

    private Location saveBlock(final World world, final int x, final int y, final int z) {
        final Block b = world.getBlockAt(x, y, z);
        if (b.getType() == Material.CHEST) {
            final Chest c = (Chest) b.getState();

            chests.put(b.getLocation(), cloneIS(c.getInventory().getContents()));
            return b.getLocation();
        }
        if (b.getType() == Material.FURNACE) {
            Furnace c = (Furnace) b.getState();

            furnaces.put(b.getLocation(), cloneIS(c.getInventory()
                    .getContents()));
            return b.getLocation();
        }
        if (b.getType() == Material.DISPENSER) {
            Dispenser c = (Dispenser) b.getState();

            dispensers.put(b.getLocation(), cloneIS(c.getInventory()
                    .getContents()));
            return b.getLocation();
        }
        return null;
    }

    private Location parseStringToLocation(final String loc) {
        // world,x,y,z
        final String[] args = loc.split(",");

        final World world = Bukkit.getWorld(args[0]);
        final int x = Integer.parseInt(args[1]);
        final int y = Integer.parseInt(args[2]);
        final int z = Integer.parseInt(args[3]);

        return new Location(world, x, y, z);
    }

    private String parseLocationToString(final Location loc) {
        return loc.getWorld().getName() + ',' + loc.getBlockX() + ','
                + loc.getBlockY() + ',' + loc.getBlockZ();
    }
}
