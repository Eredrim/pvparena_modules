package net.slipcor.pvparena.modules.blockrestore;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Stack;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;

class RestoreContainerRunnable extends BukkitRunnable {
    private final Stack<ArenaContainer> containers;
    private final BlockRestore module;

    public RestoreContainerRunnable(BlockRestore module, Stack<ArenaContainer> containers) {
        debug("RestoreRunner constructor: {}", module.getArena());

        this.module = module;
        this.containers = containers;

        if (containers != null) {
            debug("containers: {}", containers.size());
        }
    }

    @Override
    public void run() {
        if(!this.module.isRestoringBlocks()) {
            if(this.containers.isEmpty()) {
                this.module.finishContainerRestore();
                this.cancel();
            } else {
                int i = 0;
                final World world = this.module.getArena().getWorld();
                while (!this.containers.isEmpty() && i < 10) {
                    ArenaContainer container = this.containers.pop();
                    try {
                        this.refillContainer(world, container);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    } finally {
                        i++;
                    }
                }
            }
        }
    }

    public void runOnce() {
        final World world = this.module.getArena().getWorld();
        while (!this.containers.isEmpty()) {
            ArenaContainer container = this.containers.pop();
            try {
                this.refillContainer(world, container);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        this.module.finishContainerRestore();
    }

    private void refillContainer(World world, ArenaContainer container) {
        final Block block = world.getBlockAt(container.getLocation());
        trace("trying to restore container {}: {}", block.getType(), container.getLocation());
        if(block.getState() instanceof Container) {
            final Inventory blockInventory = ((Container) block.getState()).getInventory();
            blockInventory.setContents(container.getContent());
            trace("success!");
        }
    }
}
