package net.slipcor.pvparena.modules.blockrestore;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PaperListener implements Listener {
    private final BlockRestore module;

    public PaperListener(BlockRestore module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockDestroyPaper(BlockDestroyEvent event) {
        this.module.saveBlockIfInBattleground(event.getBlock());
    }
}
