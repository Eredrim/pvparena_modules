package net.slipcor.pvparena.modules.walls;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaRegionShape;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;
import static net.slipcor.pvparena.core.StringUtils.startsWithIgnoreCase;

public class Walls extends ArenaModule {
    private final Deque<PABlockLocation> wallBlockLocations = new ArrayDeque<>();
    private WallsTimer wallsTimer;
    private WallsBuilder wallsBuilder;
    private WallsRemover wallsRemover;
    private Deque<Block> placedWallBlocks = new ConcurrentLinkedDeque<>();

    public Walls() {
        super("Walls");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void parseJoin(Player player, ArenaTeam team) {
        if (this.wallBlockLocations.isEmpty()) {
            this.loadWallBlockLocations();
        }
    }

    private void loadWallBlockLocations() {
        long startTimestamp = System.currentTimeMillis();
        trace(this.arena, this, "Start loading walls block locations");
        this.arena.getRegions().forEach(region -> {
            if (startsWithIgnoreCase(region.getRegionName(), "wall")) {
                ArenaRegionShape shape = region.getShape();
                shape.getAllBlocksOrdered().forEach(this.wallBlockLocations::push);
            }
        });
        long duration = System.currentTimeMillis() - startTimestamp;
        trace(this.arena, this, "End loading walls block locations - Duration: {}ms - Size: {} blocks", duration, this.wallBlockLocations.size());
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("seconds: %d, material: %s".formatted(
                this.arena.getConfig().getInt(CFG.MODULES_WALLS_SECONDS),
                this.arena.getConfig().getString(CFG.MODULES_WALLS_MATERIAL))
        );
    }

    @Override
    public void parseStart() {
        if (this.wallsTimer == null && this.wallsBuilder == null) {
            this.wallsTimer = new WallsTimer(this, this.arena, this.arena.getConfig().getInt(CFG.MODULES_WALLS_SECONDS));
            debug(this.arena, this, "WallTimer START");

            this.wallsBuilder = new WallsBuilder(this, this.getWallMaterial(), this.wallBlockLocations);
            this.wallsBuilder.runTaskTimer(PVPArena.getInstance(), 0, 1);
            debug(this.arena, this, "WallBuilder START");
        }
    }

    @Override
    public void reset(final boolean force) {
        debug(this.arena, this, "call reset(), force mode: {}", force);
        boolean needsReset = false;
        this.wallBlockLocations.clear();

        if (this.wallsBuilder != null) {
            if (!this.wallsBuilder.isCancelled()) {
                this.wallsBuilder.cancel();
                needsReset = true;
            }
            this.wallsBuilder = null;
        }

        if (this.wallsTimer != null) {
            if (!this.wallsTimer.isCancelled()) {
                this.wallsTimer.cancel();
                needsReset = true;
            }
            this.wallsTimer = null;
        }

        if (needsReset) {
            if (force) {
                if (this.wallsRemover != null && !this.wallsRemover.isCancelled()) {
                    this.wallsTimer.cancel();
                }
                this.forceRemoveWalls();
            } else {
                if (this.wallsRemover == null) {
                    this.wallsRemover = new WallsRemover(this, this.placedWallBlocks);
                    this.wallsRemover.runTaskTimer(PVPArena.getInstance(), 0, 1);
                    debug(this.arena, this, "WallRemover START (during reset)");
                    this.arena.setResetting(true);
                }
                // Else - removal is already in progress
            }
        }
    }

    void setPlacedWallBlocks(ConcurrentLinkedDeque<Block> blocks) {
        debug(this.arena, this, "WallBuilder END");
        this.placedWallBlocks = blocks;
    }

    void removeWallsAsync() {
        debug(this.arena, this, "WallTimer END");
        this.wallsTimer = null;
        if (this.wallsBuilder != null && !this.wallsBuilder.isCancelled()) {
            this.wallsBuilder.cancel();
        }
        this.wallsBuilder = null;
        this.wallsRemover = new WallsRemover(this, this.placedWallBlocks);
        this.wallsRemover.runTaskTimer(PVPArena.getInstance(), 0, 1);
        debug(this.arena, this, "WallRemover START");
    }

    void finalizeWallsRemoval() {
        debug(this.arena, this, "WallRemover END");
        this.wallsRemover = null;
        this.arena.setResetting(false);
        this.placedWallBlocks.clear();
    }

    private void forceRemoveWalls() {
        debug(this.arena, this, "Running Force Removal");
        while (!this.placedWallBlocks.isEmpty()) {
            Block block = this.placedWallBlocks.poll();
            block.setType(Material.AIR);
        }
    }

    private Material getWallMaterial() {
        try {
            return this.arena.getConfig().getMaterial(CFG.MODULES_WALLS_MATERIAL, Material.SAND);
        } catch (final Exception e) {
            return Material.SAND;
        }
    }
}
