package net.slipcor.pvparena.modules.spectate;


import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ModuleType;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeleportManager;
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
    public void commitJoin(final Player player, final ArenaTeam team) {
        class RunLater implements Runnable {

            @Override
            public void run() {
                Spectate.this.commitSpectate(player);
            }

        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 3L);
    }

    @Override
    public void commitSpectate(final Player player) {
        debug(player, "committing spectate");

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        if (this.arena.equals(arenaPlayer.getArena())) {
            this.arena.msg(player, MSG.ERROR_ARENA_ALREADY_PART_OF, this.arena.getName());
            return;
        }

        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));

        arenaPlayer.debugPrint();

        arenaPlayer.setArena(this.arena);
        this.getListener().addSpectator(player);

        if (arenaPlayer.getState() == null) {
            // Important: clear inventory before setting player state to deal with armor modifiers (like health)
            ArenaPlayer.backupAndClearInventory(this.arena, player);
            arenaPlayer.createState(player);
            arenaPlayer.dump();

        } else {
            new PAG_Leave().commit(this.arena, player, new String[0]);
            return;
        }

        this.teleportAndChangeState(player);
        arenaPlayer.setStatus(PlayerStatus.WATCH);
    }

    @Override
    public void switchToSpectate(Player player) {
        debug(player, "becoming spectator using Spectate");
        this.teleportAndChangeState(player);
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

    private void teleportAndChangeState(Player player) {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        this.getListener().addSpectator(player);

        final long delay = this.arena.getConfig().getBoolean(CFG.PERMS_FLY) ? 6L : 5L;
        final long delay2 = this.arena.getConfig().getBoolean(CFG.PERMS_FLY) ? 20L : 24L;
        class RunLater implements Runnable {

            @Override
            public void run() {
                TeleportManager.teleportPlayerToRandomSpawn(Spectate.this.arena, arenaPlayer, SpawnManager.getPASpawnsStartingWith(Spectate.this.arena, PASpawn.SPECTATOR));
                Spectate.this.arena.msg(player, MSG.NOTICE_WELCOME_SPECTATOR);
                arenaPlayer.setSpectating(true);
            }
        }
        class RunEvenLater implements Runnable {

            @Override
            public void run() {
                if (Spectate.this.arena.getConfig().getGameMode(CFG.GENERAL_GAMEMODE) != null) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
                player.setFlySpeed(0.2f);
            }
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new RunLater(), delay);
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new RunEvenLater(), delay2);
    }

    private SpectateListener getListener() {
        if (this.listener == null) {
            this.listener = new SpectateListener(this);
            Bukkit.getPluginManager().registerEvents(this.listener, PVPArena.getInstance());
        }
        return this.listener;
    }
}
