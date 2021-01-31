package net.slipcor.pvparena.modules.aftermatch;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.runnables.ArenaRunnable;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

public class AfterMatch extends ArenaModule {
    private ArenaRunnable afterTask;
    private boolean aftermatch;

    public AfterMatch() {
        super("AfterMatch");
    }


    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    public void afterMatch() {
        for (final ArenaTeam t : arena.getTeams()) {
            for (final ArenaPlayer p : t.getTeamMembers()) {
                if (p.getStatus() != Status.FIGHT) {
                    continue;
                }
                final Player player = p.get();
                if (player != null) {
                    this.arena.tpPlayerToCoordName(p, "after");
                }
            }
        }
        arena.broadcast(Language.parse(MSG.MODULE_AFTERMATCH_STARTING));
        PVPArena.getInstance().getAgm().setPlayerLives(arena, 0);
        aftermatch = true;
        try {
            afterTask.cancel();
        } catch (final Exception e) {

        }
        afterTask = null;
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!am".equals(s) || "aftermatch".equals(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("aftermatch");
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
    public String checkForMissingSpawns(final Set<String> list) {
        for (final String s : list) {
            if (s.startsWith("after")) {
                return null;
            }
        }
        return Language.parse(MSG.MODULE_AFTERMATCH_SPAWNNOTSET);
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !am time 6
        // !am death 4

        if (!PVPArena.hasAdminPerms(sender)
                && !PVPArena.hasCreatePerms(sender, arena)) {
            arena.msg(
                    sender,
                    Language.parse(MSG.ERROR_NOPERM,
                            Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{2, 3})) {
            return;
        }

        if ("!am".equals(args[0]) || "aftermatch".equals(args[0])) {
            if (args.length == 2) {
                if ("off".equals(args[1])) {
                    arena.getArenaConfig().set(CFG.MODULES_AFTERMATCH_AFTERMATCH, args[1]);
                    arena.getArenaConfig().save();
                    arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_AFTERMATCH_AFTERMATCH.getNode(), args[1]));
                    return;
                }
                arena.msg(sender, Language.parse(MSG.ERROR_ARGUMENT, args[1], "off"));
                return;
            }
            final int i;
            try {
                i = Integer.parseInt(args[2]);
            } catch (final Exception e) {
                arena.msg(sender,
                        Language.parse(MSG.ERROR_NOT_NUMERIC, args[2]));
                return;
            }
            if ("time".equals(args[1]) || "death".equals(args[1])) {
                arena.getArenaConfig().set(CFG.MODULES_AFTERMATCH_AFTERMATCH, args[1] + ':' + i);
                arena.getArenaConfig().save();
                arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_AFTERMATCH_AFTERMATCH.getNode(), args[1] + ':' + i));
                return;
            }

            arena.msg(sender, Language.parse(MSG.ERROR_ARGUMENT, args[1], "time | death"));
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
                + StringParser.colorVar(!"off".equals(arena.getArenaConfig().getString(CFG.MODULES_AFTERMATCH_AFTERMATCH)))
                + '('
                + StringParser.colorVar(arena.getArenaConfig()
                .getString(CFG.MODULES_AFTERMATCH_AFTERMATCH)) + ')');
    }

    @Override
    public boolean hasSpawn(final String string) {
        return "after".equals(string);
    }

    @Override
    public void parsePlayerDeath(final Player player,
                                 final EntityDamageEvent cause) {
        final String pu = arena.getArenaConfig().getString(CFG.MODULES_AFTERMATCH_AFTERMATCH);

        if ("off".equals(pu) || aftermatch) {
            return;
        }

        final String[] ss = pu.split(":");
        if (pu.startsWith("time") || afterTask != null) {
            return;
        }

        int i = Integer.parseInt(ss[1]);

        for (final ArenaTeam t : arena.getTeams()) {
            for (final ArenaPlayer p : t.getTeamMembers()) {
                if (p.getStatus() != Status.FIGHT) {
                    continue;
                }
                if (--i < 0) {
                    return;
                }
            }
        }

        afterTask = null;

        afterMatch();
    }

    @Override
    public void reset(final boolean force) {
        if (afterTask != null) {
            afterTask.cancel();
            afterTask = null;
        }
        aftermatch = false;
    }

    @Override
    public void parseStart() {
        final String pu = arena.getArenaConfig().getString(CFG.MODULES_AFTERMATCH_AFTERMATCH);

        if (afterTask != null) {
            afterTask.cancel();
            afterTask = null;
        }

        final int i;
        final String[] ss = pu.split(":");
        if (pu.startsWith("time")) {
            // arena.powerupTrigger = "time";
            i = Integer.parseInt(ss[1]);
        } else {
            return;
        }

        debug("using aftermatch : {}:{}", arena.getArenaConfig().getString(CFG.MODULES_AFTERMATCH_AFTERMATCH), i);
        if (i > 0) {
            debug("aftermatch time trigger!");
            afterTask = new AfterRunnable(this, i);
        }
    }
}
