package net.slipcor.pvparena.modules.announcements;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.PermissionManager;
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
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void announce(final String message, final String type) {
        Announcement.announce(this.arena, Announcement.type.valueOf(type), message);
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

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args,
                new Integer[]{2})) {
            return;
        }

        if ("!aa".equals(args[0]) || args[0].startsWith("announce")) {

            for (final Announcement.type t : Announcement.type.values()) {
                if (t.name().equalsIgnoreCase(args[1])) {
                    final boolean b = this.arena.getConfig().getBoolean(
                            CFG.valueOf("MODULES_ANNOUNCEMENTS_" + t.name()));
                    this.arena.getConfig().set(
                            CFG.valueOf("MODULES_ANNOUNCEMENTS_" + t.name()),
                            !b);
                    this.arena.getConfig().save();

                    this.arena.msg(sender, MSG.SET_DONE, t.name(), String.valueOf(!b));
                    return;
                }
            }

            final String list = StringParser.joinArray(Announcement.type.values(),
                    ", ");
            this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], list);
        }
    }

    @Override
    public void parsePlayerDeath(final Player player, final EntityDamageEvent cause) {
        Announcement.announce(this.arena, Announcement.type.LOSER, Language.parse(
                MSG.FIGHT_KILLED_BY,
                player.getName(),
                this.arena.parseDeathCause(player, cause.getCause(),
                        ArenaPlayer.getLastDamagingPlayer(cause))));
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage("");
        player.sendMessage("radius: "
                + StringParser.colorVar(this.arena.getConfig().getInt(
                CFG.MODULES_ANNOUNCEMENTS_RADIUS, 0))
                + " || color: "
                + StringParser.colorVar(this.arena.getConfig().getString(
                CFG.MODULES_ANNOUNCEMENTS_COLOR)));
        player.sendMessage(StringParser.colorVar("advert", this.arena.getConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_ADVERT))
                + " || "
                + StringParser.colorVar("custom", this.arena.getConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_CUSTOM))
                + " || "
                + StringParser.colorVar("end", this.arena.getConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_END))
                + " || "
                + StringParser.colorVar("join", this.arena.getConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_JOIN))
                + " || "
                + StringParser.colorVar("loser", this.arena.getConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_LOSER))
                + " || "
                + StringParser.colorVar("prize", this.arena.getConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_PRIZE))
                + " || "
                + StringParser.colorVar("start", this.arena.getConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_START))
                + " || "
                + StringParser.colorVar("winner", this.arena.getConfig()
                .getBoolean(CFG.MODULES_ANNOUNCEMENTS_WINNER)));
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {

        debug(player, "parseJoin ... ");
        ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
        if (ap.getStatus() == PlayerStatus.WARM || !WarmupJoin.didNotAnnounceYet(this.arena)) {
            debug(player, "skipping because we already did!");
            return;
        }

        if (TeamManager.countPlayersInTeams(this.arena) < 2) {
            final String arenaname =
                    PermissionManager.hasOverridePerm(player) ? this.arena.getName() : ArenaManager.getIndirectArenaName(this.arena);
            Announcement.announce(this.arena, Announcement.type.ADVERT, Language
                    .parse(this.arena, CFG.MSG_STARTING, arenaname +
                            ChatColor.valueOf(this.arena.getConfig().getString(
                                    CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        }

        if (this.arena.isFreeForAll()) {
            Announcement.announce(this.arena, Announcement.type.JOIN,
                    this.arena.getConfig().getString(CFG.MSG_PLAYERJOINED)
                            .replace("%1%", player.getName() +
                                    ChatColor.valueOf(this.arena.getConfig().getString(
                                            CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        } else {
            Announcement.announce(
                    this.arena,
                    Announcement.type.JOIN,
                    this.arena.getConfig().getString(CFG.MSG_PLAYERJOINEDTEAM)
                            .replace("%1%", player.getName() +
                                    ChatColor.valueOf(this.arena.getConfig().getString(
                                            CFG.MODULES_ANNOUNCEMENTS_COLOR)))
                            .replace("%2%", team.getColoredName() +
                                    ChatColor.valueOf(this.arena.getConfig().getString(
                                            CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        }
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        if (team == null) {
            Announcement.announce(this.arena, Announcement.type.LOSER,
                    Language.parse(MSG.FIGHT_PLAYER_LEFT, player.getName() +
                            ChatColor.valueOf(this.arena.getConfig().getString(
                                    CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        } else {
            Announcement.announce(
                    this.arena,
                    Announcement.type.LOSER,
                    Language.parse(MSG.FIGHT_PLAYER_LEFT,
                            team.colorizePlayer(player) +
                                    ChatColor.valueOf(this.arena.getConfig().getString(
                                            CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        }
    }

    @Override
    public void parseStart() {
        Announcement.announce(this.arena, Announcement.type.START,
                Language.parse(MSG.FIGHT_BEGINS));
    }
}
