package net.slipcor.pvparena.modules.vaultsupport;

import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.events.PADeathEvent;
import net.slipcor.pvparena.events.PAKillEvent;
import net.slipcor.pvparena.events.goal.PAGoalScoreEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class VaultListener implements Listener {
    private final VaultSupport modInstance;

    public VaultListener(VaultSupport modInstance) {
        this.modInstance = modInstance;
    }

    @EventHandler
    public void onPADeathEvent(PADeathEvent event) {
        if(this.modInstance.getArena().equals(event.getArena())) {
            this.modInstance.rewardPlayerForAction(event.getPlayer(), Config.CFG.MODULES_VAULT_REWARD_DEATH);
        }
    }

    @EventHandler
    public void onPAKillEvent(PAKillEvent event) {
        if(this.modInstance.getArena().equals(event.getArena())) {
            this.modInstance.rewardPlayerForAction(event.getPlayer(), Config.CFG.MODULES_VAULT_REWARD_KILL);
        }
    }

    @EventHandler
    public void onPAScoreEvent(PAGoalScoreEvent event) {
        if(this.modInstance.getArena().equals(event.getArena()) && event.getArenaPlayer() != null) {
            this.modInstance.rewardPlayerForAction(event.getArenaPlayer().getPlayer(), Config.CFG.MODULES_VAULT_REWARD_SCORE);
        }
    }
}
