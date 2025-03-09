package net.slipcor.pvparena.modules.realspectate.runnables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.managers.InventoryManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class InitSpectateRunnable extends BukkitRunnable {
    private final Player spectator;
    private final Player fighter;

    public InitSpectateRunnable(Player spectator, Player fighter) {
        this.spectator = spectator;
        this.fighter = fighter;
    }

    @Override
    public void run() {
        this.spectator.setHealth(this.fighter.getHealth() > 0 ? this.fighter.getHealth() : 1);

        InventoryManager.clearInventory(this.spectator);
        this.spectator.getInventory().setContents(this.fighter.getInventory().getContents());

        this.spectator.teleport(this.fighter.getLocation());

        this.spectator.hidePlayer(PVPArena.getInstance(), this.fighter);
    }
}
