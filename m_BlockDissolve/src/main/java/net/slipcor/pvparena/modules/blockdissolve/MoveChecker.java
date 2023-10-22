package net.slipcor.pvparena.modules.blockdissolve;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
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
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static net.slipcor.pvparena.config.Debugger.debug;

class MoveChecker implements Listener {
    private final List<Material> checkMaterialList;
    private final Arena arena;
    private final ConcurrentNavigableMap<Long, Set<Block>> map = new ConcurrentSkipListMap<>();
    private final int delay;
    private final int startSeconds;
    private final double offset;
    private boolean active;
    private BukkitTask cleanTask;

    public MoveChecker(final Arena arena) {
        debug("BlockDissolve MoveChecker constructor");
        this.arena = arena;
        this.delay = this.arena.getConfig().getInt(CFG.MODULES_BLOCKDISSOLVE_TICKS);
        this.checkMaterialList = Arrays.stream(this.arena.getConfig().getItems(CFG.MODULES_BLOCKDISSOLVE_MATERIALS))
                .map(ItemStack::getType)
                .collect(Collectors.toList());
        this.startSeconds = arena.getConfig().getInt(CFG.MODULES_BLOCKDISSOLVE_STARTSECONDS);
        this.offset = arena.getConfig().getDouble(CFG.MODULES_BLOCKDISSOLVE_CALCOFFSET);
    }

    public void startChecker() {
        this.active = true;

        // remove block under all player feet on startup if they don't move
        this.arena.getFighters().stream()
                .filter(ap -> ap.getStatus() == PlayerStatus.FIGHT)
                .forEach(ap -> this.checkBlock(ap.getPlayer().getLocation().clone().subtract(0, 1, 0)));

        // runs a task to remove blocks and clean map each tick
        this.cleanTask = Bukkit.getScheduler().runTaskTimer(PVPArena.getInstance(), () -> {
            long now = System.currentTimeMillis();
            while(!this.map.isEmpty() && (this.map.firstKey() + (this.delay * 50L)) <= now) {
                this.map.pollFirstEntry().getValue().forEach(block -> {
                    ArenaModuleManager.onBlockBreak(this.arena, block);
                    block.setType(Material.AIR);
                });
            }
        }, 1, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {

        if (this.active) {
            ArenaPlayer player = ArenaPlayer.fromPlayer(event.getPlayer());

            if (this.arena == player.getArena() && player.getStatus() == PlayerStatus.FIGHT) {
                this.checkBlock(event.getPlayer().getLocation().clone().subtract(0, 1, 0));
            }
        }
    }

    private void checkBlock(final Location location) {

        final double x = Math.abs(location.getX() * 10 % 10 / 10);
        final double z = Math.abs(location.getZ() * 10 % 10 / 10);

        if (x < this.offset) {
            this.tagBlockToRemove(location.clone().add(location.getX()<0?1:-1, 0, 0).getBlock());
        } else if (x > (1- this.offset)) {
            this.tagBlockToRemove(location.clone().add(location.getX()<0?-1:1, 0, 0).getBlock());
        }

        if (z < this.offset) {
            this.tagBlockToRemove(location.clone().add(0, 0, location.getZ()<0?1:-1).getBlock());
        } else if (z > (1- this.offset)) {
            this.tagBlockToRemove(location.clone().add(0, 0, location.getZ()<0?-1:1).getBlock());
        }

        if (x < this.offset && z < this.offset) {
            this.tagBlockToRemove(location.clone().add(location.getX()<0?1:-1, 0, location.getZ()<0?1:-1).getBlock());
        } else if (x < this.offset && z > (1- this.offset)) {
            this.tagBlockToRemove(location.clone().add(location.getX()<0?1:-1, 0, location.getZ()<0?-1:1).getBlock());
        } else if (x > (1- this.offset) && z < this.offset) {
            this.tagBlockToRemove(location.clone().add(location.getX()<0?-1:1, 0, location.getZ()<0?1:-1).getBlock());
        } else if (x > (1- this.offset) && z > (1- this.offset)) {
            this.tagBlockToRemove(location.clone().add(location.getX()<0?-1:1, 0, location.getZ()<0?-1:1).getBlock());
        }

        this.tagBlockToRemove(location.getBlock());
    }

    private void tagBlockToRemove(final Block block) {
        if(this.checkMaterialList.contains(block.getType())) {
            long currentTime = System.currentTimeMillis();
            if(!this.map.isEmpty() && currentTime < this.map.lastKey() + 50) {
                this.map.lastEntry().getValue().add(block);
            } else {
                this.map.put(currentTime, Stream.of(block).collect(toSet()));
            }
        }
    }

    public void clear() {
        this.cleanTask.cancel();
        this.map.clear();
        this.active = false;
    }

    public void startCountdown() {
        new CountdownRunner(this.arena, this, this.startSeconds);
    }
}
