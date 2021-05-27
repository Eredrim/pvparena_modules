package net.slipcor.pvparena.modules.battlefieldguard;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import static net.slipcor.pvparena.config.Debugger.trace;

class BattleRunnable extends BukkitRunnable {
    /**
     * construct a battle runnable
     */
    public BattleRunnable() {
        trace("BattleRunnable constructor");
    }

    /**
     * the run method, spawn a powerup
     */
    @Override
    public void run() {
        trace("BattleRunnable commiting");
        for (final Player player : Bukkit.getServer().getOnlinePlayers()) {
            final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

            final Arena arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(player.getLocation()));

            if (arena == null) {
                continue; // not physically in an arena
            }

            if (PVPArena.hasAdminPerms(player)) {
                continue;
            }

            trace(player, "arena pos: {}", arena);
            trace(player, "arena IN: {}", arenaPlayer.getArena());

            if (arenaPlayer.getArena() == null) {
                if (arena.getConfig().getBoolean(CFG.MODULES_BATTLEFIELDGUARD_ENTERDEATH)) {
                    player.setLastDamageCause(new EntityDamageEvent(player, DamageCause.CUSTOM, 1000.0));
                    player.setHealth(0);
                    player.damage(1000);
                } else {
                    TeleportManager.teleportPlayerToRandomSpawn(arena, arenaPlayer,
                            SpawnManager.getPASpawnsStartingWith(arena, PASpawn.EXIT));
                }
            } else if (!arenaPlayer.getArena().equals(arena)) {
                if (arenaPlayer.getArena().getConfig().getBoolean(CFG.MODULES_BATTLEFIELDGUARD_ENTERDEATH)) {
                    player.setLastDamageCause(new EntityDamageEvent(player, DamageCause.CUSTOM, 1000.0));
                    player.setHealth(0);
                    player.damage(1000);
                } else {
                    arenaPlayer.getArena().playerLeave(player, CFG.TP_EXIT, false, false, false);
                }
            }
        }
    }
}
