package net.slipcor.pvparena.modules.flyspectate;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class FlySpectateListener implements Listener {
    private final FlySpectate flySpectate;
    private final Set<Player> spectators = new HashSet<>();

    public FlySpectateListener(final FlySpectate flySpectate) {
        this.flySpectate = flySpectate;
    }

    void initiate(final ArenaPlayer arenaPlayer) {
        for (final ArenaPlayer arenaPlayerFly : flySpectate.getArena().getEveryone()) {
            arenaPlayerFly.getPlayer().hidePlayer(PVPArena.getInstance(), arenaPlayer.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Entity damager = event.getDamager();

        if (event.getDamager() instanceof Projectile) {
            final Projectile projectile = (Projectile) event.getDamager();

            if (projectile.getShooter() instanceof LivingEntity) {

                damager = (LivingEntity) projectile.getShooter();
            }
        }

        if (!(damager instanceof Player)) {
            return;
        }

        final Player subject = (Player) damager;

        if (!spectators.contains(subject)) {
            return;
        }

        // subject is spectating
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player subject = event.getPlayer();

        if (!spectators.contains(subject)) {
            return;
        }

        // subject is spectating
        // --> cancel
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
        final Player subject = event.getPlayer();

        if (!spectators.contains(subject)) {
            return;
        }

        // subject is spectating
        // --> cancel
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player subject = event.getPlayer();

        if (!spectators.contains(subject)) {
        }

        // subject is spectating
        // ->so what?
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(final InventoryOpenEvent event) {
        final Player subject = (Player) event.getPlayer();

        if (!spectators.contains(subject)) {
            return;
        }

        // subject is spectating
        // --> cancel
        event.setCancelled(true);
        event.getPlayer().closeInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        final Player subject = (Player) event.getWhoClicked();

        if (!spectators.contains(subject)) {
            return;
        }

        // subject is spectating
        // --> cancel
        event.setCancelled(true);
        event.getWhoClicked().closeInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        if (event == null ||
                event.getEntity() == null ||
                event.getEntity().getShooter() == null ||
                !(event.getEntity().getShooter() instanceof Player)) {
            return;
        }
        final Player subject = (Player) event.getEntity().getShooter();

        if (!spectators.contains(subject)) {
            return;
        }

        // subject is spectating
        // --> cancel
        event.setCancelled(true);
    }

    public void hidePlayerLater(final Player s) {
        if (!spectators.contains(s)) {
            spectators.add(s);

            class LaterRun implements Runnable {
                @Override
                public void run() {

                    for (final ArenaPlayer arenaPlayer : flySpectate.getArena().getEveryone()) {
                        arenaPlayer.getPlayer().hidePlayer(PVPArena.getInstance(), s);
                    }
                }
            }
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(), 5L);
        }
    }

    public void hideAllSpectatorsLater() {
        for (final Player s : spectators) {

            class LaterRun implements Runnable {
                private final Player s;

                LaterRun(final Player p) {
                    s = p;
                }

                @Override
                public void run() {
                    for (final ArenaPlayer arenaPlayer : flySpectate.getArena().getEveryone()) {
                        arenaPlayer.getPlayer().hidePlayer(PVPArena.getInstance(), s);
                    }
                }
            }
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(s), 5L);
        }
    }

    public void removeSpectator(final Player spectator) {
        spectators.remove(spectator);
    }

    public void stop() {
        final Collection<Player> removals = new HashSet<>();
        removals.addAll(spectators);
        for (final Player p : removals) {
            Bukkit.getServer().dispatchCommand(p, "pa leave");
        }
        spectators.clear();
    }
}
