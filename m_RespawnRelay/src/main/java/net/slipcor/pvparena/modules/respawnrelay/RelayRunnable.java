package net.slipcor.pvparena.modules.respawnrelay;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.runnables.ArenaRunnable;
import net.slipcor.pvparena.runnables.InventoryRefillRunnable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static net.slipcor.pvparena.config.Debugger.debug;

public class RelayRunnable extends ArenaRunnable {
    private final ArenaPlayer ap;
    private final PADeathInfo deathInfo;
    private final List<ItemStack> keptItems;
    private final RespawnRelay mod;

    public RelayRunnable(RespawnRelay relay, Arena arena, ArenaPlayer arenaPlayer, PADeathInfo deathInfo, List<ItemStack> keptItems) {
        super(MSG.MODULE_RESPAWNRELAY_RESPAWNING.getNode(), arena.getConfig().getInt(CFG.MODULES_RESPAWNRELAY_INTERVAL), arenaPlayer.getPlayer(), null, false);
        this.mod = relay;
        this.ap = arenaPlayer;
        this.deathInfo = deathInfo;
        this.keptItems = keptItems;
    }

    @Override
    protected void commit() {
        debug(this.ap, "RelayRunnable commiting");

        Player maybePlayer = this.ap.getPlayer();

        if (!this.ap.getPlayer().isOnline()) {
                PVPArena.getInstance().getLogger().warning("player offline: " + this.ap.getName());
                return;
        }

        if (this.ap.getArena() == null) {
            return;
        }

        new InventoryRefillRunnable(this.ap.getArena(), maybePlayer, this.keptItems);
        final String spawn = this.mod.getSpawnPointOverrideMap().get(this.ap.getName());
        SpawnManager.respawn(this.ap, spawn);

        this.ap.revive(this.deathInfo);
        if(this.ap.getStatus() == PlayerStatus.DEAD) { // Necessary to keep players of teams that lost in lost status
            this.ap.setStatus(PlayerStatus.FIGHT);
        }
        this.mod.getRunnerMap().remove(this.ap.getName());
        this.mod.getSpawnPointOverrideMap().remove(this.ap.getName());
    }

    @Override
    protected void warn() {
        PVPArena.getInstance().getLogger().warning("RelayRunnable not scheduled yet!");
    }
}
