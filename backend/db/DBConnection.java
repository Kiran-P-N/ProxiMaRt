// In db/DBConnection.java
package backend.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // --- DATABASE CONNECTION DETAILS ---
    // Make sure the database name "proximart" matches what you created.
    private static final String DB_URL = "jdbc:mysql://localhost:3306/proximart";
    
    // !!! IMPORTANT !!!
    // Replace "your_mysql_username" with your actual MySQL username (e.g., "root").
    private static final String DB_USER = "root"; 
    
    // Replace "your_mysql_password" with the password you set for MySQL.
    private static final String DB_PASSWORD = "opensql";

    /**
     * Establishes and returns a connection to the database.
     * @return A Connection object or null if an error occurs.
     */
    public static Connection getConnection() {
        Connection connection = null;
        try {
            // This line loads the MySQL driver.
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // This line attempts to connect to the database.
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
        } catch (ClassNotFoundException e) {
            System.err.println("Error: MySQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error: Failed to connect to the database!");
            e.printStackTrace();
        }
        return connection;
    }
}