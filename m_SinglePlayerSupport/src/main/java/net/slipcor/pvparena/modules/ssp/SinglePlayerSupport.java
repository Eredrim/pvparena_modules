package net.slipcor.pvparena.modules.ssp;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeleportManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
            throw new GameplayException(Language.parse(MSG.ERROR_DISABLED));
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        if (aPlayer.getArena() != null) {
            debug(aPlayer.getArena(), player, this.getName());
            throw new GameplayException(Language.parse(MSG.ERROR_ARENA_ALREADY_PART_OF, aPlayer.getArena().getName()));
        }

        return true;
    }

    @Override
    public void commitJoin(final Player player, final ArenaTeam team) {
        // standard join --> fight!
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));

        arenaPlayer.setArena(arena);
        arenaPlayer.setStatus(PlayerStatus.FIGHT);
        team.add(arenaPlayer);

        TeleportManager.teleportPlayerToSpawnForJoin(this.arena, arenaPlayer,
                SpawnManager.selectSpawnsForPlayer(this.arena, arenaPlayer, PASpawn.FIGHT), true);

        if (arenaPlayer.getState() == null) {

            // Important: clear inventory before setting player state to deal with armor modifiers (like health)
            ArenaPlayer.backupAndClearInventory(this.arena, player);
            arenaPlayer.createState(player);
            arenaPlayer.dump();

            if (arenaPlayer.getArenaTeam() != null && arenaPlayer.getArenaClass() == null) {
                String autoClassCfg = this.arena.getConfig().getDefinedString(CFG.READY_AUTOCLASS);
                if (autoClassCfg != null) {
                    this.arena.getAutoClass(autoClassCfg, arenaPlayer.getArenaTeam()).ifPresent(autoClass ->
                            this.arena.chooseClass(player, null, autoClass)
                    );
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
