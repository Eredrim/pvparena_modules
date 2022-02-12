package net.slipcor.pvparena.command;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BanKick extends ArenaModule {
    private static final List<String> commands = new ArrayList<>();

    static {
        commands.add("ban");
        commands.add("kick");
        commands.add("tempban");
        commands.add("unban");
        commands.add("tempunban");
    }

    public BanKick() {
        super("BanKick");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    private List<String> banList;

    @Override
    public boolean checkCommand(final String s) {
        return commands.contains(s.toLowerCase());
    }

    @Override
    public List<String> getMain() {
        return commands;
    }

    @Override
    public List<String> getShort() {
        return new ArrayList<>();
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"kick", "{Player}"});
        result.define(new String[]{"tempban", "{Player}"});
        result.define(new String[]{"ban", "{Player}"});
        result.define(new String[]{"unban"});
        result.define(new String[]{"tempunban"});
        return result;
    }

    @Override
    public void checkJoin(Player player) throws GameplayException {
        if (this.getBans().contains(player.getName())) {
            throw new GameplayException(Language.parse(MSG.MODULE_BANVOTE_YOUBANNED, this.arena.getName()));
        }
    }

    @Override
    public void checkSpectate(Player player) throws GameplayException {
        this.checkJoin(player);
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {

        if (!commands.contains(args[0].toLowerCase())) {
            return;
        }

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

		/*
        /pa [arenaname] kick [player]
        /pa [arenaname] tempban [player] [timediff*]
        /pa [arenaname] ban [player]
        /pa [arenaname] unban [player]
        /pa [arenaname] tempunban [player] [timediff*]
		 */

        final String cmd = args[0].toLowerCase();

        final Player p = Bukkit.getPlayer(args[1]);
        if (p != null) {
            args[1] = p.getName();
        }

        if ("kick".equals(cmd)) {
            if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
                return;
            }
            this.tryKick(sender, args[1]);
        } else if ("tempban".equals(cmd)) {
            if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3})) {
                return;
            }
            this.tryKick(sender, args[1]);
            final long time = this.parseStringToSeconds(args[2]);
            final BanRunnable run = new BanRunnable(this, sender, args[1], false);
            Bukkit.getScheduler().runTaskLaterAsynchronously(PVPArena.getInstance(), run, 20 * time);
            this.doBan(sender, args[1]);
        } else if ("ban".equals(cmd)) {
            if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
                return;
            }
            this.tryKick(sender, args[1]);
            this.doBan(sender, args[1]);
        } else if ("unban".equals(cmd)) {
            if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
                return;
            }
            this.doUnBan(sender, args[1]);
        } else if ("tempunban".equals(cmd)) {
            if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3})) {
                return;
            }
            final long time = this.parseStringToSeconds(args[2]);
            final BanRunnable run = new BanRunnable(this, sender, args[1], true);
            Bukkit.getScheduler().runTaskLaterAsynchronously(PVPArena.getInstance(), run, 20 * time);
            this.doUnBan(sender, args[1]);
        }
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        final List<String> lBans = config.getStringList("bans");

        final Set<String> hsBans = new HashSet<>();


        for (final String s : lBans) {
            hsBans.add(s);
        }

        this.getBans().clear();
        for (final String s : hsBans) {
            this.getBans().add(s);
        }
    }

    private List<String> getBans() {
        if (this.banList == null) {
            this.banList = new ArrayList<>();
        }
        return this.banList;
    }

    void doBan(final CommandSender admin, final String player) {
        this.getBans().add(player);
        if (admin != null) {
            this.arena.msg(admin, MSG.MODULE_BANVOTE_BANNED, player);
        }
        this.tryNotify(Language.parse(MSG.MODULE_BANVOTE_YOUBANNED, this.arena.getName()));
        this.arena.getConfig().setManually("bans", this.getBans());
        this.arena.getConfig().save();
    }

    void doUnBan(final CommandSender admin, final String player) {
        this.getBans().remove(player);
        if (admin != null) {
            this.arena.msg(admin, MSG.MODULE_BANVOTE_UNBANNED, player);
        }
        this.tryNotify(Language.parse(MSG.MODULE_BANVOTE_YOUBANNED, this.arena.getName()));
        this.arena.getConfig().setManually("bans", this.getBans());
        this.arena.getConfig().save();
    }

    private long parseStringToSeconds(final String string) {
        String input = "";

        int pos = 0;
        char type = 's';

        while (pos < string.length()) {
            final Character c = string.charAt(pos);

            try {
                final int i = Integer.parseInt(String.valueOf(c));
                input += String.valueOf(i);
            } catch (final Exception e) {
                if (c == '.' || c == ',') {
                    input += ".";
                } else {
                    type = c;
                    break;
                }
            }

            pos++;
        }

        float time = Float.parseFloat(input);

        if (type == 'd') {
            time *= 24;
            type = 'h';
        }
        if (type == 'h') {
            time *= 60;
            type = 'm';
        }
        if (type == 'm') {
            time *= 60;
        }

        return (long) time;
    }

    private void tryKick(final CommandSender sender, final String string) {
        final Player p = Bukkit.getPlayer(string);
        if (p == null) {
            this.arena.msg(sender, MSG.MODULE_BANVOTE_NOTKICKED, string);
            return;
        }
        this.arena.playerLeave(p, CFG.TP_EXIT, true, true, false);
        this.arena.msg(p, MSG.MODULE_BANVOTE_YOUKICKED, this.arena.getName());
        this.arena.msg(sender, MSG.MODULE_BANVOTE_KICKED, string);
    }

    private void tryNotify(final String string) {
        final Player p = Bukkit.getPlayer(string);
        if (p == null) {
            return;
        }
        this.arena.msg(p, string);
    }
	/*
/pa tempban [player] [timediff*]                             <----- This means banning the Player temporary from ALL Arenas!
/pa ban [player]                                                     <----- The Player can't play PVP-Arena anymore. He is banned from ALL Arenas!
/pa unban [player]                                                 <----- Unbans a Player from ALL Arenas!
/pa tempunban [player] [timediff*]                         <----- Unbans a Player temporary from ALL Arenas!
	 */
}
