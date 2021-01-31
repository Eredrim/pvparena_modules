package net.slipcor.pvparena.modules.skins;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class LibsDisguiseRunnable extends BukkitRunnable implements Runnable {
    private final Disguise disguise;
    private final Player player;

    public LibsDisguiseRunnable(Player player, Disguise disguise) {
        this.player = player;
        this.disguise = disguise;
    }

    @Override
    public void run() {
        DisguiseAPI.disguiseEntity(player, disguise);
    }
}
