package net.slipcor.pvparena.modules.announcements;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
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
import static net.slipcor.pvparena.modules.announcements.Announcement.type.*;

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

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
            return;
        }

        if ("!aa".equals(args[0]) || args[0].startsWith("announce")) {

            for (final Announcement.type t : Announcement.type.values()) {
                if (t.name().equalsIgnoreCase(args[1])) {
                    CFG cfgEntry = CFG.valueOf("MODULES_ANNOUNCEMENTS_" + t.name());
                    final boolean b = this.arena.getConfig().getBoolean(cfgEntry);
                    this.arena.getConfig().set(cfgEntry, !b);
                    this.arena.getConfig().save();

                    this.arena.msg(sender, MSG.SET_DONE, t.name(), String.valueOf(!b));
                    return;
                }
            }

            final String list = StringParser.joinArray(Announcement.type.values(), ", ");
            this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], list);
        }
    }

    @Override
    public void parsePlayerDeath(final Player player, final EntityDamageEvent cause) {
        String deathCause = this.arena.parseDeathCause(player, cause.getCause(), ArenaPlayer.getLastDamagingPlayer(cause));
        Announcement.announce(this.arena, LOSER, Language.parse(MSG.FIGHT_KILLED_BY, player.getName(), deathCause));
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage("");
        Config cfg = this.arena.getConfig();
        player.sendMessage(String.format("radius: %s || color: %s",
                StringParser.colorVar(cfg.getInt(CFG.MODULES_ANNOUNCEMENTS_RADIUS, 0)),
                StringParser.colorVar(cfg.getString(CFG.MODULES_ANNOUNCEMENTS_COLOR))));
        player.sendMessage(String.format("%s || %s || %s || %s || %s || %s || %s || %s",
                StringParser.colorVar("advert", cfg.getBoolean(CFG.MODULES_ANNOUNCEMENTS_ADVERT)),
                StringParser.colorVar("custom", cfg.getBoolean(CFG.MODULES_ANNOUNCEMENTS_CUSTOM)),
                StringParser.colorVar("end", cfg.getBoolean(CFG.MODULES_ANNOUNCEMENTS_END)),
                StringParser.colorVar("join", cfg.getBoolean(CFG.MODULES_ANNOUNCEMENTS_JOIN)),
                StringParser.colorVar("loser", cfg.getBoolean(CFG.MODULES_ANNOUNCEMENTS_LOSER)),
                StringParser.colorVar("prize", cfg.getBoolean(CFG.MODULES_ANNOUNCEMENTS_PRIZE)),
                StringParser.colorVar("start", cfg.getBoolean(CFG.MODULES_ANNOUNCEMENTS_START)),
                StringParser.colorVar("winner", cfg.getBoolean(CFG.MODULES_ANNOUNCEMENTS_WINNER))));
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {

        debug(player, "parseJoin ... ");
        ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
        if (ap.getStatus() == PlayerStatus.WARM || !WarmupJoin.didNotAnnounceYet(this.arena)) {
            debug(player, "skipping because we already did!");
            return;
        }

        String color = this.arena.getConfig().getString(CFG.MODULES_ANNOUNCEMENTS_COLOR);
        if (TeamManager.countPlayersInTeams(this.arena) < 2) {
            final String arenaName = this.arena.getName();
            Announcement.announce(this.arena, ADVERT, Language.parse(this.arena, CFG.MSG_STARTING, arenaName + ChatColor.valueOf(color)));
        }

        if (this.arena.isFreeForAll()) {
            Announcement.announce(this.arena, JOIN, Language.parse(this.arena, CFG.MSG_PLAYERJOINED, player.getName() + ChatColor.valueOf(color)));
        } else {
            String msg = Language.parse(this.arena, CFG.MSG_PLAYERJOINEDTEAM, player.getName() + ChatColor.valueOf(color), team.getColoredName() + ChatColor.valueOf(color));
            Announcement.announce(this.arena, JOIN, msg);
        }
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        String color = this.arena.getConfig().getString(CFG.MODULES_ANNOUNCEMENTS_COLOR);
        if (team == null) {
            Announcement.announce(this.arena, LOSER, Language.parse(MSG.FIGHT_PLAYER_LEFT, player.getName() + ChatColor.valueOf(color)));
        } else {
            Announcement.announce(this.arena, LOSER, Language.parse(MSG.FIGHT_PLAYER_LEFT, team.colorizePlayer(player) + ChatColor.valueOf(color)));
        }
    }

    @Override
    public void parseStart() {
        Announcement.announce(this.arena, START, Language.parse(MSG.FIGHT_BEGINS));
    }
}
