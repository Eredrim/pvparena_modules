package net.slipcor.pvparena.modules.titles;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.Config.CFG;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static net.slipcor.pvparena.config.Debugger.debug;

public final class Title {
    public enum type {
        JOIN, ADVERT, START, END, WINNER, LOSER, PRIZE, CUSTOM, COUNT
    }

    private Title() {

    }

    /**
     * Announce a message to everyone in the arena
     *
     * @param a       the arena from where the announcement should come
     * @param t       the type of announcement
     * @param message the message to announce
     */
    static void announce(final Arena a, final type t, final String message) {
        if (!sendCheck(a, t)) {
            return; // do not send the announcement type
        }
        debug(a, "[announce] type: {} : {}", t.name(), message);

        for (ArenaPlayer arenaPlayer : a.getFighters()) {
            send(a, arenaPlayer.getPlayer(), message.replace(
                    ChatColor.WHITE.toString(),
                    ChatColor.valueOf(
                            a.getArenaConfig().getString(
                                    CFG.MODULES_TITLES_COLOR))
                            .toString()));
        }
    }

    /**
     * check the arena for the announcement tyoe
     *
     * @param a the arena to check
     * @param t the announcement type to check
     * @return true if the arena is configured to send this announcement type,
     * false otherwise
     */
    protected static boolean sendCheck(final Arena a, final type t) {
        final CFG cfg = CFG.valueOf("MODULES_TITLES_" + t.name());
        return a.getArenaConfig().getBoolean(cfg);
    }

    /**
     * send an announcement to a player
     *
     * @param a       the arena sending the announcement
     * @param p       the player to send the message
     * @param message the message to send
     */
    private static void send(final Arena a, final Player p, final String message) {
        p.sendTitle("",
                ChatColor.valueOf(a.getArenaConfig().getString(
                        CFG.MODULES_TITLES_COLOR)) + message);
    }

}
