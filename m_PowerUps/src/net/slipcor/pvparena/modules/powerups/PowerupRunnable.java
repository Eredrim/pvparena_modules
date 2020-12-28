package net.slipcor.pvparena.modules.powerups;

import org.bukkit.Bukkit;

import static net.slipcor.pvparena.config.Debugger.trace;

class PowerupRunnable implements Runnable {
    private final PowerupManager pum;

    /**
     * construct a powerup spawn runnable
     *
     * @param pm the module instance
     */
    public PowerupRunnable(final PowerupManager pm) {
        pum = pm;
        trace("PowerupRunnable constructor");
    }

    /**
     * the run method, spawn a powerup
     */
    @Override
    public void run() {
        trace("PowerupRunnable commiting spawn");
        if (pum.getArena().isLocked()) {
            // deactivate the auto saving task
            Bukkit.getServer().getScheduler().cancelTask(pum.SPAWN_ID);
        } else {

            pum.calcPowerupSpawn();
        }
    }
}
