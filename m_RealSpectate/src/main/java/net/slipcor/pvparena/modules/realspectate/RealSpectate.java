package net.slipcor.pvparena.modules.realspectate;


import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

public class RealSpectate extends ArenaModule {
    public RealSpectate() {
        super("RealSpectate");
    }

    private RealSpectateListener listener;

    private static final int PRIORITY = 2;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean handleSpectate(Player p) throws GameplayException {
        if (this.arena.getFighters().size() < 1) {
            throw new GameplayException(MSG.ERROR_NOPLAYERFOUND);
        }
        return true;
    }

    @Override
    public void commitSpectate(final Player player) {
        debug(player, "committing REAL spectate");
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));

        arenaPlayer.setArena(arena);
        arenaPlayer.setStatus(Status.WATCH);

        if (arenaPlayer.getState() == null) {

            final Arena arena = arenaPlayer.getArena();
            arenaPlayer.createState(player);
            ArenaPlayer.backupAndClearInventory(arena, player);
            arenaPlayer.dump();
        }

        this.arena.setupScoreboard(arenaPlayer);

        debug(player, "switching:");
        this.getListener().switchPlayer(player, null, true);
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        for (final SpectateWrapper sw : this.getListener().spectated_players.values()) {
            sw.update();
        }
    }


    @Override
    public void parseStart() {
        this.getListener();
    }

    @Override
    public void reset(final boolean force) {
        this.getListener();
        final Set<SpectateWrapper> list = new HashSet<>();
        for (final SpectateWrapper sw : this.getListener().spectated_players.values()) {
            list.add(sw);
        }

        for (final SpectateWrapper sw : list) {
            sw.stopHard();
        }
        this.getListener().spectated_players.clear();

    }

    @Override
    public void unload(final Player player) {
        final Set<SpectateWrapper> list = new HashSet<>();
        for (final SpectateWrapper sw : this.getListener().spectated_players.values()) {
            list.add(sw);
        }


        for (final Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(player);
        }

        for (final SpectateWrapper sw : list) {
            if (sw.hasSpectator(player)) {
                sw.removeSpectator(player);
                return;
            }
        }

        if (arena.getFighters().size() < 1) {
            final Set<SpectateWrapper> list2 = new HashSet<>();
            for (final SpectateWrapper sw : this.getListener().spectated_players.values()) {
                list2.add(sw);
            }

            for (final SpectateWrapper sw : list2) {
                sw.stopSpectating();
            }
            this.getListener().spectated_players.clear();
        }
    }

    private RealSpectateListener getListener() {
        if (listener == null) {
            listener = new RealSpectateListener(this);
            Bukkit.getPluginManager().registerEvents(listener, PVPArena.getInstance());
        }
        return listener;
    }


}
