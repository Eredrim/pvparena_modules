package net.slipcor.pvparena.modules.autosneak;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.events.*;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AutoSneak module
 *
 * Thanks to CVDH for providing your code!
 */
public class AutoSneak extends ArenaModule implements Listener {

    public AutoSneak() {
        super("autosneak");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    private final List<String> sneaker = new ArrayList<>();

    @Override
    public void configParse(final YamlConfiguration config) {
        Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
    }

    @Override
    public void reset(final boolean force) {
        sneaker.clear();
    }

    @EventHandler
    public void onStart(final PAStartEvent e) {
        if (!e.getArena().getName().equals(arena.getName())) {
            return;
        }
        final Set<String> players = e.getArena().getPlayedPlayers();
        for (final String player : players) {
            doSneak(player);
        }
    }

    @EventHandler
    public void onDeath(final PADeathEvent e) {
        if (e.isRespawning() && sneaker.contains(e.getPlayer().getName())) {
            doSneak(e.getPlayer().getName());
        }
    }

    @EventHandler
    public void OnLeave(final PALeaveEvent e) {
        if (sneaker.contains(e.getPlayer().getName())) {
            stopSneak(e.getPlayer().getName());
        }
    }

    @EventHandler
    public void OnExit(final PAExitEvent e) {
        if (sneaker.contains(e.getPlayer().getName())) {
            stopSneak(e.getPlayer().getName());
        }
    }

    @EventHandler
    public void onSneak(final PlayerToggleSneakEvent e) {
        if (sneaker.contains(e.getPlayer().getName())) {
            e.setCancelled(true);
            e.getPlayer().setSneaking(true);
        }
    }

    @EventHandler
    public void OnEnd(final PAEndEvent e) {
        for (final String p : e.getArena().getPlayedPlayers()) {
            if (sneaker.contains(p)) {
                stopSneak(p);
            }
        }
    }

    void doSneak(final String player) {
        Bukkit.getPlayer(player).setSneaking(true);
        sneaker.add(player);
    }

    void stopSneak(final String player) {
        Bukkit.getPlayer(player).setSneaking(false);
        sneaker.remove(player);
    }
}

