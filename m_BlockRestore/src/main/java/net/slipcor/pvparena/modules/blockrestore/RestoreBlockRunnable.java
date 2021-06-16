package net.slipcor.pvparena.modules.blockrestore;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class RestoreBlockRunnable extends BukkitRunnable {
    private final TreeMap<Long, Set<ArenaBlock>> removals;
    private final BlockRestore module;

    public RestoreBlockRunnable(final BlockRestore module, TreeMap<Long, Set<ArenaBlock>> removals) {
        this.module = module;
        this.removals = removals;
    }

    @Override
    public void run() {
        if(this.removals.isEmpty()) {
            this.module.finishBlockRestore();
            this.cancel();
        } else {
            int i = 0;
            while (!this.removals.isEmpty() && i < 10) {
                Map.Entry<Long, Set<ArenaBlock>> lastEntry = this.removals.pollLastEntry();
                lastEntry.getValue().forEach(ArenaBlock::reset);
                i += lastEntry.getValue().size();
            }
        }
    }
}
