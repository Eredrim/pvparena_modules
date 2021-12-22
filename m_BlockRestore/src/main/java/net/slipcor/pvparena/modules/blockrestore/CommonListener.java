package net.slipcor.pvparena.modules.blockrestore;

import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.util.BlockIterator;

public class CommonListener implements Listener {
    private final BlockRestore module;

    public CommonListener(BlockRestore module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void projectileHit(final ProjectileHitEvent event) {
        for (final ArenaRegion shape : this.module.getArena().getRegionsByType(RegionType.BATTLE)) {
            if (shape.getShape().contains(new PABlockLocation(event.getEntity().getLocation()))) {
                if (event.getEntityType() == EntityType.ARROW) {
                    final Arrow arrow = (Arrow) event.getEntity();
                    if (arrow.getFireTicks() > 0) {
                        final BlockIterator bi = new BlockIterator(arrow.getWorld(), arrow.getLocation().toVector(), arrow.getVelocity(), 0, 2);
                        while (bi.hasNext()) {
                            final Block block = bi.next();
                            if (block.getType() == Material.TNT) {
                                this.module.saveBlock(block);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        if (event.getBucket() != Material.LAVA_BUCKET && event.getBucket() != Material.WATER_BUCKET) {
            return;
        }

        Block toCheck = event.getBlockClicked();
        if(!(toCheck.getBlockData() instanceof Waterlogged)) {
            toCheck = event.getBlockClicked().getRelative(event.getBlockFace());
        }
        this.module.saveBlockIfInBattleground(toCheck);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBucketFill(final PlayerBucketFillEvent event) {
        if (event.getBucket() != Material.BUCKET || event.getItemStack() == null ||
                event.getItemStack().getType() == Material.MILK_BUCKET) {
            return;
        }
        this.module.saveBlockIfInBattleground(event.getBlockClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockFromTo(BlockFromToEvent event) {
        Block block = event.getToBlock();
        if (block.getType() == Material.WATER || block.getType() == Material.LAVA) {
            this.module.saveBlockIfInBattleground(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockFall(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof FallingBlock) {
            this.module.saveBlockIfInBattleground(event.getBlock());
        }
    }
}
