package net.slipcor.pvparena.modules.arenarotate;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.runnables.StartRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ArenaRotate extends ArenaModule {
    private static Arena a;

    private static ArenaRotateRunnable vote;

    public ArenaRotate() {
        super("ArenaRotate");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void checkJoin(Player player) throws GameplayException {
        if (a != null && !this.arena.equals(a)) {
            Bukkit.getServer().dispatchCommand(player, "join " + this.arena.getName());
            throw new GameplayException(Language.parse(MSG.MODULE_AUTOVOTE_ARENARUNNING, this.arena.getName()));
        }

        if (this.arena.getArenaConfig().getBoolean(CFG.PERMS_JOINWITHSCOREBOARD)) {
            for (final Team team : player.getScoreboard().getTeams()) {
                for (final String playerName : team.getEntries()) {
                    if (player.getName().equals(playerName)) {
                        throw new GameplayException(Language.parse(MSG.ERROR_COMMAND_BLOCKED, "You already have a scoreboard!"));
                    }
                }
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        a = null;

        if (vote == null) {
            vote = new ArenaRotateRunnable(
                    arena.getArenaConfig().getInt(CFG.MODULES_ARENAVOTE_SECONDS));
        }
    }

    public static void commit() {

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "pvparena ALL disable");


        if (a == null) {

            int pos = new Random().nextInt(ArenaManager.getArenas().size());

            for (final Arena arena : ArenaManager.getArenas()) {
                if (--pos < 0) {
                    a = arena;
                    break;
                }
            }
        }

        if (a == null) {
            PVPArena.getInstance().getLogger().warning("Rotation resulted in NULL!");

            return;
        }

        final PAG_Join pj = new PAG_Join();

        final Set<String> toTeleport = new HashSet<>();

        for (final Player p : Bukkit.getOnlinePlayers()) {
            toTeleport.add(p.getName());
        }

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "pvparena " + a.getName() + " enable");

        class TeleportLater extends BukkitRunnable {

            @Override
            public void run() {
                for (final String pName : toTeleport) {
                    final Player p = Bukkit.getPlayerExact(pName);
                    toTeleport.remove(pName);
                    if (p == null) {
                        return;
                    }

                    pj.commit(a, p, new String[0]);
                    return;
                }

                new StartRunnable(a,
                        a.getArenaConfig().getInt(CFG.MODULES_ARENAVOTE_READYUP));
                class RunLater implements Runnable {

                    @Override
                    public void run() {
                        vote = null;
                    }

                }
                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 20L);
                cancel();
            }

        }
        new TeleportLater().runTaskTimer(PVPArena.getInstance(), 1L, 1L);

    }

    @Override
    public void onThisLoad() {

        class RunLater implements Runnable {

            @Override
            public void run() {
                boolean active = false;
                ArenaModule commitMod = null;
                for (final Arena arena : ArenaManager.getArenas()) {
                    for (final ArenaModule mod : arena.getMods()) {
                        if (mod.getName().equals(getName())
                                && arena.getArenaConfig().getBoolean(CFG.MODULES_ARENAVOTE_AUTOSTART)) {

                            active = true;
                            commitMod = mod;
                            break;
                        }
                    }
                }

                if (!active) {
                    return;
                }
                commitMod.reset(false);
            }

        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 200L);
    }
}
