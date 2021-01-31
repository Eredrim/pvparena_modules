package net.slipcor.pvparena.modules.matchresultstats.JesiKat;


import java.sql.*;

/**
 * @author Jesika(Kaitlyn) Tremaine aka JesiKat
 *         <p/>
 *         MySQLConnection.
 *         This class is a simple database connector with the basic useful methods such as getting the number of rows in a table,
 *         the names of columns in the table, etc.
 */
public class MySQLConnection {
    /*The host for the database, the username for the database, and the password*/
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;

    /*The connection object*/
    private Connection databaseConnection;

    public MySQLConnection(final String host, final int port, final String database, final String username, final String password) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        dbUrl = host + ':' + port + '/' + database;
        dbUsername = username;
        dbPassword = password;
        Class.forName("com.mysql.jdbc.Driver").newInstance();
    }

    /**
     * @param printerror If this is true, this method will print an error if there is one and return false
     * @return True if the connection was made successfully, false if otherwise.
     */
    public boolean connect(final boolean printerror) {
        try {
            databaseConnection = DriverManager.getConnection("jdbc:mysql://" + dbUrl + "?autoReconnect=true", dbUsername, dbPassword);
            return databaseConnection != null;
        } catch (final SQLException e) {
            if (printerror) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * @param query    The Query to send to the SQL server.
     * @param modifies If the Query modifies the database, set this to true. If not, set this to false
     * @return If modifies is true, returns a valid ResultSet obtained from the Query. If modifies is false, returns null.
     * @throws SQLException if the Query had an error or there was not a valid connection.
     */
    public ResultSet executeQuery(final String query, final boolean modifies) throws SQLException {
        final Statement statement = databaseConnection.createStatement();
        if (modifies) {
            statement.execute(query);
            return null;
        } else {
            return statement.executeQuery(query);
        }
    }

    /**
     * @param database The database to check for the table in.
     * @param table    The table to check for existence.
     * @return true if the table exists, false if there was an error or the database doesn't exist.
     * <p/>
     * This method looks through the information schema that comes with a MySQL installation and checks
     * if a certain table exists within a database.
     */
    public boolean tableExists(final String database, final String table) {
        try {
            final String format = "SELECT * FROM `information_schema`.`TABLES` WHERE TABLE_SCHEMA = '$DB' && TABLE_NAME = '$TABLE';";
            return databaseConnection.createStatement().executeQuery(format.replace("$DB", database).replace("$TABLE", table)).first();
        } catch (final SQLException e) {
            return false;
        }
    }
}
