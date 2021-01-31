package net.slipcor.pvparena.modules.blockrestore;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.core.Config.CFG;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;

class RestoreRunner implements Runnable {
    private final Map<Location, ItemStack[]> chests;
    private final HashMap<Location, ItemStack[]> furnaces;
    private final Map<Location, ItemStack[]> dispensers;
    private final Blocks blocks;

    public RestoreRunner(final Blocks blocks, final HashMap<Location, ItemStack[]> chests,
                         final HashMap<Location, ItemStack[]> furnaces,
                         final HashMap<Location, ItemStack[]> dispensers) {
        debug("RestoreRunner constructor: {}", blocks.getArena());

        this.blocks = blocks;
        this.chests = chests;
        this.furnaces = furnaces;
        this.dispensers = dispensers;

        if (chests != null) {
            debug("chests: {}", chests.size());
        }
        if (furnaces != null) {
            debug("furnaces: {}", furnaces.size());
        }
        if (dispensers != null) {
            debug("dispensers: {}", dispensers.size());
        }
    }

    @Override
    public void run() {
        PAA_Edit.activeEdits.put("server", blocks.getArena());
        final World world = Bukkit.getWorld(blocks.getArena().getWorld());
        for (final Map.Entry<Location, ItemStack[]> locationEntry1 : chests.entrySet()) {
            if (locationEntry1.getKey() == null) {
                break;
            }
            try {
                trace("trying to restore chest: {}", locationEntry1.getKey());
                final Block b = world.getBlockAt(locationEntry1.getKey());
                if(b.getType() == Material.CHEST) {
                    final Inventory inv = ((Chest) b.getState()).getInventory();
                    inv.clear();
                    int i = 0;
                    for (final ItemStack is : locationEntry1.getValue()) {
                        trace("restoring: {}", is);
                        inv.setItem(i++, is);
                    }
                    trace("success!");
                    chests.remove(locationEntry1.getKey());
                    Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(),
                            this, blocks.getArena().getArenaConfig().getInt(CFG.MODULES_BLOCKRESTORE_OFFSET) * 1L);
                }
                return;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        for (final Map.Entry<Location, ItemStack[]> locationEntry : dispensers.entrySet()) {
            if (locationEntry.getKey() == null) {
                break;
            }
            try {
                trace("trying to restore dispenser: {}", locationEntry.getKey());

                final Inventory inv = ((Dispenser) world.getBlockAt(locationEntry.getKey()).getState())
                        .getInventory();
                inv.clear();
                for (final ItemStack is : locationEntry.getValue()) {
                    if (is != null) {
                        inv.addItem(is.clone());
                    }
                }
                trace("success!");
                dispensers.remove(locationEntry.getKey());
                Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(),
                        this, blocks.getArena().getArenaConfig().getInt(CFG.MODULES_BLOCKRESTORE_OFFSET) * 1L);
                return;
            } catch (final Exception e) {
                //
            }
        }
        for (final Location loc : furnaces.keySet()) {
            if (loc == null) {
                break;
            }
            try {
                trace("trying to restore furnace: {}", loc);
                ((Furnace) world.getBlockAt(loc).getState()).getInventory()
                        .setContents(RestoreContainer.cloneIS(furnaces.get(loc)));
                trace("success!");
                furnaces.remove(loc);
                Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(),
                        this, blocks.getArena().getArenaConfig().getInt(CFG.MODULES_BLOCKRESTORE_OFFSET) * 1L);
                return;
            } catch (final Exception e) {
                //
            }
        }
        PAA_Edit.activeEdits.remove("server");
    }

}
