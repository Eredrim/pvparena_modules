package net.slipcor.pvparena.modules.eventactions;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.*;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import static net.slipcor.pvparena.managers.SpawnManager.ROOT_SPAWNS_NODE;

class PAListener implements Listener {
    private final EventActions ea;

    public PAListener(final EventActions ea) {
        this.ea = ea;
    }

    @EventHandler
    public void onDeath(final PADeathEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        ea.catchEvent("death", p, a);
    }

    @EventHandler
    public void onEnd(final PAEndEvent event) {
        final Arena a = event.getArena();
        ea.catchEvent("end", null, a);
    }

    @EventHandler
    public void onExit(final PAExitEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        ea.catchEvent("exit", p, a);
    }

    @EventHandler
    public void onJoin(final PAJoinEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();

        if (event.isSpectator()) {
            ea.catchEvent("spectate", p, a);
        } else {
            ea.catchEvent("join", p, a);
        }
    }

    @EventHandler
    public void onKill(final PAKillEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        ea.catchEvent("kill", p, a);
    }

    @EventHandler
    public void onLeave(final PALeaveEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        ea.catchEvent("leave", p, a);
    }

    @EventHandler
    public void onLose(final PALoseEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        ea.catchEvent("lose", p, a);
    }

    @EventHandler
    public void onStart(final PAStartEvent event) {
        final Arena a = event.getArena();
        ea.catchEvent("start", null, a);
    }

    @EventHandler
    public void onWin(final PAWinEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        ea.catchEvent("win", p, a);
    }

    @EventHandler
    public void onClassChange(final PAPlayerClassChangeEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        ea.catchEvent("classchange", p, a, "%class%", event.getArenaClass().getName());
    }


    /**
     * --------------------------------------------
     */


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public boolean playerInteract(final PlayerInteractEvent event) {
        if (!event.hasBlock()) {
            return false;
        }

        if (event.getHand() == null || event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            return false;
        }
//		debug.i("interact eventactions", event.getPlayer());
        final Arena a = PAA_Edit.activeEdits.get(event.getPlayer().getName() + "_power");

        if (a != null) {
//			debug.i("found edit arena", event.getPlayer());
            final Location loc = event.getClickedBlock().getLocation();

            final String s = "power";
            int i = 0;
            for (String node : a.getConfig().getKeys(ROOT_SPAWNS_NODE)) {
                if (node.startsWith(s) && !node.contains("powerup")) {

                    final PABlockLocation locc = Config.parseBlockLocation(
                            (String) a.getConfig().getUnsafe(String.format("%s.%s", ROOT_SPAWNS_NODE, node)),
                            null
                    );
                    if (loc.equals(locc.toLocation())) {
                        PVPArena.getInstance().getLogger().warning("Block already exists!");
                        return true;
                    }

                    node = node.replace(s, "");
                    if (Integer.parseInt(node) >= i) {
                        i = Integer.parseInt(node) + 1;
                    }
                }
            }

            SpawnManager.setBlock(a, new PABlockLocation(loc), s + i, null);
            Arena.pmsg(event.getPlayer(), MSG.SPAWN_SET, s + i);
            return true;
        }

        return false;
    }
}
