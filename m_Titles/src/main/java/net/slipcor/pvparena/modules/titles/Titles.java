package net.slipcor.pvparena.modules.titles;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.List;

import static net.slipcor.pvparena.config.Debugger.debug;

public class Titles extends ArenaModule {

    private static final int DEFAULT_TITLE_FADE_IN = 10;
    private static final int DEFAULT_TITLE_STAY = 70;
    private static final int DEFAULT_TITLE_FADE_OUT = 20;

    private ChatColor titlesColor = ChatColor.RESET;

    public Titles() {
        super("Titles");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
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
        for (final TitleType t : TitleType.values()) {
            result.define(new String[]{t.name()});
        }
        return result;
    }

    @Override
    public void initConfig() {
        this.titlesColor = ChatColor.valueOf(this.arena.getConfig().getString(CFG.MODULES_TITLES_COLOR));
    }

    @Override
    public void configParse(YamlConfiguration config) {
        this.initConfig();
    }

    @Override
    public void announce(final String message, final String type) {
        this.announce(TitleType.valueOf(type), message);
    }

    @Override
    public boolean checkCountOverride(Player player, String message) {
        if (this.canSendTitleType(TitleType.COUNT)) {
            this.announce(TitleType.COUNT, message);
            return true;
        }
        return false;
    }

    @Override
    public void parsePlayerDeath(final Player player, final EntityDamageEvent cause) {
        this.announce(TitleType.DEATH, Language.parse(
                MSG.FIGHT_KILLED_BY,
                player.getName(),
                this.arena.parseDeathCause(player, cause.getCause(),
                        ArenaPlayer.getLastDamagingPlayer(cause))));
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage("");
        player.sendMessage("color: " + StringParser.colorVar(this.arena.getConfig().getString(CFG.MODULES_TITLES_COLOR)));
        player.sendMessage(String.format("%s || %s || %s || %s || %s || %s || %s || %s || %s",
                StringParser.colorVar("advert", this.arena.getConfig().getBoolean(CFG.MODULES_TITLES_ADVERT)),
                StringParser.colorVar("join", this.arena.getConfig().getBoolean(CFG.MODULES_TITLES_JOIN)),
                StringParser.colorVar("count", this.arena.getConfig().getBoolean(CFG.MODULES_TITLES_COUNT)),
                StringParser.colorVar("start", this.arena.getConfig().getBoolean(CFG.MODULES_TITLES_START)),
                StringParser.colorVar("end", this.arena.getConfig().getBoolean(CFG.MODULES_TITLES_END)),
                StringParser.colorVar("death", this.arena.getConfig().getBoolean(CFG.MODULES_TITLES_DEATH)),
                StringParser.colorVar("winner", this.arena.getConfig().getBoolean(CFG.MODULES_TITLES_WINNER)),
                StringParser.colorVar("loser", this.arena.getConfig().getBoolean(CFG.MODULES_TITLES_LOSER)),
                StringParser.colorVar("leave", this.arena.getConfig().getBoolean(CFG.MODULES_TITLES_LEAVE))));
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {

        debug(player, "parseJoin ... ");

        if (TeamManager.countPlayersInTeams(this.arena) < 2) {
            final String arenaname = this.arena.getName();
            this.broadcast(TitleType.ADVERT, Language
                    .parse(this.arena, CFG.MSG_STARTING, arenaname + this.titlesColor));
        }

        if (this.arena.isFreeForAll()) {
            this.announce(TitleType.JOIN,
                    this.arena.getConfig().getString(CFG.MSG_PLAYERJOINED)
                            .replace("%1%", player.getName() + this.titlesColor));
        } else {
            this.announce(TitleType.JOIN,
                    this.arena.getConfig().getString(CFG.MSG_PLAYERJOINEDTEAM)
                            .replace("%1%", player.getName() + this.titlesColor)
                            .replace("%2%", team.getColoredName() + this.titlesColor));
        }
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        if (team == null) {
            this.announce(TitleType.LEAVE,
                    Language.parse(MSG.FIGHT_PLAYER_LEFT, player.getName() + this.titlesColor));
        } else {
            this.announce(TitleType.LEAVE,
                    Language.parse(MSG.FIGHT_PLAYER_LEFT, team.colorizePlayer(player) + this.titlesColor));
        }
    }

    @Override
    public void parseStart() {
        this.announce(TitleType.START, Language.parse(MSG.FIGHT_BEGINS));
    }

    private void announce(TitleType t, String message) {
        if (this.canSendTitleType(t)) {
            debug(this.arena, "[announce] type: {} : {}", t.name(), message);

            this.arena.getFighters().forEach(arenaPlayer -> {
                String recoloredMsg = message.replace(ChatColor.WHITE.toString(), this.titlesColor.toString());
                this.sendTitle(arenaPlayer.getPlayer(), recoloredMsg);
            });
        }
    }

    private void broadcast(TitleType t, String message) {
        if (this.canSendTitleType(t)) {
            debug(this.arena, "[announce] type: {} : {}", t.name(), message);

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !this.arena.hasPlayer(p) && !ArenaPlayer.fromPlayer(p).isIgnoringAnnouncements())
                    .forEach(p -> {
                        String recoloredMsg = message.replace(ChatColor.WHITE.toString(), this.titlesColor.toString());
                        this.sendTitle(p, recoloredMsg);
                    });
        }
    }

    /**
     * check the arena for the announcement tyoe
     *
     * @param t the announcement type to check
     * @return true if the arena is configured to send this announcement type,
     * false otherwise
     */
    private boolean canSendTitleType(TitleType t) {
        return this.arena.getConfig().getBoolean(CFG.valueOf("MODULES_TITLES_" + t.name()));
    }

    /**
     * send an announcement to a player
     *
     * @param p       the player to send the message
     * @param message the message to send
     */
    private void sendTitle(Player p, String message) {
        p.sendTitle("", this.titlesColor + message, DEFAULT_TITLE_FADE_IN, DEFAULT_TITLE_STAY, DEFAULT_TITLE_FADE_OUT);
    }
}
