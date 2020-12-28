package net.slipcor.pvparena.modules.realspectate;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAG_Spectate;
import net.slipcor.pvparena.config.Debugger;
import net.slipcor.pvparena.events.PADeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;

class RealSpectateListener implements Listener {
    final RealSpectate rs;
    final Map<Player, SpectateWrapper> spectated_players = new HashMap<>();

    public RealSpectateListener(final RealSpectate realSpectate) {
        rs = realSpectate;
    }

    void initiate(final ArenaPlayer ap) {
        for (final ArenaPlayer a : rs.getArena().getEveryone()) {
            update(ap, a);

            break;
        }
    }

    void update(final ArenaPlayer spectator, final ArenaPlayer fighter) {
        final Player s = spectator.get();
        final Player f = fighter.get();

        createSpectateWrapper(s, f);
    }

    SpectateWrapper createSpectateWrapper(final Player s,
                                          final Player f) {
        //debug.i("createSwapper", s);
        debug(s, "create wrapper: {} + {}", s.getName(), f);
        if (!spectated_players.containsKey(f)) {
            spectated_players.put(f, new SpectateWrapper(s, f, this));
        }
        for (final SpectateWrapper sw : spectated_players.values()) {
            sw.update(s);
            sw.update();
        }
        return spectated_players.get(f);
    }

    private Player getSpectatedSuspect(final Player p) {
        debug(rs.getArena(), "getSpecated: " + p.getName());
        for (final SpectateWrapper sw : spectated_players.values()) {
            debug(rs.getArena(), "found wrapper: " + sw.getSuspect().getName());
            sw.debugSpectators(rs.getArena());
            if (sw.hasSpectator(p)) {
                return sw.getSuspect();
            }
        }

        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        final Player player = (Player) event.getEntity();
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());

        if (rs.getArena() == null || !rs.getArena().equals(aPlayer.getArena())) {
            return;
        }
        debug(rs.getArena(), "RealSpectateListener oEEDBE");

        Player subject = getSpectatedSuspect(player);

        if (subject != null) {
            // subject is being spectated

            debug(subject, "player is spectating and being damaged");

            if (event.getDamager() instanceof Projectile) {

                debug(subject, "relay damage");
                // Damage is a Projectile that should have hit the subject
                // --> relay damage to subject

                final EntityDamageByEntityEvent projectileEvent = new EntityDamageByEntityEvent(
                        event.getDamager(), subject, event.getCause(),
                        event.getDamage());

                subject.setLastDamageCause(projectileEvent);
                subject.damage(event.getDamage(), event.getDamager());

            }

            // spectators don't receive damage

            event.setCancelled(true);
            event.getDamager().remove();

            return;
        }

