package net.slipcor.pvparena.modules.eventactions;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlockLocation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

class EADelay implements Runnable {
    private final Block block;

    public EADelay(final PABlockLocation loc2) {
        this.block = loc2.toLocation().getBlock();
    }

    @Override
    public void run() {
        final Material type = this.block.getType();
        final BlockData data = this.block.getBlockData();

        this.block.setType(Material.REDSTONE_BLOCK);

        class OffRunner implements Runnable {

            @Override
            public void run() {
                EADelay.this.block.setType(type);
                EADelay.this.block.setBlockData(data);
            }

        }

        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new OffRunner(), 12L);
    }

}
