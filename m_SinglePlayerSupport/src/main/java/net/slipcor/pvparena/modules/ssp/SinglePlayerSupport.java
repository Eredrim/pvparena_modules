package net.slipcor.pvparena.modules.ssp;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

public class SinglePlayerSupport extends ArenaModule {

    private static final int PRIORITY = 1666;

    public SinglePlayerSupport() {
        super("SinglePlayerSupport");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean handleJoin(Player player) throws GameplayException {
        if (this.arena.isLocked() && !player.hasPermission("pvparena.admin")
                && !(player.hasPermission("pvparena.create") && this.arena.getOwner().equals(player.getName()))) {
            throw new GameplayException(Language.parse(this.arena, MSG.ERROR_DISABLED));
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        if (aPlayer.getArena() != null) {
            debug(aPlayer.getArena(), player, this.getName());
            throw new GameplayException(Language.parse(this.arena, MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(aPlayer.getArena())));
        }

        return true;
    }

    @Override
    public void commitJoin(final Player player, final ArenaTeam team) {
        // standard join --> fight!
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));

        arenaPlayer.setArena(arena);
        arenaPlayer.setStatus(Status.FIGHT);
        team.add(arenaPlayer);
        final Set<PASpawn> spawns = new HashSet<>();
        if (arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
            final String arenaClass = arenaPlayer.getArenaClass().getName();
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName() + arenaClass + "spawn"));
        } else if (arena.isFreeForAll()) {
            if ("free".equals(team.getName())) {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, "spawn"));
            } else {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName()));
            }
        } else {
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName() + "spawn"));
        }

        int pos = new Random().nextInt(spawns.size());

        for (final PASpawn spawn : spawns) {
            if (--pos < 0) {
                this.arena.tpPlayerToCoordNameForJoin(arenaPlayer, spawn.getName(), true);
                break;
            }
        }

        if (arenaPlayer.getState() == null) {

            final Arena arena = arenaPlayer.getArena();


            arenaPlayer.createState(arenaPlayer.getPlayer());
            ArenaPlayer.backupAndClearInventory(arena, arenaPlayer.getPlayer());
            arenaPlayer.dump();


            if (arenaPlayer.getArenaTeam() != null && arenaPlayer.getArenaClass() == null) {
                final String autoClass = arena.getArenaConfig().getDefinedString(CFG.READY_AUTOCLASS);
                if (autoClass != null && arena.getClass(autoClass) != null) {
                    arena.chooseClass(arenaPlayer.getPlayer(), null, autoClass);
                }
            }
        } else {
            PVPArena.getInstance().getLogger().warning("Player has a state while joining: " + arenaPlayer.getName());
        }

        class RunLater implements Runnable {

            @Override
            public void run() {
                Boolean check = WorkflowManager.handleStart(arena, player, true);
                if (check == null || !check) {
                    Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), this, 10L);
                }
            }

        }

        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 10L);
    }
}