        subject = (Player) event.getEntity();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        spectated_players.get(subject).updateHealth();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(final EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player subject = getSpectatedSuspect((Player) event.getEntity());

        if (subject != null) {
            // subject is being spectated

            final Player spectator = (Player) event.getEntity();
            // player is spectating and has died. wait, what?
            // --> hack reset!
            spectator.setHealth(1);
            event.getDrops().clear();
            return;
        }

        subject = (Player) event.getEntity();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        spectated_players.get(subject).stopSpectating();
        spectated_players.remove(subject);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player subject = getSpectatedSuspect((Player) event.getEntity());

        if (subject != null) {
            // subject is being spectated

            // player is spectating and wanting to regain health
            // --> cancelling
            event.setCancelled(true);
            return;
        }

        subject = (Player) event.getEntity();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        spectated_players.get(subject).updateHealth();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(final EntityTargetEvent event) {
        if (event.getTarget() == null || event.getTarget().getType() != EntityType.PLAYER) {
            return;
        }

        final Player subject = (Player) event.getTarget();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        // --> nope. DON'T LOOK AT ME!
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        Player subject = getSpectatedSuspect((Player) event.getWhoClicked());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> no clicking!

            event.setCancelled(true);
            return;
        }

        subject = (Player) event.getWhoClicked();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        spectated_players.get(subject).updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(final InventoryCloseEvent event) {
        Player subject = getSpectatedSuspect((Player) event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> don't care
            return;
        }

        subject = (Player) event.getPlayer();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        // --> close all other inventories

        spectated_players.get(subject).closeInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(final InventoryOpenEvent event) {
        Player subject = getSpectatedSuspect((Player) event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> no opening!
            event.setCancelled(true);
            return;
        }

        subject = (Player) event.getPlayer();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        spectated_players.get(subject).openInventory(event.getInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        final Player subject = getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> no dropping!
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player subject = getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            final Player spectator = event.getPlayer();
            // player is spectating
            // --> cancel and switch
            final String actionName = event.getAction().name();

            event.setCancelled(true);

            if (actionName.startsWith("LEFT_")) {
                switchPlayer(spectator, subject, false);
            } else if (actionName.startsWith("RIGHT_")) {
                switchPlayer(spectator, subject, true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        final Player subject = getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            final Player spectator = event.getPlayer();
            // player is spectating
            // --> no clicking!!!
            event.setCancelled(true);
            switchPlayer(spectator, subject, event.getRightClicked() != null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemHeld(final PlayerItemHeldEvent event) {
        Player subject = getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> so what?
            return;
        }

        subject = event.getPlayer();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        // --> so what?
        spectated_players.get(subject).selectItem(event.getNewSlot());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        Player subject = getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> NO MOVING!
            event.setCancelled(true);
            return;
        }

        subject = event.getPlayer();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        spectated_players.get(subject).updateLocation();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
        Player subject = getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> no pickup!
            event.setCancelled(true);
            return;
        }

        subject = event.getPlayer();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        spectated_players.get(subject).updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        Player subject = getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            final Player spectator = event.getPlayer();
            // player is spectating
            // --> remove from spectators
            spectated_players.get(subject).removeSpectator(spectator);
            return;
        }

        subject = event.getPlayer();

        if (!spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        spectated_players.get(subject).stopSpectating();
        spectated_players.remove(subject);
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        debug("ProjectileLaunch!");
        if (event == null ||
                event.getEntity() == null ||
                event.getEntity().getShooter() == null ||
                !(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        Player subject = getSpectatedSuspect((Player) event.getEntity().getShooter());

        if (subject != null) {
            debug(subject, "subject != null");
            // subject is being spectated
            // player is spectating
            // --> cancel and out
            event.setCancelled(true);
            return;
        }

        subject = (Player) event.getEntity().getShooter();

        if (!spectated_players.containsKey(subject)) {
            debug(subject, "not being spectated");
            return;
        }

        debug(subject, "subject is being spectated");

        final Projectile projectile = event.getEntity();
        final Location location = subject.getLocation();

        debug(subject, "location: {}", new PABlockLocation(location));
        final Vector direction = location.getDirection();

        location.add(direction.normalize().multiply(1));
        //location.setY(subject.getEyeLocation().getY());
        location.setY(location.getY() + 1.4D);

        debug(subject, "location: {}", new PABlockLocation(location));

        projectile.teleport(location);

    }

    @EventHandler
    public void onPADeath(final PADeathEvent event) {
        if (!event.getArena().equals(rs.getArena())) {
            return;
        }
        if (!event.isRespawning()) {
            try {
                class RunLater implements Runnable {

                    @Override
                    public void run() {
                        if (event.getArena().isFightInProgress()) {
                            final PAG_Spectate spec = new PAG_Spectate();
                            spec.commit(event.getArena(), event.getPlayer(), new String[0]);
                        }
                    }

                }
                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 5L);
            } catch (final Exception e) {

            }
        }
    }

    void switchPlayer(final Player spectator, final Player subject, final boolean forward) {
        if (subject != null) {
            spectator.showPlayer(subject);
        }

        if (rs.getArena().getFighters().size() < 1) {
            debug(spectator, "< 1");
            return;
        }

        Player nextPlayer = null;
        for (final ArenaPlayer ap : rs.getArena().getFighters()) {
            debug(spectator, "checking {}", ap.getName());
            final Player p = ap.get();

            if (ap.getName().equals(spectator.getName())) {
                debug(spectator, "we are still in -.-");
                continue;
            }

            if (subject == null) {
                debug(spectator, "subject == null");
                nextPlayer = p;
                break;
            }


            if (!p.equals(subject)) {
                debug(spectator, "||");
                nextPlayer = p;
                continue;
            }

            // p == subject

            if (!forward) {
                debug(spectator, "step back");
                if (nextPlayer == null) {
                    debug(spectator, "get last element");
                    for (final ArenaPlayer ap2 : rs.getArena().getFighters()) {
                        debug(spectator, ap2.getName());
                        nextPlayer = ap2.get();
                    }
                    continue;
                } // else: nextPlayer has content. yay!

                debug(spectator, "==> {}", nextPlayer.getName());
                break;
            }
        }
        if (subject != null) {
            spectated_players.get(subject).removeSpectator(spectator);
        }
        createSpectateWrapper(spectator, nextPlayer);
    }
}
