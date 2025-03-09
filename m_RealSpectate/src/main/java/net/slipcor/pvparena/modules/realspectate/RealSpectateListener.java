package net.slipcor.pvparena.modules.realspectate;

import net.slipcor.pvparena.PVPArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Map;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

class RealSpectateListener implements Listener {
    private final RealSpectate module;
    // References to Map and Set in RealSpectate
    private final Map<Player, Set<Player>> fighterWithSpectators;
    private final Set<Player> spectators;

    public RealSpectateListener(RealSpectate realSpectate, Set<Player> spectatorsRef, Map<Player, Set<Player>> fighterWithSpectatorsRef) {
        this.module = realSpectate;
        this.spectators = spectatorsRef;
        this.fighterWithSpectators = fighterWithSpectatorsRef;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player damagedPlayer = (Player) event.getEntity();
        if (this.spectators.contains(damagedPlayer)) {
            // player is spectating => cancel
            event.setCancelled(true);
            event.getDamager().remove();

            return;
        }

        Set<Player> spectatorsForEventPlayer = this.fighterWithSpectators.get(damagedPlayer);

        if (spectatorsForEventPlayer != null) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                spectatorsForEventPlayer.forEach(spectator -> spectator.setHealth(Math.max(damagedPlayer.getHealth(), 1)));
            }, 5L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player eventPlayer = (Player) event.getEntity();
        if (this.spectators.contains(eventPlayer)) {
            // player is spectating => cancel
            event.setCancelled(true);
            return;
        }

        Set<Player> spectatorsForEventPlayer = this.fighterWithSpectators.get(eventPlayer);

        if (spectatorsForEventPlayer != null) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                spectatorsForEventPlayer.forEach(spectator -> spectator.setHealth(Math.max(eventPlayer.getHealth(), 1)));
            }, 5L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(final EntityTargetEvent event) {
        if (event.getTarget() != null && event.getTarget().getType() == EntityType.PLAYER) {
            Player subject = (Player) event.getTarget();

            if (this.fighterWithSpectators.containsKey(subject)) {
                // subject is being spectated
                // --> nope. DON'T LOOK AT ME!
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        Player eventPlayer = (Player) event.getWhoClicked();
        if (this.spectators.contains(eventPlayer)) {
            // player is spectating => cancel
            event.setCancelled(true);
            return;
        }

        Set<Player> spectatorsForEventPlayer = this.fighterWithSpectators.get(eventPlayer);

        if (spectatorsForEventPlayer != null) {
            Bukkit.getScheduler().runTask(PVPArena.getInstance(), () -> {
                spectatorsForEventPlayer.forEach(spectator -> spectator.getInventory().setContents(eventPlayer.getInventory().getContents()));
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(final InventoryCloseEvent event) {
        Player eventPlayer = (Player) event.getPlayer();
        // if player is spectating => don't care

        Set<Player> spectatorsForEventPlayer = this.fighterWithSpectators.get(eventPlayer);

        if (spectatorsForEventPlayer != null) {
            spectatorsForEventPlayer.forEach(Player::closeInventory);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(final InventoryOpenEvent event) {
        Player eventPlayer = (Player) event.getPlayer();
        if (this.spectators.contains(eventPlayer)) {
            // player is spectating => cancel
            event.setCancelled(true);
            return;
        }

        Set<Player> spectatorsForEventPlayer = this.fighterWithSpectators.get(eventPlayer);

        if (spectatorsForEventPlayer != null) {
            spectatorsForEventPlayer.forEach(spectator -> spectator.openInventory(event.getInventory()));
//            Location location = event.getInventory().getLocation();
//            BlockState state = location.getBlock().getState();
//            if (state instanceof Container) {
//                spectatorsForEventPlayer.forEach(spectator -> spectator.openInventory(((Container) state).getInventory()));
//            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSneak(final PlayerToggleSneakEvent event) {
        Player eventPlayer = event.getPlayer();
        Player currentSubject = this.getFighterFromSpectator(eventPlayer);

        if (currentSubject != null && event.isSneaking()) {
            // currentSubject is being spectated
            // player is spectating
            // --> switch player to spectate
            this.module.switchPlayer(eventPlayer, currentSubject, true);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        Player eventPlayer = event.getPlayer();
        Player currentSubject = this.getFighterFromSpectator(eventPlayer);

        if (currentSubject != null) {
            // currentSubject is being spectated
            // player is spectating
            // --> switch player to spectate
            this.module.switchPlayer(eventPlayer, currentSubject, false);
            event.setCancelled(true);
        }

        Set<Player> spectatorsForEventPlayer = this.fighterWithSpectators.get(eventPlayer);

        if (spectatorsForEventPlayer != null) {
            Bukkit.getScheduler().runTask(PVPArena.getInstance(), () -> {
                spectatorsForEventPlayer.forEach(spectator -> spectator.getInventory().setContents(eventPlayer.getInventory().getContents()));
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemHeld(final PlayerItemHeldEvent event) {
        if (this.spectators.contains(event.getPlayer())) {
            // player is spectating => cancel
            event.setCancelled(true);
            return;
        }

        Set<Player> spectatorsForEventPlayer = this.fighterWithSpectators.get(event.getPlayer());

        if (spectatorsForEventPlayer != null) {
            spectatorsForEventPlayer.forEach(spectator -> spectator.getInventory().setHeldItemSlot(event.getNewSlot()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (this.spectators.contains(event.getPlayer())) {
            // player is spectating => cancel
            event.setCancelled(true);
            return;
        }

        Set<Player> spectatorsForEventPlayer = this.fighterWithSpectators.get(event.getPlayer());

        if (spectatorsForEventPlayer != null) {
            spectatorsForEventPlayer.forEach(spectator -> spectator.teleport(event.getPlayer()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(final EntityPickupItemEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player eventPlayer = (Player) event.getEntity();
        if (this.spectators.contains(eventPlayer)) {
            // player is spectating => cancel
            event.setCancelled(true);
            return;
        }

        Set<Player> spectatorsForEventPlayer = this.fighterWithSpectators.get(eventPlayer);

        if (spectatorsForEventPlayer != null) {
            Bukkit.getScheduler().runTask(PVPArena.getInstance(), () -> {
                spectatorsForEventPlayer.forEach(spectator -> spectator.getInventory().addItem(event.getItem().getItemStack()));
            });
        }
    }

    private Player getFighterFromSpectator(final Player p) {
        debug(this.module.getArena(), this.module, "getSpectatedSuspect: {}", p.getName());
        return this.fighterWithSpectators.entrySet().stream()
                .filter(entry -> entry.getValue().contains(p))
                .findAny()
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
