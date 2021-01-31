package net.slipcor.pvparena.modules.turrets;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

public class Turrets extends ArenaModule implements Listener {
    public Turrets() {
        super("Turrets");
    }

    private boolean setup;
    private final Map<String, Long> shootingPlayers = new HashMap<>();
    private final Map<PABlockLocation, Turret> turretMap = new HashMap<>();

    private int minInterval;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean hasSpawn(final String spawn) {
        return spawn.contains("turret");
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (arena == null) {
            return;
        }
        if (!setup) {
            Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
            setup = true;
        }

        class RunLater implements Runnable {

            @Override
            public void run() {


                final Set<PASpawn> spawns = new HashSet<>();
                for (final PASpawn spawn : arena.getSpawns()) {
                    if (spawn.getName().contains("turret")) {
                        spawns.add(spawn);
                    }
                }

                if (spawns.size() < 1) {
                    PVPArena.getInstance().getLogger().warning("No valid turret spawns found!");
                    return;
                }

                final double degrees = arena.getArenaConfig().getDouble(CFG.MODULES_TURRETS_MAXDEGREES);
                for (final PASpawn location : spawns) {
                    final PALocation loc = location.getLocation();
                    turretMap.put(new PABlockLocation(loc.toLocation()), new Turret(location.getName(), loc, degrees));
                }
            }

        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 5L);

        minInterval = arena.getArenaConfig().getInt(CFG.MODULES_TURRETS_MININTERVAL);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlateTrigger(final PlayerInteractEvent event) {

        if (event.getAction() != Action.PHYSICAL) {
            return;
        }

        debug(event.getPlayer(), "plateTrigger");

        final PABlockLocation loc = new PABlockLocation(event.getClickedBlock().getLocation());

        if (turretMap.containsKey(loc)) {
            debug(event.getPlayer(), "found. set!");
            class RunLater implements Runnable {

                @Override
                public void run() {
                    debug(event.getPlayer(), "set done!");
                    shootingPlayers.put(event.getPlayer().getName(), System.currentTimeMillis() + minInterval * 50);
                }

            }
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 5L);
        }
    }

    @EventHandler
    public void onClick(final PlayerInteractEvent event) {
        debug(event.getPlayer(), "click!");

        if (arena == null || !arena.isFightInProgress() || !shootingPlayers.containsKey(event.getPlayer().getName())) {
            return;
        }

        if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            debug(event.getPlayer(), "exiting: offhand");
            return;
        }
        debug(event.getPlayer(), "ok?");

        if (event.getAction() == Action.PHYSICAL || shootingPlayers.get(event.getPlayer().getName()) > System.currentTimeMillis()) {
            return; // no click OR waiting
        }
        debug(event.getPlayer(), "ok!");

        final Player player = event.getPlayer();

        final Turret turret = turretMap.get(new PABlockLocation(event.getPlayer().getLocation()));

        if (turret == null) {
            return;
        }

        final Projectile projectile = player.launchProjectile(turret.getType());


        projectile.teleport(event.getPlayer().getLocation().getBlock().getRelative(BlockFace.UP, 3).getLocation().add(0.5, 0.5, 0.5).add(projectile.getVelocity().multiply(1 / 4)));

        shootingPlayers.put(event.getPlayer().getName(), System.currentTimeMillis() + minInterval * 50);
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (arena == null || !arena.isFightInProgress() || !shootingPlayers.containsKey(event.getPlayer().getName())) {
            return;
        }
        //TODO: check movement, cancel if too much off, break chain if off block
        if (event.getTo().getBlock().equals(event.getFrom().getBlock())) {

            final Turret turret = turretMap.get(new PABlockLocation(event.getFrom()));
            if (turret != null && turret.cancelMovement(event.getTo().getYaw())) {
                event.setCancelled(true);
                event.getPlayer().getLocation().setYaw(turret.getYaw());
            }

            return;
        }
        debug(event.getPlayer(), "new block!");

        shootingPlayers.remove(event.getPlayer().getName());
    }

    @Override
    public void reset(final boolean force) {
        shootingPlayers.clear();
    }
}
