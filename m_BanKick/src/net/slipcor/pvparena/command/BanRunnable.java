package net.slipcor.pvparena.command;

import org.bukkit.command.CommandSender;

import static net.slipcor.pvparena.config.Debugger.debug;

class BanRunnable implements Runnable {
    private final CommandSender admin;
    private final String player;
    private final boolean ban;
    private final BanKick bk;

    public BanRunnable(final BanKick m, final CommandSender admin, final String p, final boolean b) {
        this.bk = m;
        this.admin = admin;
        this.player = p;
        this.ban = b;
        debug(admin, "BanRunnable constructor");
    }

    /**
     * the run method, commit arena end
     */
    @Override
    public void run() {
        debug(admin, "BanRunnable commiting");
        if (ban) {
            bk.doBan(admin, player);
        } else {
            bk.doUnBan(admin, player);
        }
    }
}
