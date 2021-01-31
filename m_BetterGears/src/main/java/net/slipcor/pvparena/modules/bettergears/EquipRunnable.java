package net.slipcor.pvparena.modules.bettergears;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import org.bukkit.Bukkit;

class EquipRunnable implements Runnable {

    private final ArenaPlayer p;
    private final BetterGears m;

    public EquipRunnable(final ArenaPlayer ap, final BetterGears mod) {
        p = ap;
        m = mod;
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), this, 65L);
    }

    @Override
    public void run() {
        m.equip(p);
    }

}
