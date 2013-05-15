package me.maciekmm.FrameStore;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;

public class DatabaseConnector {

    private final Plugin plugin;
    private Logger log;
    private String url;

    public DatabaseConnector(final Plugin plugin, final String host, final String database, final String user, final String password) {
        this.plugin = plugin;
        url = "jdbc:mysql://" + host + "/" + database + "?user=" + user + "&password=" + password;
        log = plugin.getServer().getLogger();
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "Cannot connect to database!");
        }
    }
	private void initDriver(final String driver)
	{
		try
		{
			Class.forName(driver);
		}
		catch(final Exception e)
		{
			log.severe("Database driver error:" + e.getMessage());
		}
	}
    public ResultSet query(final String query) {
        return query(query, false);
    }
    public void close() {
        try {
            DriverManager.getConnection(url).close();
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public ResultSet query(final String query, final boolean retry) {
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
}
