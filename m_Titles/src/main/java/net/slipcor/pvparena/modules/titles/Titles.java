package net.slipcor.pvparena.modules.titles;

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
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.List;

import static net.slipcor.pvparena.config.Debugger.debug;

public class Titles extends ArenaModule {

    public Titles() {
        super("Titles");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void announce(final String message, final String type) {
        Title.announce(arena, Title.type.valueOf(type), message);
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!tt".equals(s) || s.startsWith("titles");
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("titles");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!tt");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        for (final Title.type t : Title.type.values()) {
            result.define(new String[]{t.name()});
        }
        return result;
    }

    @Override
    public boolean checkCountOverride(Player player, String message) {
        if (Title.sendCheck(arena, Title.type.COUNT)) {
            Title.announce(arena, Title.type.COUNT, message);
            return true;
        }
        return false;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !tt [type]

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, arena)) {
            arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args,
                new Integer[]{2})) {
            return;
        }

        if ("!tt".equals(args[0]) || args[0].startsWith("titles")) {

            for (final Title.type t : Title.type.values()) {
                if (t.name().equalsIgnoreCase(args[1])) {
                    final boolean b = arena.getConfig().getBoolean(
                            CFG.valueOf("MODULES_TITLES_" + t.name()));
                    arena.getConfig().set(
                            CFG.valueOf("MODULES_TITLES_" + t.name()),
                            !b);
                    arena.getConfig().save();

                    arena.msg(
                            sender,
                            Language.parse(MSG.SET_DONE, t.name(),
                                    String.valueOf(!b)));
                    return;
                }
            }

            final String list = StringParser.joinArray(Title.type.values(),
                    ", ");
            arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], list);
        }
    }

    @Override
    public void parsePlayerDeath(final Player player, final EntityDamageEvent cause) {
        Title.announce(arena, Title.type.LOSER, Language.parse(
                MSG.FIGHT_KILLED_BY,
                player.getName(),
                arena.parseDeathCause(player, cause.getCause(),
                        ArenaPlayer.getLastDamagingPlayer(cause))));
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage("");
        player.sendMessage("color: "
                + StringParser.colorVar(arena.getConfig().getString(
                CFG.MODULES_TITLES_COLOR)));
        player.sendMessage(StringParser.colorVar("advert", arena.getConfig()
                .getBoolean(CFG.MODULES_TITLES_ADVERT))
                + " || "
                + StringParser.colorVar("count", arena.getConfig()
                .getBoolean(CFG.MODULES_TITLES_COUNT))
                + " || "
                + StringParser.colorVar("custom", arena.getConfig()
                .getBoolean(CFG.MODULES_TITLES_CUSTOM))
                + " || "
                + StringParser.colorVar("end", arena.getConfig()
                .getBoolean(CFG.MODULES_TITLES_END))
                + " || "
                + StringParser.colorVar("join", arena.getConfig()
                .getBoolean(CFG.MODULES_TITLES_JOIN))
                + " || "
                + StringParser.colorVar("loser", arena.getConfig()
                .getBoolean(CFG.MODULES_TITLES_LOSER))
                + " || "
                + StringParser.colorVar("prize", arena.getConfig()
                .getBoolean(CFG.MODULES_TITLES_PRIZE))
                + " || "
                + StringParser.colorVar("start", arena.getConfig()
                .getBoolean(CFG.MODULES_TITLES_START))
                + " || "
                + StringParser.colorVar("winner", arena.getConfig()
                .getBoolean(CFG.MODULES_TITLES_WINNER)));
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {

        debug(player, "parseJoin ... ");

        if (TeamManager.countPlayersInTeams(arena) < 2) {
            final String arenaname =
                    PermissionManager.hasOverridePerm(player) ? arena.getName() : ArenaManager.getIndirectArenaName(arena);
            Title.announce(arena, Title.type.ADVERT, Language
                    .parse(arena, CFG.MSG_STARTING, arenaname +
                            ChatColor.valueOf(arena.getConfig().getString(
                                    CFG.MODULES_TITLES_COLOR))));
        }

        if (arena.isFreeForAll()) {
            Title.announce(arena, Title.type.JOIN,
                    arena.getConfig().getString(CFG.MSG_PLAYERJOINED)
                            .replace("%1%", player.getName() +
                                    ChatColor.valueOf(arena.getConfig().getString(
                                            CFG.MODULES_TITLES_COLOR))));
        } else {
            Title.announce(
                    arena,
                    Title.type.JOIN,
                    arena.getConfig().getString(CFG.MSG_PLAYERJOINEDTEAM)
                            .replace("%1%", player.getName() +
                                    ChatColor.valueOf(arena.getConfig().getString(
                                            CFG.MODULES_TITLES_COLOR)))
                            .replace("%2%", team.getColoredName() +
                                    ChatColor.valueOf(arena.getConfig().getString(
                                            CFG.MODULES_TITLES_COLOR))));
        }
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        if (team == null) {
            Title.announce(arena, Title.type.LOSER,
                    Language.parse(MSG.FIGHT_PLAYER_LEFT, player.getName() +
                            ChatColor.valueOf(arena.getConfig().getString(
                                    CFG.MODULES_TITLES_COLOR))));
        } else {
            Title.announce(
                    arena,
                    Title.type.LOSER,
                    Language.parse(MSG.FIGHT_PLAYER_LEFT,
                            team.colorizePlayer(player) +
                                    ChatColor.valueOf(arena.getConfig().getString(
                                            CFG.MODULES_TITLES_COLOR))));
        }
    }

    @Override
    public void parseStart() {
        Title.announce(arena, Title.type.START,
                Language.parse(MSG.FIGHT_BEGINS));
    }
}
