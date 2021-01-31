package net.slipcor.pvparena.modules.spectate;


import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.slipcor.pvparena.config.Debugger.debug;

public class Spectate extends ArenaModule {
    public Spectate() {
        super("Spectate");
    }

    private SpectateListener listener;

    private static final int priority = 3;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public PACheck checkJoin(final CommandSender sender,
                             final PACheck res, final boolean join) {
        if (join) {
            return res;
        }

        if (arena.getFighters().size() < 1) {
            res.setError(this, Language.parse(MSG.ERROR_NOPLAYERFOUND));
        }

        if (res.getPriority() < priority || join && res.hasError()) {
            res.setPriority(this, priority);
        }
        return res;
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

        final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
        if (arena.equals(ap.getArena())) {
            arena.msg(player, Language.parse(MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(arena)));
            return;
        }

        ap.setLocation(new PALocation(ap.get().getLocation()));

        ap.debugPrint();

        ap.setArena(arena);
        this.getListener().addSpectator(player);

        if (ap.getState() == null) {
            final Arena arena = ap.getArena();

            ap.createState(player);
            ArenaPlayer.backupAndClearInventory(arena, player);
            ap.dump();
        } else {
            new PAG_Leave().commit(arena, player, new String[0]);
            return;
        }


        final long delay = arena.getArenaConfig().getBoolean(CFG.PERMS_FLY) ? 6L : 5L;
        final long delay2 = arena.getArenaConfig().getBoolean(CFG.PERMS_FLY) ? 20L : 24L;
        class RunLater implements Runnable {

            @Override
            public void run() {
                arena.tpPlayerToCoordNameForJoin(ap, "spectator", false);
                arena.msg(player, Language.parse(MSG.NOTICE_WELCOME_SPECTATOR));
                ap.setStatus(Status.WATCH);
            }
        }
        class RunEvenLater implements Runnable {

            @Override
            public void run() {
                if (arena.getArenaConfig().getInt(CFG.GENERAL_GAMEMODE) > -1) {
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
