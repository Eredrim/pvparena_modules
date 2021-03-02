package net.slipcor.pvparena.modules.flyspectate;


import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.slipcor.pvparena.config.Debugger.debug;

public class FlySpectate extends ArenaModule {
    public static final int PRIORITY = 3;

    public FlySpectate() {
        super("FlySpectate");
    }

    private FlySpectateListener listener;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public int getPriority(){
        return PRIORITY;
    }

    @Override
    public boolean handleSpectate(Player player) throws GameplayException {
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
        debug(player, "committing FLY spectate");
        final ArenaPlayer arenaPlayer = ArenaPlayer.parsePlayer(player.getName());
        if (arena.equals(arenaPlayer.getArena())) {
            arena.msg(player, Language.parse(MSG.ERROR_ARENA_ALREADY_PART_OF, arena.getName()));
            return;
        }

        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));

        arenaPlayer.debugPrint();

        arenaPlayer.setArena(arena);
        arenaPlayer.setTeleporting(true);
        debug(player, "switching:");
        this.getListener().hidePlayerLater(player);

        if (arenaPlayer.getState() == null) {

            final Arena arena = arenaPlayer.getArena();

            arenaPlayer.createState(player);
            ArenaPlayer.backupAndClearInventory(arena, player);
            arenaPlayer.dump();

        } else {
            new PAG_Leave().commit(arena, player, new String[0]);
            return;
        }


        final long delay = this.arena.getArenaConfig().getBoolean(CFG.PERMS_FLY) ? 6L : 5L;
        this.arena.tpPlayerToCoordNameForJoin(arenaPlayer, "spectator", false);

        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), () -> {
            if (this.arena.getArenaConfig().getGameMode(CFG.GENERAL_GAMEMODE) != null) {
                player.setGameMode(GameMode.CREATIVE);
            }
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setCollidable(false);
            this.arena.msg(player, Language.parse(MSG.NOTICE_WELCOME_SPECTATOR));
            arenaPlayer.setStatus(ArenaPlayer.Status.WATCH);
            arenaPlayer.setTeleporting(false);
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
