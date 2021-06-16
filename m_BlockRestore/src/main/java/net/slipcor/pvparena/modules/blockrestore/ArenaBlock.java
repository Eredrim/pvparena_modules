package net.slipcor.pvparena.modules.blockrestore;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlockLocation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;

class ArenaBlock {
    private final PABlockLocation location;
    private final BlockData blockData;
    private final String[] lines;

    /**
     * create an arena block instance (blockdestroy)
     *
     * @param block the block to copy
     */
    public ArenaBlock(final Block block) {
        this.location = new PABlockLocation(block.getLocation());
        this.blockData = block.getBlockData().clone();

        debug("creating arena block:");
        debug("loc: {} ; mat: {}", this.location, this.blockData.getMaterial());

        if (block.getState() instanceof Sign) {
            this.lines = ((Sign) block.getState()).getLines();
        } else {
            this.lines = null;
        }
    }

    /**
     * create an arena block instance (blockplace)
     *
     * @param block the block to copy
     * @param type  the Material to override (the Material before placing)
     */
    public ArenaBlock(final Block block, final Material type) {
        this.location = new PABlockLocation(block.getLocation());
        this.blockData = Bukkit.createBlockData(type);
        this.lines = null;

        debug("creating arena block:");
        debug("loc: {} ; mat: {}", this.location, this.blockData.getMaterial());

    }

    /**
     * reset an arena block
     */
    public void reset() {
        final Block b = this.location.toLocation().getBlock();
        this.resetBlock(b);
    }

    /**
     * Reset an arena block only if the material at the coordinates is the same than the one stored in blockData
     */
    public void resetIfPresent() {
        final Block b = this.location.toLocation().getBlock();

        if(b.getType() == this.blockData.getMaterial()) {
            this.resetBlock(b);
        }

    }

    private void resetBlock(Block block) {
        block.setBlockData(this.blockData);

        if (this.lines != null) {
            for (int i = 0; i < this.lines.length; i++) {
                try {
                    ((Sign) block.getState()).setLine(i, ofNullable(this.lines[i]).orElse(""));
                } catch (Exception e) {
                    PVPArena.getInstance().getLogger().warning(
                            String.format("tried to reset sign at location %s", this.location));
                }
            }
        }
    }
}
