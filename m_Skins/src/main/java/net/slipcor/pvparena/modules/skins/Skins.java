package net.slipcor.pvparena.modules.skins;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Skins extends ArenaModule {
    private static final String SKIN = "skin";
    private static final String MODULES_SKINS = "modules.skins";
    private static LibsDisguiseHandler libsDisguiseHandler;
    private static boolean enabled;

    private final Set<String> disguised = new HashSet<>();

    public Skins() {
        super("Skins");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!sk".equals(s) || s.startsWith(MODULES_SKINS);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList(MODULES_SKINS);
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!sk");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }
        for (final String team : arena.getTeamNames()) {
            result.define(new String[]{team});
        }
        for (final ArenaClass aClass : arena.getClasses()) {
            result.define(new String[]{aClass.getName()});
        }
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !sk [teamname] [skin] |
        // !sk [classname] [skin] |
        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3})) {
            return;
        }

        final ArenaClass c = this.arena.getClass(args[1]);

        if (c == null) {
            final ArenaTeam team = this.arena.getTeam(args[1]);
            if (team != null) {
                // !sk [teamname] [skin]

                if (args.length == 2) {
                    this.arena.msg(sender, Language.parse(
                            MSG.MODULE_SKINS_SHOWTEAM,
                            team.getColoredName(),
                            this.arena.getConfig().getUnsafe(MODULES_SKINS + "." + team.getName())));
                    return;
                }

                this.arena.getConfig().setManually(MODULES_SKINS + "." + team.getName(), args[2]);
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.SET_DONE, team.getName(), args[2]);

                return;
            }
            // no team AND no class!

            this.arena.msg(sender, MSG.ERROR_CLASS_NOT_FOUND, args[1]);
            this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, args[1]);
            this.printHelp(this.arena, sender);
            return;
        }
        // !sk [classname] | show
        // !bg [classname] [skin]

        if (args.length == 2) {
            this.arena.msg(sender, MSG.MODULE_SKINS_SHOWCLASS, (String) this.arena.getConfig().getUnsafe(MODULES_SKINS + "." + c.getName()));
            return;
        }

        this.arena.getConfig().setManually(MODULES_SKINS + "." + c.getName(), args[2]);
        this.arena.getConfig().save();
        this.arena.msg(sender, MSG.SET_DONE, c.getName(), args[2]);

    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (config.get(MODULES_SKINS) == null) {
            for (final ArenaTeam team : this.arena.getTeams()) {
                final String sName = team.getName();
                config.addDefault(MODULES_SKINS + "." + sName, "Herobrine");
            }
            config.options().copyDefaults(true);
        }
    }

    @Override
    public void parseRespawn(final Player player, final ArenaTeam team, final DamageCause lastDamageCause, final Entity damager) {
        if (libsDisguiseHandler != null) {
            libsDisguiseHandler.parseRespawn(player);
        }
    }

    @Override
    public void onThisLoad() {
        if (this.arena == null) {
            return;
        }
        if (enabled || this.arena.getConfig().getBoolean(CFG.MODULES_SKINS_VANILLA)) {
            enabled = true;
            return;
        }
        MSG m = MSG.MODULE_SKINS_NOMOD;
        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            libsDisguiseHandler = new LibsDisguiseHandler();
            m = MSG.MODULE_SKINS_LIBSDISGUISE;
        }

        enabled = true;

        Arena.pmsg(Bukkit.getConsoleSender(), m);
    }

    private void printHelp(final Arena arena, final CommandSender sender) {
        arena.msg(sender, "/pa [arenaname] !sk [teamname]  | show team disguise");
        arena.msg(sender, "/pa [arenaname] !sk [teamname] [skin] | set team disguise");
        arena.msg(sender, "/pa [arenaname] !sk [classname]  | show class disguise");
        arena.msg(sender, "/pa [arenaname] !sk [classname] [skin] | set class disguise");
    }

    @Override
    public void teleportPlayer(final Player player, final PASpawn place) {

        if (this.disguised.contains(player.getName()) || !this.arena.hasPlayer(player)) {
            return;
        }

        final ArenaTeam team = ArenaPlayer.fromPlayer(player).getArenaTeam();
        if (team == null) {
            return;
        }
        String disguise = (String) this.arena.getConfig().getUnsafe("skins." + team.getName());

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        if (!PlayerStatus.FIGHT.equals(arenaPlayer.getStatus())) {
            return;
        }

        if (arenaPlayer.getArenaClass() != null && (disguise == null || "none".equals(disguise))) {
            disguise = (String) this.arena.getConfig().getUnsafe("skins." + arenaPlayer.getArenaClass().getName());
        }

        if (disguise == null || "none".equals(disguise)) {
            return;
        }

        if (libsDisguiseHandler != null) {
            libsDisguiseHandler.parseTeleport(player, disguise);
        } else {
            this.setPlayerHead(team, player);
        }

        this.disguised.add(player.getName());
    }

    private void setPlayerHead(ArenaTeam team, Player player) {
        if (team != null) {
            final ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD, 1);
            final String disguise = (String) this.arena.getConfig().getUnsafe("skins." + team.getName());
            if (disguise == null) {
                return;
            }
            final SkullMeta sm = (SkullMeta) itemStack.getItemMeta();
            if (sm != null) {
                sm.setUnbreakable(true);
                final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(disguise);
                sm.setOwningPlayer(offlinePlayer);
                itemStack.setItemMeta(sm);
            }
            class TempRunnable implements Runnable {
                @Override
                public void run() {
                    player.getInventory().setHelmet(itemStack);
                }
            }
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new TempRunnable(), 5L);
        }
    }

    @Override
    public void unload(final Player player) {
        if (libsDisguiseHandler != null) {
            libsDisguiseHandler.unload(player);
        }
        this.disguised.remove(player.getName());
    }
}
