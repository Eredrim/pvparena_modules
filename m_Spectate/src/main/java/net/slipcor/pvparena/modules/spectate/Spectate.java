package net.slipcor.pvparena.modules.spectate;


import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import static net.slipcor.pvparena.config.Debugger.debug;

public class Spectate extends ArenaModule {
    public Spectate() {
        super("Spectate");
    }

    private SpectateListener listener;

    private static final int PRIORITY = 3;

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
    public void commitJoin(final Player player, final ArenaTeam team) {
        class RunLater implements Runnable {

            @Override
            public void run() {
                commitSpectate(player);
            }

        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 3L);
    }

    @Override
    public void commitSpectate(final Player player) {
        debug(player, "committing spectate");

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        if (arena.equals(arenaPlayer.getArena())) {
            arena.msg(player, Language.parse(MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(arena)));
            return;
        }

        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));

        arenaPlayer.debugPrint();

        arenaPlayer.setArena(arena);
        this.getListener().addSpectator(player);

        if (arenaPlayer.getState() == null) {
            final Arena arena = arenaPlayer.getArena();

            arenaPlayer.createState(player);
            ArenaPlayer.backupAndClearInventory(arena, player);
            arenaPlayer.dump();
        } else {
            new PAG_Leave().commit(arena, player, new String[0]);
            return;
        }


        final long delay = arena.getConfig().getBoolean(CFG.PERMS_FLY) ? 6L : 5L;
        final long delay2 = arena.getConfig().getBoolean(CFG.PERMS_FLY) ? 20L : 24L;
        class RunLater implements Runnable {

            @Override
            public void run() {
                arena.tpPlayerToCoordNameForJoin(arenaPlayer, "spectator", false);
                arena.msg(player, Language.parse(MSG.NOTICE_WELCOME_SPECTATOR));
                arenaPlayer.setStatus(PlayerStatus.WATCH);
            }
        }
        class RunEvenLater implements Runnable {

            @Override
            public void run() {
                if (arena.getConfig().getGameMode(CFG.GENERAL_GAMEMODE) != null) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
                player.setFlySpeed(0.2f);
            }
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new RunLater(), delay);
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new RunEvenLater(), delay2);
    }

    @Override
    public void reset(final boolean force) {
        if(this.listener != null) {
            this.listener.stop();
        }
    }

    @Override
    public void unload(final Player player) {
        if(this.listener != null) {
            this.listener.removeSpectator(player);
        }

        player.setAllowFlight(false);
        player.setFlying(false);
    }

    private SpectateListener getListener() {
        if (listener == null) {
            listener = new SpectateListener(this);
            Bukkit.getPluginManager().registerEvents(listener, PVPArena.getInstance());
        }
        return listener;
    }
}
