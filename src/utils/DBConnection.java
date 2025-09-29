package utils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class DBConnection {
    private static final Logger logger = Logger.getLogger(DBConnection.class.getName());
    private static Connection connection;
    private static Properties props;

    private DBConnection() {}

    public static class DBConnectionException extends RuntimeException {
        public DBConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static Connection getInstance() {
        if (connection == null || isConnectionClosed()) {
            try {
                if (props == null) {
                    props = new Properties();
                    try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
                        if (input == null) {
                            String errorMsg = "config.properties not found";
                            logger.severe(errorMsg);
                            throw new DBConnectionException(errorMsg, null);
                        }
                        props.load(input);
                    }
                }

                String url = props.getProperty("db.url");
                String user = props.getProperty("db.user");
                String password = props.getProperty("db.password");

                if (url == null || user == null || password == null) {
                    String errorMsg = "Database configuration properties are missing";
                    logger.severe(errorMsg);
                    throw new DBConnectionException(errorMsg, null);
                }

                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(url, user, password);
                logger.info("Database connection established");
            } catch (SQLException e) {
                String errorMsg = String.format("Failed to connect to database: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
                logger.severe(errorMsg);
                throw new DBConnectionException(errorMsg, e);
            } catch (ClassNotFoundException e) {
                String errorMsg = "PostgreSQL driver not found";
                logger.severe(errorMsg);
                throw new DBConnectionException(errorMsg, e);
            } catch (Exception e) {
                String errorMsg = "Unexpected error while connecting to database: " + e.getMessage();
                logger.severe(errorMsg);
                throw new DBConnectionException(errorMsg, e);
            }
        }
        return connection;
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                logger.info("Database connection closed");
            } catch (SQLException e) {
                String errorMsg = String.format("Failed to close database connection: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
                logger.severe(errorMsg);
                throw new DBConnectionException(errorMsg, e);
            }
        }
    }

    private static boolean isConnectionClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (SQLException e) {
            logger.warning("Error checking connection status: " + e.getMessage());
            return true;
        }
    }
}