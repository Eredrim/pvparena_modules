package net.slipcor.pvparena.modules.powerups;

import net.slipcor.pvparena.arena.ArenaPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.slipcor.pvparena.modules.powerups.PowerUps.removePowerupItemForPlayer;

/**
 * Runnable which is called each second to make 2 things:
 * - Decrement Powerup items lifetime
 * - Remove items that can't be used anymore
 */
public class PowerupLifetimeRunnable extends BukkitRunnable {
    private final PowerUps module;

    public PowerupLifetimeRunnable(PowerUps module) {
        this.module = module;
    }

    @Override
    public void run() {
        Map<ArenaPlayer, PowerupItem> activePowerUps = this.module.getActivePowerUps();
        List<ArenaPlayer> keysToRemove = new ArrayList<>();
        activePowerUps.forEach((player, puItem) -> {
            puItem.decrementTime();
            if (!puItem.canBeTriggered()) {
                keysToRemove.add(player);
                puItem.removeEffects(player.getPlayer());
                removePowerupItemForPlayer(player);
            }
        });

        keysToRemove.forEach(activePowerUps::remove);
    }
}
