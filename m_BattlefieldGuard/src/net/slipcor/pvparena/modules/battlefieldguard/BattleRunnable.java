package net.slipcor.pvparena.modules.battlefieldguard;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import static net.slipcor.pvparena.config.Debugger.debug;
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
        try {
            for (final Player p : Bukkit.getServer().getOnlinePlayers()) {
                final ArenaPlayer ap = ArenaPlayer.parsePlayer(p.getName());

                final Arena arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(p.getLocation()));

                if (arena == null) {
                    continue; // not physically in an arena
                }

                if (PVPArena.hasAdminPerms(p)) {
                    continue;
                }

                debug(p, "arena pos: {}", arena);
                debug(p, "arena IN: {}", ap.getArena());

                if(ap.getArena() == null) {
                    if (arena.getArenaConfig().getBoolean(CFG.MODULES_BATTLEFIELDGUARD_ENTERDEATH)) {
                        p.setLastDamageCause(new EntityDamageEvent(p, DamageCause.CUSTOM,1000.0));
                        p.setHealth(0);
                        p.damage(1000);
                    } else {
                        arena.tpPlayerToCoordName(ap, "exit");
                    }
                } else if(!ap.getArena().equals(arena)) {
                    if (ap.getArena().getArenaConfig().getBoolean(CFG.MODULES_BATTLEFIELDGUARD_ENTERDEATH)) {
                        p.setLastDamageCause(new EntityDamageEvent(p, DamageCause.CUSTOM,1000.0));
                        p.setHealth(0);
                        p.damage(1000);
                    } else {
                        ap.getArena().playerLeave(p, CFG.TP_EXIT, false, false, false);
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
