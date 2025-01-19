package net.slipcor.pvparena.modules.teamsizerestrict;


import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.RandomUtils;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static net.slipcor.pvparena.config.Debugger.debug;

public class TeamSizeRestrict extends ArenaModule {

    private static final String CFG_PATH = "modules.teamsize";

    public TeamSizeRestrict() {
        super("TeamSizeRestrict");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!tsr".equalsIgnoreCase(s) || "teamsizerestrict".equalsIgnoreCase(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("teamsizerestrict");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!tsr");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        CommandTree<String> result = new CommandTree<>(null);
        arena.getTeamNames().forEach(teamName -> result.define(new String[]{teamName}));
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !tsr [team] [size]

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3})) {
            return;
        }

        ArenaTeam matchedTeam = this.arena.getTeam(args[1]);
        if(matchedTeam != null) {
            String fullPath = String.format("%s.%s", CFG_PATH, matchedTeam.getName());
            try {
                this.arena.getConfig().setManually(fullPath, Integer.parseInt(args[2]));
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.CFG_SET_DONE, fullPath, args[2]);
            } catch (NumberFormatException e) {
                this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[2]);
            }
        } else {
            this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, args[1]);
        }
    }

    @Override
    public ArenaTeam choosePlayerTeam(Player player, ArenaTeam team, boolean canSwitch) throws GameplayException {
        try {
            int maxTeam = Integer.parseInt(this.arena.getConfig().getUnsafe(String.format("%s.%s", CFG_PATH, team.getName())).toString());
            if (maxTeam > 0 && team.getTeamMembers().size() >= maxTeam) {
                if(canSwitch) {
                    ArenaTeam secondTryTeam = this.getRandomTeam();
                    if(secondTryTeam == null) {
                        this.arena.msg(player, String.format("%s%s", ChatColor.RED, Language.parse(MSG.ERROR_JOIN_ARENA_FULL)));
                        throw new GameplayException(MSG.ERROR_JOIN_ARENA_FULL);
                    } else {
                        return secondTryTeam;
                    }
                } else {
                    this.arena.msg(player, String.format("%s%s", ChatColor.RED, Language.parse(MSG.ERROR_JOIN_TEAM_FULL, team.getName())));
                    throw new GameplayException(Language.parse(MSG.ERROR_JOIN_TEAM_FULL, team.getName()));
                }
            }
        } catch (NumberFormatException | NullPointerException e) {
            this.arena.getConfig().setManually(String.format("%s.%s", CFG_PATH, team.getName()), 0);
            this.arena.getConfig().save();
        }
        return null;
    }

    @Override
    public void configParse(YamlConfiguration config) {
        this.initConfig();
    }

    @Override
    public void initConfig() {
        Config config = this.arena.getConfig();
        this.arena.getTeamNames().forEach(teamName -> {
            String fullPath = String.format("%s.%s", CFG_PATH, teamName);
            if(!config.getYamlConfiguration().contains(fullPath)) {
                config.setManually(fullPath, 0);
            }
        });
        config.save();
    }

    private ArenaTeam getRandomTeam() {
        debug(this.arena, "calculating free team for TeamSizeRestrict");

        int maxTeamPlayersCfg = this.arena.getConfig().getInt(Config.CFG.READY_MAXTEAMPLAYERS);

        // collect the available teams into a map and get count of "players missing" or "space available"
        Map<ArenaTeam, Integer> availableTeamsWithMemberCount = new HashMap<>();

        this.arena.getTeams().forEach(arenaTeam -> {
                int maxTeamSize = maxTeamPlayersCfg;
                try {
                    maxTeamSize = Integer.parseInt(this.arena.getConfig().getUnsafe(String.format("%s.%s", CFG_PATH, arenaTeam.getName())).toString());
                } catch (NumberFormatException ignored) {}

                int maxPlayerPerTeam = (maxTeamSize == 0) ? 100 : maxTeamSize;

                // don't map full teams
                if(arenaTeam.getTeamMembers().size() < maxPlayerPerTeam) {
                    availableTeamsWithMemberCount.put(arenaTeam, maxPlayerPerTeam - arenaTeam.getTeamMembers().size());
                }
        });

        if(availableTeamsWithMemberCount.isEmpty()) {
            // all teams are full
            return null;
        }

        // pick a random team considering free spaces as a weight for computing randomness
        return RandomUtils.getWeightedRandom(availableTeamsWithMemberCount, new Random());
    }
}
