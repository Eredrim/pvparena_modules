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

import static net.slipcor.pvparena.config.Debugger.debug;

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
        return this.getClass().getPackage().getImplementationVersion();
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

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            Arena.pmsg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{1, 2})) {
        }

        // TODO: do something
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (dbTable == null) {

            PVPArena.getInstance().getConfig().options().copyDefaults(true);
            PVPArena.getInstance().getConfig().addDefault("MySQLhost", "");
            PVPArena.getInstance().getConfig().addDefault("MySQLuser", "");
            PVPArena.getInstance().getConfig().addDefault("MySQLpass", "");
            PVPArena.getInstance().getConfig().addDefault("MySQLdb", "");
            PVPArena.getInstance().getConfig().addDefault("MySQLtable", "pvparena_stats");
            PVPArena.getInstance().getConfig().addDefault("MySQLport", 3306);
            PVPArena.getInstance().saveConfig();

            dbHost = PVPArena.getInstance().getConfig().getString("MySQLhost", "");
            dbUser = PVPArena.getInstance().getConfig().getString("MySQLuser", "");
            dbPass = PVPArena.getInstance().getConfig().getString("MySQLpass", "");
            dbDatabase = PVPArena.getInstance().getConfig().getString("MySQLdb", "");
            dbTable = PVPArena.getInstance().getConfig().getString("MySQLtable", "pvparena_stats");
            dbPort = PVPArena.getInstance().getConfig().getInt("MySQLport", 3306);

            if (this.sqlHandler == null) {
                try {
                    this.sqlHandler = new MySQLConnection(dbHost, dbPort, dbDatabase, dbUser,
                            dbPass);
                } catch (final InstantiationException | ClassNotFoundException | IllegalAccessException e1) {
                    e1.printStackTrace();
                }

                debug(this.arena, "MySQL Initializing");
                // Initialize MySQL Handler

                if (this.sqlHandler.connect(true)) {
                    debug(this.arena, "MySQL connection successful");
                    // Check if the tables exist, if not, create them
                    if (!this.sqlHandler.tableExists(dbDatabase, dbTable)) {
                        debug(this.arena, "Creating table " + dbTable);
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
                            this.sqlHandler.executeQuery(query, true);
                        } catch (final SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    PVPArena.getInstance().getLogger().severe("MySQL connection failed");
                }
                MRSMySQL.initiate(this);
            }
        }
    }

    private PVPData data;

    @Override
    public void giveRewards(final Player player) {
        this.data.winning(player.getName());
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        if (this.data == null) {
            this.data = new PVPData(this.arena);
        }
        this.data.join(player.getName(), team.getName());
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        this.data.losing(player.getName());
    }

    @Override
    public void parseStart() {
        this.data.start();
    }

    @Override
    public void reset(final boolean force) {
        if(this.data != null) {
            this.data.reset(force);
        }
    }
}
