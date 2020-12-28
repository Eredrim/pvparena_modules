package net.slipcor.pvparena.modules.flyspectate;


import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.slipcor.pvparena.config.Debugger.debug;

public class FlySpectate extends ArenaModule {
    public FlySpectate() {
        super("FlySpectate");
    }

    private FlySpectateListener listener;

    private static final int priority = 3;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public PACheck checkJoin(final CommandSender sender,
                             final PACheck res, final boolean join) {
        if (join && (arena.getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE) || !arena.isFightInProgress())) {
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
        debug(player, "committing FLY spectate");
        final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
        if (arena.equals(ap.getArena())) {
            arena.msg(player, Language.parse(MSG.ERROR_ARENA_ALREADY_PART_OF, arena.getName()));
            return;
        }

        ap.setLocation(new PALocation(ap.get().getLocation()));

        ap.debugPrint();

        ap.setArena(arena);
        ap.setTeleporting(true);
        debug(player, "switching:");
        this.getListener().hidePlayerLater(player);

        if (ap.getState() == null) {

            final Arena arena = ap.getArena();

            ap.createState(player);
            ArenaPlayer.backupAndClearInventory(arena, player);
            ap.dump();

        } else {
            new PAG_Leave().commit(arena, player, new String[0]);
            return;
        }


        final long delay = this.arena.getArenaConfig().getBoolean(CFG.PERMS_FLY) ? 6L : 5L;
        this.arena.tpPlayerToCoordNameForJoin(ap, "spectator", false);

        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), () -> {
            if (this.arena.getArenaConfig().getInt(CFG.GENERAL_GAMEMODE) > -1) {
                player.setGameMode(GameMode.CREATIVE);
            }
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setCollidable(false);
            this.arena.msg(player, Language.parse(MSG.NOTICE_WELCOME_SPECTATOR));
            ap.setStatus(ArenaPlayer.Status.WATCH);
            ap.setTeleporting(false);
        }, delay);
    }

    @Override
    public void parseJoin(final CommandSender sender, final ArenaTeam team) {
        this.getListener().hideAllSpectatorsLater();
    }

    @Override
    public void reset(final boolean force) {
        if (listener != null) {
            listener.stop();
        }
    }

    @Override
    public void unload(final Player player) {
        for (final Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(PVPArena.getInstance(), player);
        }

        listener.removeSpectator(player);

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setCollidable(true);
    }

    private FlySpectateListener getListener() {
        if (listener == null) {
            listener = new FlySpectateListener(this);
            Bukkit.getPluginManager().registerEvents(listener, PVPArena.getInstance());
        }
        return listener;
    }
}
