package me.haer0248.WebPlayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    public static Statement st;
    public static Connection conn = null;

    public static String databaseName = Main.maindb;

    public static boolean connect() {
        String url = "jdbc:mysql://" + Main.hostname + "/" + databaseName + "?user=" + Main.user + "&password="
                + Main.password + "&autoReconnect=true&useSSL=false";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url);
            st = conn.createStatement();
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isDisconnect() {
        try {
            if (conn.isClosed() || conn == null) {
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }
}
