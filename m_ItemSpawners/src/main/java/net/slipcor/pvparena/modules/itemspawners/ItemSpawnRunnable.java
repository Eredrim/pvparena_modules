package net.slipcor.pvparena.modules.itemspawners;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.RandomUtils;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.trace;

class ItemSpawnRunnable extends BukkitRunnable {
    private final Set<PASpawn> spawnLocations;
    private final Map<String, List<ItemStack>> spawnItemLists;
    private final Arena arena;

    public ItemSpawnRunnable(Arena a, Map<String, List<ItemStack>> spawnItemLists) {
        this.arena = a;
        trace(this.arena, "ItemSpawnRunnable constructor");
        this.spawnLocations = SpawnManager.getPASpawnsStartingWith(this.arena, "item");
        this.spawnItemLists = spawnItemLists;
    }

    @Override
    public void run() {
        trace(this.arena, "ItemSpawnRunnable running");
        Random rdmGenerator = new Random();
        this.spawnLocations.forEach(spawn -> {
            Location loc = spawn.getPALocation().toLocation();
            List<ItemStack> itemStacks = this.spawnItemLists.get(spawn.getName());
            if(itemStacks != null && !itemStacks.isEmpty()) {
                ItemStack randomItem = RandomUtils.getRandom(itemStacks, rdmGenerator);
                loc.getWorld().dropItemNaturally(loc, randomItem);
            }
        });
    }

}
