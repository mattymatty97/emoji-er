package com.emoji_er;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.net.URISyntaxException;
import java.sql.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.core.entities.Game;

public class Reconnector {
    public static void reconnect(){
        boolean connected=false;
        Connection conn = null;

        while (!connected) {
            try {
                TimeUnit.SECONDS.sleep(5);
            }catch(Exception e){
                e.printStackTrace();
            }
            Logger.logGeneral("trying to reconnect to sql");
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                Logger.logGeneral("Missing postgresql JDBC Driver!");
                e.printStackTrace();
                connected = false;
                continue;
            }
            try {
                URI dbUri;
                try {
                    dbUri = new URI(System.getenv("DATABASE_URL"));
                }catch (URISyntaxException ex)
                {
                    Logger.logGeneral("URIException: " + ex.getMessage());
                    Logger.logGeneral("Reason: " + ex.getReason());
                    continue;
                }

                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() + "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory&" + "user=" + username + "&password=" + password;
                Logger.logGeneral("Connecting to: " + dbUrl);
                conn = DriverManager.getConnection(dbUrl);
                Logger.logGeneral("SQL INITIALIZZATED");
                connected = true;
            } catch (SQLException ex) {
                Logger.logGeneral("NOT CONNECTED RETRY IN 5 SEC");
                connected = false;
            }
        }
        try {
            JDA api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildAsync();
            api.addEventListener(new MyListener(conn));
            api.getPresence().setGame(Game.listening("suggestions :/"));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
