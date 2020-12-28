package net.slipcor.pvparena.modules.announcements;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.modules.WarmupJoin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.List;

import static net.slipcor.pvparena.config.Debugger.debug;

public class AnnouncementManager extends ArenaModule {

    public AnnouncementManager() {
        super("Announcements");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void announce(final String message, final String type) {
        Announcement.announce(arena, Announcement.type.valueOf(type), message);
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!aa".equals(s) || s.startsWith("announce");
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("announce");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!aa");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        for (final Announcement.type t : Announcement.type.values()) {
            result.define(new String[]{t.name()});
        }
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !aa [type]

        if (!PVPArena.hasAdminPerms(sender)
                && !PVPArena.hasCreatePerms(sender, arena)) {
            arena.msg(
                    sender,
                    Language.parse(MSG.ERROR_NOPERM,
                            Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args,
                new Integer[]{2})) {
            return;
        }

        if ("!aa".equals(args[0]) || args[0].startsWith("announce")) {

            for (final Announcement.type t : Announcement.type.values()) {
                if (t.name().equalsIgnoreCase(args[1])) {
                    final boolean b = arena.getArenaConfig().getBoolean(
                            CFG.valueOf("MODULES_ANNOUNCEMENTS_" + t.name()));
                    arena.getArenaConfig().set(
                            CFG.valueOf("MODULES_ANNOUNCEMENTS_" + t.name()),
                            !b);
                    arena.getArenaConfig().save();

                    arena.msg(
                            sender,
                            Language.parse(MSG.SET_DONE, t.name(),
                                    String.valueOf(!b)));
                    return;
                }
            }

            final String list = StringParser.joinArray(Announcement.type.values(),
                    ", ");
            arena.msg(sender, Language.parse(MSG.ERROR_ARGUMENT, args[1], list));
        }
    }

    @Override
    public void parsePlayerDeath(final Player player, final EntityDamageEvent cause) {
        Announcement.announce(arena, Announcement.type.LOSER, Language.parse(
                MSG.FIGHT_KILLED_BY,
                player.getName(),
                arena.parseDeathCause(player, cause.getCause(),
                        ArenaPlayer.getLastDamagingPlayer(cause, player))));
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage("");
        player.sendMessage("radius: "
                + StringParser.colorVar(arena.getArenaConfig().getInt(
                CFG.MODULES_ANNOUNCEMENTS_RADIUS, 0))
                + " || color: "
                + StringParser.colorVar(arena.getArenaConfig().getString(
                CFG.MODULES_ANNOUNCEMENTS_COLOR)));
        player.sendMessage(StringParser.colorVar("advert", arena.getArenaConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_ADVERT))
                + " || "
                + StringParser.colorVar("custom", arena.getArenaConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_CUSTOM))
                + " || "
                + StringParser.colorVar("end", arena.getArenaConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_END))
                + " || "
                + StringParser.colorVar("join", arena.getArenaConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_JOIN))
                + " || "
                + StringParser.colorVar("loser", arena.getArenaConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_LOSER))
                + " || "
                + StringParser.colorVar("prize", arena.getArenaConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_PRIZE))
                + " || "
                + StringParser.colorVar("start", arena.getArenaConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_START))
                + " || "
                + StringParser.colorVar("winner", arena.getArenaConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_WINNER)));
    }

    @Override
    public void parseJoin(final CommandSender sender, final ArenaTeam team) {

        debug(sender, "parseJoin ... ");
        ArenaPlayer ap = ArenaPlayer.parsePlayer(sender.getName());
        if (ap.getStatus() == ArenaPlayer.Status.WARM || !WarmupJoin.didNotAnnounceYet(arena)) {
            debug(sender, "skipping because we already did!");
            return;
        }

        if (TeamManager.countPlayersInTeams(arena) < 2) {
            final String arenaname =
                    PVPArena.hasOverridePerms(sender) ? arena.getName() : ArenaManager.getIndirectArenaName(arena);
            Announcement.announce(arena, Announcement.type.ADVERT, Language
                    .parse(arena, CFG.MSG_STARTING, arenaname +
                            ChatColor.valueOf(arena.getArenaConfig().getString(
                                    CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        }

        if (arena.isFreeForAll()) {
            Announcement.announce(arena, Announcement.type.JOIN,
                    arena.getArenaConfig().getString(CFG.MSG_PLAYERJOINED)
                            .replace("%1%", sender.getName() +
                                    ChatColor.valueOf(arena.getArenaConfig().getString(
                                            CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        } else {
            Announcement.announce(
                    arena,
                    Announcement.type.JOIN,
                    arena.getArenaConfig().getString(CFG.MSG_PLAYERJOINEDTEAM)
                            .replace("%1%", sender.getName() +
                                    ChatColor.valueOf(arena.getArenaConfig().getString(
                                            CFG.MODULES_ANNOUNCEMENTS_COLOR)))
                            .replace("%2%", team.getColoredName() +
                                    ChatColor.valueOf(arena.getArenaConfig().getString(
                                            CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        }
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        if (team == null) {
            Announcement.announce(arena, Announcement.type.LOSER,
                    Language.parse(MSG.FIGHT_PLAYER_LEFT, player.getName() +
                            ChatColor.valueOf(arena.getArenaConfig().getString(
                                    CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        } else {
            Announcement.announce(
                    arena,
                    Announcement.type.LOSER,
                    Language.parse(MSG.FIGHT_PLAYER_LEFT,
                            team.colorizePlayer(player) +
                                    ChatColor.valueOf(arena.getArenaConfig().getString(
                                            CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        }
    }

    @Override
    public void parseStart() {
        Announcement.announce(arena, Announcement.type.START,
                Language.parse(MSG.FIGHT_BEGINS));
    }
}
