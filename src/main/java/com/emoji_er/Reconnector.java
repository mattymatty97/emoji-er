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
                String url= System.getenv("DATABASE_URL");
                String username = System.getenv("DATABASE_USER");
                String password = System.getenv("DATABASE_PASSWORD");
                Logger.logGeneral("Connecting to: "+ url);
                conn = DriverManager.getConnection("jdbc:"+url,username,password);
                Logger.logGeneral("SQL INITIALIZZATED");
                connected = true;
            } catch (SQLException ex) {
                Logger.logGeneral("NOT CONNECTED RETRY");
                connected = false;
            }
        }
        try {
            JDA api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildAsync();
            api.addEventListener(new MyListener(conn));
            api.getPresence().setGame(Game.playing("v1.6.0 - em prj"));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
