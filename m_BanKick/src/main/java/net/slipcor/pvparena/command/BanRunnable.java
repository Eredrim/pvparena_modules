package net.slipcor.pvparena.command;

import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import static net.slipcor.pvparena.config.Debugger.debug;

class BanRunnable extends BukkitRunnable {
    private final CommandSender admin;
    private final String player;
    private final boolean ban;
    private final BanKick bk;

    public BanRunnable(final BanKick m, final CommandSender admin, final String player, final boolean b) {
        this.bk = m;
        this.admin = admin;
        this.player = player;
        this.ban = b;
        debug(admin, "BanRunnable constructor");
    }

    /**
     * the run method, commit arena end
     */
    @Override
    public void run() {
        debug(this.admin, "BanRunnable commiting");
        if (this.ban) {
            this.bk.doBan(this.admin, this.player);
        } else {
            this.bk.doUnban(this.admin, this.player);
        }
    }
}
