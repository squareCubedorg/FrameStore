package me.maciekmm.FrameStore;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DatabaseConnector {

    private final FrameStore plugin;
    private Logger log;
    private String url;

    public DatabaseConnector(final FrameStore plugin, final String host, final String database, final String user, final String password) {
        this.plugin = plugin;
        url = "jdbc:mysql://" + host + "/" + database + "?user=" + user + "&password=" + password;
        log = plugin.getServer().getLogger();
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "Cannot connect to database!");
        }
    }

    /**
     * Connect/create a SQLite database
     *
     * @param plugin the plugin handle
     * @param filePath database storage path/name.extension
     */
    public DatabaseConnector(final FrameStore plugin, final String filePath) {
        this.plugin = plugin;
        url = "jdbc:sqlite:" + new File(filePath).getAbsolutePath();
        log = plugin.getServer().getLogger();

        initDriver("org.sqlite.JDBC");
    }

    private void initDriver(final String driver) {
        try {
            Class.forName(driver);
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Database driver error:{0}", e.getMessage());
        }
    }

    public ResultSet query(final String query) {
        if(!plugin.type.equalsIgnoreCase("mysql"))
        {
            return query(convert(query),false);
        }
        else
        {
            return query(query, false);
        }
    }

    public void close() {
        try {
            DriverManager.getConnection(url).close();
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private ResultSet query(final String query, final boolean retry) {
        try {
            final Connection connection = DriverManager.getConnection(url);
            final PreparedStatement statement = connection.prepareStatement(query);

            if (statement.execute()) {
                return statement.getResultSet();
            }
        } catch (final SQLException e) {
            final String msg = e.getMessage();

            log.log(Level.SEVERE, "Database error in query: {0}", msg);

            if (retry && msg.contains("_BUSY")) {
                log.severe("Retrying query");

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        query(query);
                    }
                }, 20);
            }
        }

        return null;
    }
    
    private String convert(String mysqlQuery) {
        return mysqlQuery.replaceAll("`", "\"").
                replaceAll("/(int|integer)\\(/i", "(").
                replaceAll("/\\(NULL(\\s|\\t)?\\,/i", "(").
                replaceAll("/DEFAULT '(.*)'/im","DEFAULT \"$1\"").replaceAll("AUTO_INCREMENT", "");
    }
}
