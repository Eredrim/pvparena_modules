package net.slipcor.pvparena.modules.blockrestore;

import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;

public class RestorationListener implements Listener {
    private final BlockRestore module;

    public RestorationListener(BlockRestore module) {
        this.module = module;
    }

    /**
     * This event handler is used to prevent fire propagation during block restoration
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockIgnite(BlockIgniteEvent event) {
        if(this.module.isRestoringBlocks()) {
            for (final ArenaRegion shape : this.module.getArena().getRegionsByType(RegionType.BATTLE)) {
                if (shape.getShape().contains(new PABlockLocation(event.getBlock().getLocation()))) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
