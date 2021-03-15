package net.slipcor.pvparena.modules.playerfinder;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

public class PlayerFinder extends ArenaModule implements Listener {
    public PlayerFinder() {
        super("PlayerFinder");
    }

    private boolean setup;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void parseStart() {
        if (!setup) {
            Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
            setup = true;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerFind(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        if (event.getHand() != null && event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            debug(player, "exiting: offhand");
            return;
        }

        if (aPlayer.getArena() == null) {
            debug(player, "No arena!");
            return;
        }

        if (!aPlayer.getArena().equals(arena)) {
            debug(player, "Wrong arena!");
            return;
        }

        if (player.getInventory().getItemInHand() == null || player.getInventory().getItemInHand().getType() != Material.COMPASS) {
            debug(player, "No compass!");
            return;
        }

        final int maxRadius = arena.getArenaConfig().getInt(CFG.MODULES_PLAYERFINDER_MAXRADIUS, 100);

        final List<Entity> list = player.getNearbyEntities(maxRadius, maxRadius, maxRadius);
        final Map<Double, Player> sortMap = new HashMap<>();

        debug(player, "ok!");

        final boolean teams = !arena.isFreeForAll();

        for (final Entity e : list) {
            if (e instanceof Player) {
                if (e == player) {
                    continue;
                }

                final Player innerPlayer = (Player) e;
                final ArenaPlayer ap = ArenaPlayer.fromPlayer(innerPlayer);

                if (ap.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }

                if (teams && ap.getArenaTeam().equals(aPlayer.getArenaTeam())) {
                    continue;
                }

                debug(player, innerPlayer.getName());
                sortMap.put(player.getLocation().distance(e.getLocation()), innerPlayer);

            }
        }

        if (sortMap.isEmpty()) {
            debug(player, "noone there!");
        }

        final SortedMap<Double, Player> sortedMap = new TreeMap<>(sortMap);

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            debug("left");
            for (final Player otherPlayer : sortedMap.values()) {
                player.setCompassTarget(otherPlayer.getLocation().clone());
                arena.msg(player, Language.parse(MSG.MODULE_PLAYERFINDER_POINT, otherPlayer.getName()));
                break;
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            debug("right");
            for (final double d : sortedMap.keySet()) {
                arena.msg(player, Language.parse(MSG.MODULE_PLAYERFINDER_NEAR, String.valueOf((int) d)));
                break;
            }
        }

    }
}
