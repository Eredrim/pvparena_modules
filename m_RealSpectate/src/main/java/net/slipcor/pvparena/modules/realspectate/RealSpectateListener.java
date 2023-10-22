package net.slipcor.pvparena.modules.realspectate;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAG_Spectate;
import net.slipcor.pvparena.events.PADeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;

class RealSpectateListener implements Listener {
    final RealSpectate rs;
    final Map<Player, SpectateWrapper> spectated_players = new HashMap<>();

    public RealSpectateListener(final RealSpectate realSpectate) {
        this.rs = realSpectate;
    }

    void createSpectateWrapper(final Player s, final Player f) {
        debug(s, "create wrapper: {} + {}", s.getName(), f);
        if (!this.spectated_players.containsKey(f)) {
            this.spectated_players.put(f, new SpectateWrapper(s, f, this));
        }
        this.spectated_players.values().forEach(sw -> {
            sw.update(s);
            sw.update();
        });
    }

    private Player getSpectatedSuspect(final Player p) {
        debug(this.rs.getArena(), "getSpecated: " + p.getName());
        for (final SpectateWrapper sw : this.spectated_players.values()) {
            debug(this.rs.getArena(), "found wrapper: " + sw.getSuspect().getName());
            sw.debugSpectators(this.rs.getArena());
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
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        if (this.rs.getArena() == null || !this.rs.getArena().equals(aPlayer.getArena())) {
            return;
        }
        debug(this.rs.getArena(), "RealSpectateListener oEEDBE");

        Player subject = this.getSpectatedSuspect(player);

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

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        this.spectated_players.get(subject).updateHealth();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(final EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player subject = this.getSpectatedSuspect((Player) event.getEntity());

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

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        this.spectated_players.get(subject).stopSpectating();
        this.spectated_players.remove(subject);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player subject = this.getSpectatedSuspect((Player) event.getEntity());

        if (subject != null) {
            // subject is being spectated

            // player is spectating and wanting to regain health
            // --> cancelling
            event.setCancelled(true);
            return;
        }

        subject = (Player) event.getEntity();

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        this.spectated_players.get(subject).updateHealth();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(final EntityTargetEvent event) {
        if (event.getTarget() == null || event.getTarget().getType() != EntityType.PLAYER) {
            return;
        }

        final Player subject = (Player) event.getTarget();

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        // --> nope. DON'T LOOK AT ME!
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        Player subject = this.getSpectatedSuspect((Player) event.getWhoClicked());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> no clicking!

            event.setCancelled(true);
            return;
        }

        subject = (Player) event.getWhoClicked();

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        this.spectated_players.get(subject).updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(final InventoryCloseEvent event) {
        Player subject = this.getSpectatedSuspect((Player) event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> don't care
            return;
        }

        subject = (Player) event.getPlayer();

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        // --> close all other inventories

        this.spectated_players.get(subject).closeInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(final InventoryOpenEvent event) {
        Player eventPlayer = (Player) event.getPlayer();
        Player subject = this.getSpectatedSuspect(eventPlayer);

        if (subject != null) {
            // subject is being spectated
            // player is spectating
            // --> no opening!
            event.setCancelled(true);
            return;
        }

        if (!this.spectated_players.containsKey(eventPlayer)) {
            return;
        }

        // subject is being spectated
        this.spectated_players.get(eventPlayer).openInventory(event.getInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSneak(final PlayerToggleSneakEvent event) {
        Player eventPlayer = event.getPlayer();
        Player subject = this.getSpectatedSuspect(eventPlayer);

        if (subject != null && event.isSneaking()) {
            // subject is being spectated
            // player is spectating
            // --> no opening and switch player to spectate
            this.switchPlayer(eventPlayer, subject, true);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        Player eventPlayer = event.getPlayer();
        Player subject = this.getSpectatedSuspect(eventPlayer);

        if (subject != null) {
            // subject is being spectated
            // player is spectating
            // --> no opening and switch player to spectate
            this.switchPlayer(eventPlayer, subject, false);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemHeld(final PlayerItemHeldEvent event) {
        Player subject = this.getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> so what?
            return;
        }

        subject = event.getPlayer();

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        // --> so what?
        this.spectated_players.get(subject).selectItem(event.getNewSlot());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        Player subject = this.getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            // player is spectating
            // --> NO MOVING!
            event.setCancelled(true);
            return;
        }

        subject = event.getPlayer();

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        this.spectated_players.get(subject).updateLocation();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(final EntityPickupItemEvent event) {
        if(!(event.getEntity() instanceof Player)) {
            return;
        }

        Player subject = (Player) event.getEntity();
        Player suspect = this.getSpectatedSuspect(subject);

        if (suspect != null) {
            // subject is being spectated

            // player is spectating
            // --> no pickup!
            event.setCancelled(true);
            return;
        }

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        this.spectated_players.get(subject).updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        Player subject = this.getSpectatedSuspect(event.getPlayer());

        if (subject != null) {
            // subject is being spectated

            final Player spectator = event.getPlayer();
            // player is spectating
            // --> remove from spectators
            this.spectated_players.get(subject).removeSpectator(spectator);
            return;
        }

        subject = event.getPlayer();

        if (!this.spectated_players.containsKey(subject)) {
            return;
        }

        // subject is being spectated
        this.spectated_players.get(subject).stopSpectating();
        this.spectated_players.remove(subject);
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        debug("ProjectileLaunch!");
        if (event == null || event.getEntity().getShooter() == null || !(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        Player subject = this.getSpectatedSuspect((Player) event.getEntity().getShooter());

        if (subject != null) {
            debug(subject, "subject != null");
            // subject is being spectated
            // player is spectating
            // --> cancel and out
            event.setCancelled(true);
            return;
        }

        subject = (Player) event.getEntity().getShooter();

        if (!this.spectated_players.containsKey(subject)) {
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
        if (event.getArena().equals(this.rs.getArena()) && !event.isRespawning()) {
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
            } catch (final Exception ignored) {

            }
        }
    }

    void switchPlayer(final Player spectator, final Player subject, final boolean forward) {
        if (subject != null) {
            spectator.showPlayer(PVPArena.getInstance(), subject);
        }

        if (this.rs.getArena().getFighters().size() < 1) {
            debug(spectator, "< 1");
            return;
        }

        Player nextPlayer = null;
        for (final ArenaPlayer ap : this.rs.getArena().getFighters()) {
            debug(spectator, "checking {}", ap.getName());
            final Player p = ap.getPlayer();

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
                    for (final ArenaPlayer ap2 : this.rs.getArena().getFighters()) {
                        debug(spectator, ap2.getName());
                        nextPlayer = ap2.getPlayer();
                    }
                    continue;
                } // else: nextPlayer has content. yay!

                debug(spectator, "==> {}", nextPlayer.getName());
                break;
            }
        }
        if (subject != null) {
            this.spectated_players.get(subject).removeSpectator(spectator);
        }
        this.createSpectateWrapper(spectator, nextPlayer);
    }
}
