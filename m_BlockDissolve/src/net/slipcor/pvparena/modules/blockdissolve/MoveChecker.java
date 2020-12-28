package net.slipcor.pvparena.modules.blockdissolve;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

class MoveChecker implements Listener {
    private final List<Material> checkMaterialList;
    private final Arena arena;
    private final Map<Block, Runnable> map = new HashMap<>();
    private final int delay;
    private final int startSeconds;
    boolean active;
    double offset;

    public MoveChecker(final Arena arena, final ItemStack[] items, final int delay) {
        this.checkMaterialList = Arrays.stream(items).map(ItemStack::getType).collect(Collectors.toList());

        debug("BlockDissolve MoveChecker constructor");
        this.arena = arena;
        Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
        this.delay = delay;
        this.startSeconds = arena.getArenaConfig().getInt(CFG.MODULES_BLOCKDISSOLVE_STARTSECONDS);
        this.offset = arena.getArenaConfig().getDouble(CFG.MODULES_BLOCKDISSOLVE_CALCOFFSET);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {

        if (this.active && this.arena.isFightInProgress()) {
            final ArenaPlayer player = ArenaPlayer.parsePlayer(event.getPlayer().getName());
            if (this.arena != player.getArena()) {
                return;
            }

            if (this.arena.getPlayedSeconds() > this.startSeconds && player.getStatus() == Status.FIGHT) {


                this.checkBlock(event.getPlayer().getLocation().clone().subtract(0, 1, 0));
            }
        }
    }

    private void checkBlock(final Location location) {

        final double x = Math.abs(location.getX() * 10 % 10 / 10);
        final double z = Math.abs(location.getZ() * 10 % 10 / 10);

        if (x < this.offset) {
            this.checkBlock(location.clone().add(location.getX()<0?1:-1, 0, 0).getBlock());
        } else if (x > (1- this.offset)) {
            this.checkBlock(location.clone().add(location.getX()<0?-1:1, 0, 0).getBlock());
        }

        if (z < this.offset) {
            this.checkBlock(location.clone().add(0, 0, location.getZ()<0?1:-1).getBlock());
        } else if (z > 0.666) {
            this.checkBlock(location.clone().add(0, 0, location.getZ()<0?-1:1).getBlock());
        }

        if (x < this.offset && z < this.offset) {
            this.checkBlock(location.clone().add(location.getX()<0?1:-1, 0, location.getZ()<0?1:-1).getBlock());
        } else if (x < this.offset && z > (1- this.offset)) {
            this.checkBlock(location.clone().add(location.getX()<0?1:-1, 0, location.getZ()<0?-1:1).getBlock());
        } else if (x > (1- this.offset) && z < this.offset) {
            this.checkBlock(location.clone().add(location.getX()<0?-1:1, 0, location.getZ()<0?1:-1).getBlock());
        } else if (x > (1- this.offset) && z > (1- this.offset)) {
            this.checkBlock(location.clone().add(location.getX()<0?-1:1, 0, location.getZ()<0?-1:1).getBlock());
        }

        this.checkBlock(location.getBlock());
    }

    private void checkBlock(final Block block) {
        if(this.checkMaterialList.contains(block.getType())) {
            this.access(block, false);
        }
    }

    private synchronized void access(final Block block, final boolean remove) {
        if (block == null && remove) {
            this.map.clear();
            this.active = false;
            return;
        }

        if (this.map.containsKey(block)) {
            return;
        }
        if (remove) {
            this.map.remove(block);
        } else {
            this.map.put(block, new RunLater(block));
        }
    }

    public void startTask() {
        class RunLater2 implements Runnable {
            @Override
            public void run() {
                if (MoveChecker.this.active) {
                    for (ArenaPlayer ap : MoveChecker.this.arena.getFighters()) {
                        if (ap.getStatus() == Status.FIGHT) {
                            MoveChecker.this.checkBlock(ap.get().getLocation().clone().subtract(0, 1, 0));
                        }
                    }
                }
            }

        }

        Bukkit.getScheduler().runTaskTimer(PVPArena.getInstance(), new RunLater2(), 20L, 20L);
    }

    class RunLater implements Runnable {
        final Block block;

        RunLater(final Block b) {
            this.block = b;
            ArenaModuleManager.onBlockBreak(MoveChecker.this.arena, b);
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), this, MoveChecker.this.delay);
        }

        @Override
        public void run() {
            MoveChecker.this.access(this.block, true);
            this.block.setType(Material.AIR);
        }

    }

    public void clear() {
        this.access(null, true);
    }

    public void start() {
        new CountdownRunner(this.arena, this, this.startSeconds);
    }
}
