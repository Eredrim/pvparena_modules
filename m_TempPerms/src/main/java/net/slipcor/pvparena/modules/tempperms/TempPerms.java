package net.slipcor.pvparena.modules.tempperms;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TempPerms extends ArenaModule {
    private static final String DEFAULT_NODE = "default";
    private static final String SETTINGS_PATH = "modules.tempperms.";

    public TempPerms() {
        super("TempPerms");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!tps".equalsIgnoreCase(s) || "tempperms".equalsIgnoreCase(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("tempperms");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!tps");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);

        if (arena == null) {
            return result;
        }

        result.define(new String[]{"add"});
        this.getTempPerms(arena, DEFAULT_NODE).entrySet().forEach(entry ->
                result.define(new String[]{"rem", getPolarizedPermission(entry)})
        );

        arena.getClasses().forEach(aClass -> {
            result.define(new String[]{aClass.getName(), "add"});
            this.getTempPerms(arena, aClass.getName()).entrySet().forEach(entry ->
                    result.define(new String[]{aClass.getName(), "rem", getPolarizedPermission(entry)})
            );
        });
        arena.getTeamNames().forEach(team -> {
            result.define(new String[]{team, "add"});
            this.getTempPerms(arena, team).entrySet().forEach(entry ->
                    result.define(new String[]{team, "rem", getPolarizedPermission(entry)})
            );
        });
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !tps | list perms
        // !tps add [perm] | add perm
        // !tps rem [perm] | remove perm

        // !tps [classname] | list class perms
        // !tps [classname] add [perm] | add class perm
        // !tps [classname] rem [perm] | remove class perm

        // !tps [teamname] | list team perms
        // !tps [teamname] add [perm] | add team perm
        // !tps [teamname] rem [perm] | remove team perm

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{1, 2, 3, 4})) {
            return;
        }

        Map<String, Boolean> map = this.getTempPerms(this.arena, DEFAULT_NODE);

        if (args.length == 1) {
            this.arena.msg(sender, MSG.MODULE_TEMPPERMS_HEAD, DEFAULT_NODE);
            map.forEach((key, value) -> this.arena.msg(sender, key + " - " + StringParser.colorVar(value)));
            return;
        }

        if (args.length == 3 && ("add".equals(args[1]) || "rem".equals(args[1]))) {
            if ("add".equals(args[1])) {
                map.put(args[2], true);
                this.arena.msg(sender, MSG.MODULE_TEMPPERMS_ADDED, args[2], DEFAULT_NODE);
            } else {
                map.remove(getStrippedPermission(args[2]));
                this.arena.msg(sender, MSG.MODULE_TEMPPERMS_REMOVED, args[2], DEFAULT_NODE);
            }
            this.setTempPerms(this.arena, map, DEFAULT_NODE);
            return;
        }

        if (args.length == 3) {
            this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "add | remove");
            return;
        }

        map = this.getTempPerms(this.arena, args[1]);
        if (args.length == 2) {
            this.arena.msg(sender, MSG.MODULE_TEMPPERMS_HEAD, args[1]);
            map.forEach((key, value) -> this.arena.msg(sender, key + " - " + StringParser.colorVar(value)));
            return;
        }

        if (args.length == 4 && ("add".equals(args[2]) || "rem".equals(args[2]))) {
            if ("add".equals(args[2])) {
                map.put(args[3], true);
                this.arena.msg(sender, MSG.MODULE_TEMPPERMS_ADDED, args[3], args[1]);
            } else {
                map.remove(getStrippedPermission(args[3]));
                this.arena.msg(sender, MSG.MODULE_TEMPPERMS_REMOVED, args[3], args[1]);
            }
            this.setTempPerms(this.arena, map, args[1]);
            return;
        }

        this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[2], "add | remove");
    }

    @Override
    public void parseClassChange(Player player, ArenaClass aClass) {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        if (this.arena != null && this.arena.getEveryone().contains(aPlayer)) {
            this.removePermissions(player);

            class RunLater extends BukkitRunnable {

                @Override
                public void run() {
                    Map<String, Boolean> globalPerms = TempPerms.this.getTempPerms(TempPerms.this.arena, DEFAULT_NODE);
                    Map<String, Boolean> teamPerms = TempPerms.this.getTempPerms(TempPerms.this.arena, aPlayer.getArenaTeam().getName());
                    TempPerms.this.setPermissions(TempPerms.this.arena, aPlayer, globalPerms, teamPerms);
                }
            }

            try {
                new RunLater().runTaskLater(PVPArena.getInstance(), 1);
            } catch (IllegalPluginAccessException ex) {
                new RunLater().run();
            }
        }
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        final ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
        this.setPermissions(this.arena, ap, this.getTempPerms(this.arena, DEFAULT_NODE), this.getTempPerms(this.arena, ap.getArenaTeam().getName()));
    }

    /**
     * get the permissions map
     *
     * @return the temporary permissions map
     */
    private Map<String, Boolean> getTempPerms(final Arena arena, final String node) {
        final Map<String, Boolean> result = new HashMap<>();

        if (arena.getConfig().getYamlConfiguration().contains(SETTINGS_PATH + node)) {
            arena.getConfig().getStringList(SETTINGS_PATH + node, new ArrayList<>()).forEach(key ->
                    result.put(getStrippedPermission(key), !(key.startsWith("^") || key.startsWith("-")))
            );
        }
        return result;
    }

    private void setTempPerms(final Arena arena, final Map<String, Boolean> map, final String node) {
        final List<String> result = new ArrayList<>();
        for (final Map.Entry<String, Boolean> stringBooleanEntry : map.entrySet()) {
            result.add(getPolarizedPermission(stringBooleanEntry));
        }
        arena.getConfig().setManually(SETTINGS_PATH + node, result);
        arena.getConfig().save();
    }

    private static String getStrippedPermission(String key) {
        return key.replaceAll("[\\^\\-]", "");
    }

    private static String getPolarizedPermission(Map.Entry<String, Boolean> stringBooleanEntry) {
        return (stringBooleanEntry.getValue() ? "" : "-") + stringBooleanEntry.getKey();
    }

    private void setPermissions(final Arena arena, final ArenaPlayer arenaPlayer, final Map<String, Boolean> mGlobal, final Map<String, Boolean> mTeam) {
        Map<String, Boolean> total = new HashMap<>();
        total.putAll(mGlobal);
        total.putAll(mTeam);

        if (arenaPlayer.getArenaClass() != null) {
            total.putAll(this.getTempPerms(arena, arenaPlayer.getArenaClass().getName()));
        }

        if (!total.isEmpty()) {
            PermissionAttachment pa = arenaPlayer.getPlayer().addAttachment(PVPArena.getInstance());
            total.forEach(pa::setPermission);
            arenaPlayer.getTempPermissions().add(pa);
        }
    }

    /**
     * remove temporary permissions from a player
     *
     * @param p the player to reset
     */
    void removePermissions(final Player p) {
        final ArenaPlayer player = ArenaPlayer.fromPlayer(p);

        if (player != null && player.getTempPermissions() != null) {
            player.getTempPermissions().stream()
                    .filter(Objects::nonNull)
                    .forEach(PermissionAttachment::remove);
            player.getTempPermissions().clear();
        }
    }

    @Override
    public void resetPlayer(final Player player, final boolean soft, final boolean force) {
        try {
            Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), () -> {
                this.removePermissions(player);
            }, 5L);
        } catch (final IllegalPluginAccessException e) {
            // we don't care. Perms don't need to be removed on shutdown
        }
    }
}
