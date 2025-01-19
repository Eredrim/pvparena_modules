package net.slipcor.pvparena.command;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
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

import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.*;

public class BanKick extends ArenaModule {
    private static final List<String> COMMANDS = asList(
            "ban",
            "kick",
            "tempban",
            "unban",
            "tempunban"
    );

    public BanKick() {
        super("BanKick");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    private Set<String> permBanList = new HashSet<>();
    private Set<String> tempBanList = new HashSet<>();
    private Set<String> tempUnbanList = new HashSet<>();

    @Override
    public boolean checkCommand(final String s) {
        return COMMANDS.contains(s.toLowerCase());
    }

    @Override
    public List<String> getMain() {
        return COMMANDS;
    }

    @Override
    public List<String> getShort() {
        return new ArrayList<>();
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{Player}"});
        return result;
    }

    @Override
    public void checkJoin(Player player) throws GameplayException {
        if (this.tempBanList.contains(player.getName()) ||
                (this.permBanList.contains(player.getName()) && !this.tempUnbanList.contains(player.getName()))) {
            throw new GameplayException(Language.parse(MSG.MODULE_BANKICK_YOUBANNED, this.arena.getName()));
        }
    }

    @Override
    public void checkSpectate(Player player) throws GameplayException {
        this.checkJoin(player);
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {

        if (!COMMANDS.contains(args[0].toLowerCase())) {
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

        String cmd = args[0].toLowerCase();
        Player player = Bukkit.getPlayer(args[1]);
        String playerName;
        if (player != null) {
            playerName = player.getName();
        } else {
            this.arena.msg(sender, MSG.MODULE_BANKICK_NOTONLINE, args[1]);
            return;
        }

        switch (cmd) {
            case "kick":
                if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
                    return;
                }
                this.tryKick(sender, playerName);
                break;
            case "tempban": {
                if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3})) {
                    return;
                }
                this.tryKick(sender, playerName);
                long time = this.parseStringToSeconds(args[2]);
                BanRunnable unbanTask = new BanRunnable(this, sender, playerName, false);
                unbanTask.runTaskLaterAsynchronously(PVPArena.getInstance(), 20 * time);
                this.doTempBan(sender, playerName);
                break;
            }
            case "ban":
                if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
                    return;
                }
                this.tryKick(sender, playerName);
                this.doBan(sender, playerName);
                break;
            case "unban":
                if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
                    return;
                }
                this.doUnban(sender, playerName);
                break;
            case "tempunban": {
                if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3})) {
                    return;
                }
                long time = this.parseStringToSeconds(args[2]);
                BanRunnable banTask = new BanRunnable(this, sender, playerName, true);
                banTask.runTaskLaterAsynchronously(PVPArena.getInstance(), 20 * time);
                this.doTempUnban(sender, playerName);
                break;
            }
        }
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        this.permBanList = new HashSet<>(config.getStringList("bans"));
    }

    private List<String> getPermBanList() {
        return new ArrayList<>(this.permBanList);
    }

    void doBan(final CommandSender admin, final String player) {
        this.permBanList.add(player);
        this.tempUnbanList.remove(player);
        if (admin != null) {
            this.arena.msg(admin, MSG.MODULE_BANKICK_BANNED, player);
        }
        this.tryNotify(Language.parse(MSG.MODULE_BANKICK_YOUBANNED, this.arena.getName()));
        this.arena.getConfig().setManually("bans", this.getPermBanList());
        this.arena.getConfig().save();
    }

    void doTempBan(final CommandSender admin, final String player) {
        this.tempBanList.add(player);
        if (admin != null) {
            this.arena.msg(admin, MSG.MODULE_BANKICK_BANNED, player);
        }
        this.tryNotify(Language.parse(MSG.MODULE_BANKICK_YOUBANNED, this.arena.getName()));
    }

    void doUnban(final CommandSender admin, final String player) {
        this.permBanList.remove(player);
        this.tempBanList.remove(player);
        if (admin != null) {
            this.arena.msg(admin, MSG.MODULE_BANKICK_UNBANNED, player);
        }
        this.tryNotify(Language.parse(MSG.MODULE_BANKICK_YOUUNBANNED, this.arena.getName()));
        this.arena.getConfig().setManually("bans", this.getPermBanList());
        this.arena.getConfig().save();
    }

    void doTempUnban(final CommandSender admin, final String player) {
        this.tempUnbanList.add(player);
        if (admin != null) {
            this.arena.msg(admin, MSG.MODULE_BANKICK_UNBANNED, player);
        }
        this.tryNotify(Language.parse(MSG.MODULE_BANKICK_YOUUNBANNED, this.arena.getName()));
    }

    private long parseStringToSeconds(final String string) {
        String[] computedStr = string.split("(?=[dhms])", 2);
        String value = computedStr[0];
        String quantifier = "s";
        try {
            quantifier = computedStr[1].substring(0, 1);
        } catch (IndexOutOfBoundsException ignored) {}

        double time = Double.parseDouble(value);

        if ("d".equals(quantifier)) {
            return round(SECONDS.convert(1, DAYS) * time);
        }

        if ("h".equals(quantifier)) {
            return round(SECONDS.convert(1, HOURS) * time);
        }

        if ("m".equals(quantifier)) {
            return round(SECONDS.convert(1, MINUTES) * time);
        }

        return round(time);
    }

    private void tryKick(final CommandSender sender, final String string) {
        final Player p = Bukkit.getPlayer(string);
        ArenaPlayer ap = ArenaPlayer.fromPlayer(string);
        if (p != null && ap.getStatus() == PlayerStatus.NULL) {
            return;
        }
        this.arena.playerLeave(p, CFG.TP_EXIT, true, true, false);
        this.arena.msg(p, MSG.MODULE_BANKICK_YOUKICKED, this.arena.getName());
        this.arena.msg(sender, MSG.MODULE_BANKICK_KICKED, string);
    }

    private void tryNotify(final String string) {
        final Player p = Bukkit.getPlayer(string);
        if (p == null) {
            return;
        }
        this.arena.msg(p, string);
    }
}
