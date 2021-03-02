package net.slipcor.pvparena.modules.redstone;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.listeners.PlayerListener;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import static net.slipcor.pvparena.config.Debugger.debug;

public class RedStoneTriggers extends ArenaModule implements Listener {
    public RedStoneTriggers() {
        super("RedStoneTriggers");
    }

    private boolean setup;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (!setup) {
            Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
            setup = true;
        }
    }

    @EventHandler
    public void onRedStone(final BlockRedstoneEvent event) {
        final Arena arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(event.getBlock().getLocation()));
        if (arena == null || !arena.equals(this.arena)) {
            return;
        }
        debug(arena, "redstone in arena");

        final Block block = event.getBlock();

        if (!(block.getState() instanceof Sign)) {
            return;
        }

        final Sign s = (Sign) block.getState();

        if ("[WIN]".equals(s.getLine(0))) {
            for (final ArenaTeam team : arena.getTeams()) {
                if (team.getName().equalsIgnoreCase(s.getLine(1))) {
                    // skip winner
                    continue;
                }
                for (final ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                    if (arenaPlayer.getStatus() == Status.FIGHT) {
                        event.getBlock().getWorld().strikeLightningEffect(arenaPlayer.getPlayer().getLocation());
                        final EntityDamageEvent e = new EntityDamageEvent(arenaPlayer.getPlayer(), DamageCause.LIGHTNING,10.0);
                        PlayerListener.finallyKillPlayer(arena, arenaPlayer.getPlayer(), e);
                    }
                }
            }
        } else if ("[LOSE]".equals(s.getLine(0))) {
            for (final ArenaTeam team : arena.getTeams()) {
                if (!team.getName().equalsIgnoreCase(s.getLine(1))) {
                    // skip winner
                    continue;
                }
                for (final ArenaPlayer ap : team.getTeamMembers()) {
                    if (ap.getStatus() == Status.FIGHT) {
                        event.getBlock().getWorld().strikeLightningEffect(ap.getPlayer().getLocation());
                        final EntityDamageEvent e = new EntityDamageEvent(ap.getPlayer(), DamageCause.LIGHTNING, 10.0);
                        PlayerListener.finallyKillPlayer(arena, ap.getPlayer(), e);
                    }
                }
            }
        }
    }
}
