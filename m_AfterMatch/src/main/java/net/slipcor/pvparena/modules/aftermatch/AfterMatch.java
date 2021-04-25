package net.slipcor.pvparena.modules.aftermatch;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeleportManager;
import net.slipcor.pvparena.runnables.ArenaRunnable;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

public class AfterMatch extends ArenaModule {
    public static final String AFTER = "after";
    public static final String AFTER_MATCH = "aftermatch";
    private ArenaRunnable afterTask;
    private boolean isAfterMatch;

    public AfterMatch() {
        super("AfterMatch");
    }


    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    public void afterMatch() {
        for (final ArenaTeam t : this.arena.getTeams()) {
            for (final ArenaPlayer arenaPlayer : t.getTeamMembers()) {
                if (arenaPlayer.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }
                TeleportManager.teleportPlayerToRandomSpawn(this.arena, arenaPlayer,
                        SpawnManager.getPASpawnsStartingWith(this.arena, AFTER));
            }
        }

        this.arena.broadcast(Language.parse(MSG.MODULE_AFTERMATCH_STARTING));
        this.arena.getGoal().setPlayersLives(0);
        this.isAfterMatch = true;

        try {
            this.afterTask.cancel();
        } catch (final Exception e) {

        }
        this.afterTask = null;
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!am".equals(s) || AFTER_MATCH.equals(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList(AFTER_MATCH);
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!am");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"off"});
        result.define(new String[]{"time"});
        result.define(new String[]{"death"});
        return result;
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(final Set<PASpawn> spawns) {
        final Set<PASpawn> missing = new HashSet<>();
        if (spawns.stream().noneMatch(spawn ->
                (spawn.getName().equals(AFTER))
                        && spawn.getTeamName() == null)) {
            missing.add(new PASpawn(null, AFTER, null, null));
        }
        return missing;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !am time 6
        // !am death 4

        if (!PVPArena.hasAdminPerms(sender)
                && !PVPArena.hasCreatePerms(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2, 3})) {
            return;
        }

        if ("!am".equals(args[0]) || AFTER_MATCH.equals(args[0])) {
            if (args.length == 2) {
                if ("off".equals(args[1])) {
                    this.arena.getConfig().set(CFG.MODULES_AFTERMATCH_AFTERMATCH, args[1]);
                    this.arena.getConfig().save();
                    this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_AFTERMATCH_AFTERMATCH.getNode(), args[1]);
                    return;
                }
                this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "off");
                return;
            }
            final int i;
            try {
                i = Integer.parseInt(args[2]);
            } catch (final Exception e) {
                this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[2]);
                return;
            }
            if ("time".equals(args[1]) || "death".equals(args[1])) {
                this.arena.getConfig().set(CFG.MODULES_AFTERMATCH_AFTERMATCH, args[1] + ':' + i);
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_AFTERMATCH_AFTERMATCH.getNode(), args[1] + ':' + i);
                return;
            }

            this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "time | death");
        }
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        final String pu = config.getString(CFG.MODULES_AFTERMATCH_AFTERMATCH.getNode(), "off");

        if (!pu.startsWith("death") && !pu.startsWith("time")) {
            PVPArena.getInstance().getLogger().warning("error activating aftermatch module");
        }
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage("active: "
                + StringParser.colorVar(!"off".equals(this.arena.getConfig().getString(CFG.MODULES_AFTERMATCH_AFTERMATCH)))
                + '('
                + StringParser.colorVar(this.arena.getConfig()
                .getString(CFG.MODULES_AFTERMATCH_AFTERMATCH)) + ')');
    }

    @Override
    public boolean hasSpawn(final String name, final String teamName) {
        return AFTER.equals(name);
    }

    @Override
    public void parsePlayerDeath(final Player player,
                                 final EntityDamageEvent cause) {
        final String pu = this.arena.getConfig().getString(CFG.MODULES_AFTERMATCH_AFTERMATCH);

        if ("off".equals(pu) || this.isAfterMatch) {
            return;
        }

        final String[] ss = pu.split(":");
        if (pu.startsWith("time") || this.afterTask != null) {
            return;
        }

        int i = Integer.parseInt(ss[1]);

        for (final ArenaTeam t : this.arena.getTeams()) {
            for (final ArenaPlayer p : t.getTeamMembers()) {
                if (p.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }
                if (--i < 0) {
                    return;
                }
            }
        }

        this.afterTask = null;

        this.afterMatch();
    }

    @Override
    public void reset(final boolean force) {
        if (this.afterTask != null) {
            this.afterTask.cancel();
            this.afterTask = null;
        }
        this.isAfterMatch = false;
    }

    @Override
    public void parseStart() {
        final String pu = this.arena.getConfig().getString(CFG.MODULES_AFTERMATCH_AFTERMATCH);

        if (this.afterTask != null) {
            this.afterTask.cancel();
            this.afterTask = null;
        }

        final int i;
        final String[] ss = pu.split(":");
        if (pu.startsWith("time")) {
            // arena.powerupTrigger = "time";
            i = Integer.parseInt(ss[1]);
        } else {
            return;
        }

        debug("using aftermatch : {}:{}", pu, i);
        if (i > 0) {
            debug("aftermatch time trigger!");
            this.afterTask = new AfterRunnable(this, i);
        }
    }
}
