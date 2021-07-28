package net.slipcor.pvparena.modules.matchresultstats;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.modules.matchresultstats.JesiKat.MySQLConnection;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class MatchResultStats extends ArenaModule {

    public MatchResultStats() {
        super("MatchResultStats");
    }

    MySQLConnection sqlHandler; // MySQL handler

    // Settings Variables
    private static String dbHost;
    private static String dbUser;
    private static String dbPass;
    private static String dbDatabase;
    static String dbTable;
    private static int dbPort = 3306;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "sqlstats".equals(s) || "!ss".equals(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("sqlstats");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!ss");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"reset", "{Player}"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !ss reset
        // !ss reset [player]

        if (!PermissionManager.hasAdminPerm(sender)
                && !PermissionManager.hasBuilderPerm(sender, arena)) {
            Arena.pmsg(sender,
                    Language.parse(MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{1, 2})) {
        }

        // TODO: do something
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (dbTable == null) {

            PVPArena.instance.getConfig().options().copyDefaults(true);
            PVPArena.instance.getConfig().addDefault("MySQLhost", "");
            PVPArena.instance.getConfig().addDefault("MySQLuser", "");
            PVPArena.instance.getConfig().addDefault("MySQLpass", "");
            PVPArena.instance.getConfig().addDefault("MySQLdb", "");
            PVPArena.instance.getConfig().addDefault("MySQLtable", "pvparena_stats");
            PVPArena.instance.getConfig().addDefault("MySQLport", 3306);
            PVPArena.instance.saveConfig();

            dbHost = PVPArena.instance.getConfig().getString("MySQLhost", "");
            dbUser = PVPArena.instance.getConfig().getString("MySQLuser", "");
            dbPass = PVPArena.instance.getConfig().getString("MySQLpass", "");
            dbDatabase = PVPArena.instance.getConfig().getString("MySQLdb", "");
            dbTable = PVPArena.instance.getConfig().getString("MySQLtable", "pvparena_stats");
            dbPort = PVPArena.instance.getConfig().getInt("MySQLport", 3306);

            if (sqlHandler == null) {
                try {
                    sqlHandler = new MySQLConnection(dbHost, dbPort, dbDatabase, dbUser,
                            dbPass);
                } catch (final InstantiationException | ClassNotFoundException | IllegalAccessException e1) {
                    e1.printStackTrace();
                }

                arena.getDebugger().i("MySQL Initializing");
                // Initialize MySQL Handler

                if (sqlHandler.connect(true)) {
                    arena.getDebugger().i("MySQL connection successful");
                    // Check if the tables exist, if not, create them
                    if (!sqlHandler.tableExists(dbDatabase, dbTable)) {
                        arena.getDebugger().i("Creating table " + dbTable);
                        final String query = "CREATE TABLE `" + dbTable + "` ( " +
                                "`id` int(16) NOT NULL AUTO_INCREMENT, " +
                                "`mid` int(8) not null default 0, " +
                                "`arena` varchar(42) NOT NULL, " +
                                "`playername` varchar(42) NOT NULL, " +
                                "`winning` int(1) not null default 0, " +
                                "`team` varchar(42) NOT NULL, " +
                                "`timespent` int(8) NOT NULL default 0, " +
                                "PRIMARY KEY (`id`) ) AUTO_INCREMENT=1 ;";
                         /*
							 * 
							 * `mid`,
							 * `arena`,
							 * `playername`,
							 * `winning`,
							 * `team`,
							 * `timespent`
							 */
                        try {
                            sqlHandler.executeQuery(query, true);
                        } catch (final SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    PVPArena.instance.getLogger().severe("MySQL connection failed");
                }
                MRSMySQL.initiate(this);
            }
        }
    }

    private PVPData data;

    @Override
    public void giveRewards(final Player player) {
        data.winning(player.getName());
    }

    @Override
    public void parseJoin(final CommandSender sender, final ArenaTeam team) {
        if (data == null) {
            data = new PVPData(arena);
        }
        data.join(sender.getName(), team.getName());
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        data.losing(player.getName());
    }

    @Override
    public void parseStart() {
        data.start();
    }

    @Override
    public void reset(final boolean force) {
        if(data != null) {
            data.reset(force);
        }
    }
}
