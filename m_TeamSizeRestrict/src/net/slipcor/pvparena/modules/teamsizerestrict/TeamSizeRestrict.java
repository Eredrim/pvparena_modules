package net.slipcor.pvparena.modules.teamsizerestrict;


import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class TeamSizeRestrict extends ArenaModule {
    public TeamSizeRestrict() {
        super("TeamSizeRestrict");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void parseJoin(final CommandSender sender, final ArenaTeam team) {
        try {
            final Integer i = Integer.parseInt(arena.getArenaConfig().getUnsafe("modules.teamsize." + team.getName()).toString());
            if (team.getTeamMembers().size() > i) {
                class RunLater implements Runnable {

                    @Override
                    public void run() {
                        arena.msg(sender, ChatColor.RED + Language.parse(MSG.ERROR_JOIN_TEAM_FULL, team.getName()));
                        new PAG_Leave().commit(arena, sender, new String[0]);
                    }

                }

                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 1L);

            }
        } catch (final Exception e) {
            arena.getArenaConfig().setManually("modules.teamsize." + team.getName(), -1);
            arena.getArenaConfig().save();
        }
    }
}
