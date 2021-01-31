package net.slipcor.pvparena.modules.blockrestore;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;

class BlockRestoreRunnable extends BukkitRunnable {
    private final Map<Location, ArenaBlock> removals;
    private final Blocks module;

    public BlockRestoreRunnable(final Blocks module, Map<Location, ArenaBlock> removals) {
        this.module = module;
        this.removals = removals;
    }

    @Override
    public void run() {
        if(this.removals.isEmpty()) {
            this.module.endRestoring();
            this.cancel();
        } else {
            Map.Entry<Location, ArenaBlock> locationArenaBlockEntry = this.removals.entrySet().iterator().next();
            debug("location: {}", locationArenaBlockEntry.getKey());
            locationArenaBlockEntry.getValue().reset();
            this.module.removeBlock(locationArenaBlockEntry.getKey());
            this.removals.remove(locationArenaBlockEntry.getKey());
        }
    }
}
