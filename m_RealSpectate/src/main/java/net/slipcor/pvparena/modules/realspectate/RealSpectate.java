package net.slipcor.pvparena.modules.realspectate;


import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ModuleType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import static net.slipcor.pvparena.config.Debugger.debug;

public class RealSpectate extends ArenaModule {
    public RealSpectate() {
        super("RealSpectate");
    }

    private RealSpectateListener listener;

    private static final int PRIORITY = 2;

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public ModuleType getType() {
        return ModuleType.SPECTATE;
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

        arenaPlayer.setArena(this.arena);
        arenaPlayer.setStatus(PlayerStatus.WATCH);
        arenaPlayer.setSpectating(true);

        if (arenaPlayer.getState() == null) {

            // Important: clear inventory before setting player state to deal with armor modifiers (like health)
            ArenaPlayer.backupAndClearInventory(this.arena, player);
            arenaPlayer.createState(player);
            arenaPlayer.dump();
        }

        debug(player, "switching:");
        this.arena.msg(player, MSG.MODULE_REALSPECTATE_INFO);
        this.getListener().switchPlayer(player, null, true);
    }

    @Override
    public void switchToSpectate(Player player) {
        debug(player, "becoming spectator using RealSpectate");
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setSpectating(true);
        debug(player, "switching:");
        this.arena.msg(player, MSG.MODULE_REALSPECTATE_INFO);
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
        if(this.listener != null) {
            this.listener.spectated_players.values().forEach(SpectateWrapper::stopHard);
            this.listener.spectated_players.clear();
            HandlerList.unregisterAll(this.listener);
        }
    }

    @Override
    public void unload(final Player player) {
        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(PVPArena.getInstance(), player));

        this.getListener().spectated_players.values().stream()
                .filter(sw -> sw.hasSpectator(player))
                .findAny()
                .ifPresent(sw -> sw.removeSpectator(player));

        if (this.arena.getFighters().isEmpty()) {
            this.getListener().spectated_players.values().forEach(SpectateWrapper::stopSpectating);
            this.getListener().spectated_players.clear();
        }
    }

    private RealSpectateListener getListener() {
        if (this.listener == null) {
            this.listener = new RealSpectateListener(this);
            Bukkit.getPluginManager().registerEvents(this.listener, PVPArena.getInstance());
        }
        return this.listener;
    }


}
