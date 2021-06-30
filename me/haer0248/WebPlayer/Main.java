package me.haer0248.WebPlayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    public static String hostname, user, password, maindb;
    private static Main instance;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        instance = this;

        File config = new File("plugins/WebPlayer/config.yml");

        if (config.exists()) {
            sendToConsole("Config auto apply!");
        } else {
            getConfig().options().copyDefaults(true);
            saveDefaultConfig();
            sendToConsole("Config created!");
        }
        if (this.getConfig().getBoolean("enable")) {
            loadDBConfig();

            if (Database.connect()){
                sendToConsole(ChatColor.GREEN + "MySQL connect successfully!");
            }else{
                sendToConsole(ChatColor.RED + "MySQL connect failed, please check your config settings.");
            }

            if (this.getConfig().getBoolean("autoreload.enable") == true) {

                this.getConfig().set("autoreload.date", System.currentTimeMillis() + 28800000);
                saveConfig();

                sendToConsole(ChatColor.GREEN + "Auto reconnect database mode enable!");
                Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, new Runnable() {
                    public void run() {
                        Long nowTime = System.currentTimeMillis() + 28800000;
                        Long latTime = getConfig().getLong("autoreload.date");
                        Long last = nowTime - latTime;
                        Long check = getConfig().getLong("autoreload.time") * 60 * 1000;
                        if (last >= check) {
                            Integer retry = 0;
                            while (retry <= 3) {
                                if (Database.connect() == true) {
                                    sendToConsole(ChatColor.GREEN + "MySQL connect successfully! (AUTO)");
                                    break;
                                } else {
                                    sendToConsole(ChatColor.RED + "Database retry..." + retry);
                                }
                                retry += 1;
                            }
                            last = (long) 0;
                            getConfig().set("autoreload.date", System.currentTimeMillis() + 28800000);
                            saveConfig();
                        }
                    }
                }, 0L, 20L);
            } else {
                sendToConsole(ChatColor.RED + "Auto reconnect database mode disable.");
            }
        }
    }

    @EventHandler
    private void onJoin (AsyncPlayerPreLoginEvent event) {
        Integer retry = 0;
        while (retry <= 5) {
            if (Database.connect() == true) {
                sendToConsole(ChatColor.GREEN + "MySQL connect successfully! (PLAYER JOIN)");
                break;
            } else {
                sendToConsole(ChatColor.RED + "Database retry..." + retry);
            }
            retry += 1;
        }
        reloadConfig();
        String username = event.getName();
        UUID uuid = event.getUniqueId();
        ResultSet result = null;
        String message = getConfig().getString("header") + "\n";
        try {
            result = Database.st.executeQuery("SELECT * FROM mc_player WHERE uuid = '" + uuid + "'");
            if (!result.next()) {
                int random_int = (int)Math.floor(Math.random()*(99999999-11111111+1));
                for (String msg : getConfig().getStringList("get_code")) {
                    message += msg.replace("%code%", "" + random_int).replace("%player%", event.getName().toString()).replace("%uuid", event.getUniqueId().toString()) + "\n";
                }
                Database.st.executeUpdate("INSERT INTO mc_player(`username`, `uuid`, `otp_code`) VALUES ('" + username + "', '" + uuid + "', '" + random_int + "')");
                sendToConsole(ChatColor.GREEN + event.getName() + " data created.");
            } else {
                try {
                    Integer otp = result.getInt("otp_code");
                    String pws = result.getString("password");
                    if (otp <= 0) {
                        if (pws != null && pws.length() > 1) {
                            for (String msg : getConfig().getStringList("alreadyset")) {
                                message += msg.replace("%code%", "" + otp).replace("%player%", event.getName().toString()).replace("%uuid", event.getUniqueId().toString()) + "\n";
                            }
                        } else {
                            int random_int = (int)Math.floor(Math.random()*(99999999-11111111+1));
                            for (String msg : getConfig().getStringList("get_code")) {
                                message += msg.replace("%code%", "" + random_int).replace("%player%", event.getName().toString()).replace("%uuid", event.getUniqueId().toString()) + "\n";
                            }
                            Database.st.executeUpdate("UPDATE mc_player SET `otp_code`= '" + random_int + "' WHERE uuid = '" + uuid + "';");
                            sendToConsole(ChatColor.GREEN + event.getName() + " re-generate otp code.");
                        }
                    } else {
                        for (String msg: getConfig().getStringList("get_code")) {
                            message += msg.replace("%code%", ""+otp).replace("%player%", event.getName().toString()).replace("%uuid", event.getUniqueId().toString()) + "\n";
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendToConsole(ChatColor.RED + "MySQL connect failed.");
                }
                Database.st.executeUpdate("UPDATE mc_player SET `username`= '" + username + "' WHERE uuid = '" + uuid + "';");
            }
            message += "\n" + getConfig().getString("footer");
            event.setKickMessage(message);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, message);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendToConsole(String msg) {
        System.out.println(ChatColor.GOLD + "[WebPlayer] "+ msg);
    }

    private void loadDBConfig() {
        if (this.getConfig().getBoolean("enable")) {
            hostname = this.getConfig().getString("database.hostname");
            user = this.getConfig().getString("database.user");
            password = this.getConfig().getString("database.password");
            maindb = this.getConfig().getString("database.maindb");
        }else{
            sendToConsole(ChatColor.YELLOW + "WebPlayer not enable.");
        }
    }
}
