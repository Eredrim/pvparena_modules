package net.slipcor.pvparena.modules.blockrestore;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Iterator;

class RestoreInteractionsRunnable extends BukkitRunnable {
    private final Collection<ArenaBlock> removals;
    private final Iterator<ArenaBlock> iterator;
    private final BlockRestore module;

    public RestoreInteractionsRunnable(final BlockRestore module, Collection<ArenaBlock> removals) {
        this.module = module;
        this.removals = removals;
        this.iterator = removals.iterator();
    }

    @Override
    public void run() {
        if(!this.module.isRestoringBlocks() && !this.module.isRestoringContainers()) {
            if(this.removals.isEmpty()) {
                this.module.finishInteractionRestore();
                this.cancel();
            } else {
                int i = 0;
                while (this.iterator.hasNext() && i < 10) {
                    this.iterator.next().resetIfPresent();
                    this.iterator.remove();
                    i++;
                }
            }
        }
    }
}
