package net.slipcor.pvparena.modules.powerups;

import org.bukkit.scheduler.BukkitRunnable;

import static net.slipcor.pvparena.config.Debugger.trace;

public class PowerupSpawnRunnable extends BukkitRunnable {
    private final PowerUps module;

    /**
     * construct a powerup spawn runnable
     *
     * @param module the module instance
     */
    public PowerupSpawnRunnable(PowerUps module) {
        this.module = module;
        trace(this.module.getArena(), this.module, "PowerupRunnable constructor");
    }

    /**
     * the run method, spawn a powerup
     */
    @Override
    public void run() {
        trace(this.module.getArena(), this.module, "PowerupRunnable commiting spawn");
        if (this.module.getArena().isLocked()) {
            // deactivate the auto saving task
            this.cancel();
        } else {
            this.module.calcPowerupSpawn();
        }
    }
}
