package net.slipcor.pvparena.modules.walls;

import net.slipcor.pvparena.classes.PABlockLocation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WallsBuilder extends BukkitRunnable {

    private final Walls module;
    private final Material wallType;
    private final Deque<PABlockLocation> wallBlockLocations;
    private final ConcurrentLinkedDeque<Block> placedWallBlocks = new ConcurrentLinkedDeque<>();

    public WallsBuilder(final Walls wallModule, Material wallType, Deque<PABlockLocation> wallBlockLocations) {
        this.module = wallModule;
        this.wallType = wallType;
        this.wallBlockLocations = wallBlockLocations;
    }

    @Override
    public void run() {
        if (this.wallBlockLocations.isEmpty()) {
            this.cancel();
            this.module.setPlacedWallBlocks(this.placedWallBlocks);
        } else {
            int i = 0;
            World world = Bukkit.getWorld(this.wallBlockLocations.peek().getWorldName());
            while (!this.wallBlockLocations.isEmpty() && i < 100) {
                PABlockLocation blockLocation = this.wallBlockLocations.poll();
                Block block = world.getBlockAt(blockLocation.toLocation());
                if (block.getType().isAir()) {
                    block.setType(this.wallType);
                    this.placedWallBlocks.push(block);
                }
                i += 1;
            }
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        this.module.setPlacedWallBlocks(this.placedWallBlocks);
    }
}
