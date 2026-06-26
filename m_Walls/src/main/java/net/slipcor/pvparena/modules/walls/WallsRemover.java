package net.slipcor.pvparena.modules.walls;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Deque;

public class WallsRemover extends BukkitRunnable {

    private final Walls module;
    private final Deque<Block> wallBlocks;

    public WallsRemover(Walls module, Deque<Block> wallBlocks) {
        this.module = module;
        this.wallBlocks = wallBlocks;
    }

    @Override
    public void run() {
        if (this.wallBlocks.isEmpty()) {
            this.cancel();
            this.module.finalizeWallsRemoval();
        } else {
            int i = 0;
            while (!this.wallBlocks.isEmpty() && i < 100) {
                Block block = this.wallBlocks.poll();
                block.setType(Material.AIR);
                i += 1;
            }
        }
    }
}
