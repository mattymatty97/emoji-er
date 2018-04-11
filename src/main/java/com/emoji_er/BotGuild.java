package com.emoji_er;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

/**
 * @author mattymatty
 * local class for storing per guild informations.
 */
public class BotGuild {
    private Connection conn;

    public boolean memberIsMod(Member member,long guild)
    {
        List<Role> roles = member.getRoles();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT roleid FROM roles WHERE guildid="+guild);
            while (rs.next()){
                for (Role role : roles) {
                    if(role.getIdLong()==rs.getLong(1)){
                        rs.close();
                        stmt.close();
                        return true;
                    }
                }
            }
            rs.close();
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }

        return false;
    }

    public String removeModRole(Role role,long guild,ResourceBundle output)
    {
            String ret;
            Statement stmt ;
            ResultSet rs;
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT * FROM roles WHERE guildid="+guild+" AND roleid="+role.getIdLong());
                if(rs.next()) {
                    rs.close();
                    stmt.execute("DELETE FROM roles WHERE guildid=" + guild + " AND roleid=" + role.getIdLong());
                    stmt.execute("COMMIT");
                    ret = output.getString("modrole-remove");
                }else{
                    rs.close();
                    ret = output.getString("error-modrole-missing");
                }
                stmt.close();
            }catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
                return null;
            }
            return ret;
    }

    public String addModRole(Role role,long guild,ResourceBundle output)
    {
        String ret;
            Statement stmt;
            ResultSet rs;
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT * FROM roles WHERE guildid="+guild+" AND roleid="+role.getIdLong());
                if(!rs.next()) {
                    rs.close();
                    stmt.execute("INSERT INTO roles (guildid,roleid,rolename) VALUES (" + guild + "," + role.getIdLong() + ",'" + role.getName() + "')");
                    stmt.execute("COMMIT");
                    ret = output.getString("modrole-add");
                }else{
                    rs.close();
                    ret = output.getString("error-modrole-exists");
                }
                stmt.close();
            }catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
                return null;
            }
        return ret;
    }

    public String clearModrole(long guild,ResourceBundle output)
    {
        String ret;
        Statement stmt;
        try{
            stmt = conn.createStatement();
            stmt.execute("DELETE FROM roles WHERE guildid="+guild);
            stmt.execute("COMMIT ");
            ret= output.getString("modrole-clear");
            stmt.close();
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        }
        return ret;
    }
    public String listModrole(Guild guild,ResourceBundle output)
    {
        StringBuilder ret = new StringBuilder(output.getString("modrole-list"));
        Statement stmt;
        ResultSet rs;
        try{
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT roleid FROM roles WHERE guildid="+guild.getIdLong());
            while (rs.next()){
                Role role = guild.getRoleById(rs.getLong(1));
                if(role!=null)
                {
                    ret.append("\n");
                    ret.append(role.getName());
                }
            }
            rs.close();
            stmt.close();
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        }
        return ret.toString();
    }

    public String toggleEmoji(Guild guild,ResourceBundle output){
        StringBuilder ret = new StringBuilder(output.getString("toggle-head")).append(" ");
        try{
            Statement stmt;
            ResultSet rs;
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT enabled FROM guilds WHERE guildid="+guild.getIdLong());
            if(rs.next()){
                boolean enabled = rs.getBoolean(1);
                rs.close();
                stmt.execute("UPDATE guilds SET enabled="+!enabled+" WHERE guildid="+guild.getIdLong());
                ret.append(output.getString(enabled?"disabled":"enabled"));
                System.out.print("Emoji"+(!enabled?"ENABLED":"DISABLED)"));
            }else
                rs.close();
            stmt.close();
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        }
        return ret.toString();
    }

    public String registerGuild(long guildId,String title,ResourceBundle output){
        StringBuilder ret=new StringBuilder();
        Statement stmt;
        ResultSet rs;
        if(title.contains("emoji")){
            ret.append(output.getString("error-title-not_usable"));
        }else
            try
            {
                stmt=conn.createStatement();
                rs = stmt.executeQuery("SELECT * FROM registered_emoji_server WHERE guildid="+guildId);
                if(rs.next()){
                    ret.append(output.getString("error-emoji-registered"));
                    System.out.print("emoji not registered");
                    rs.close();
                }else {
                    rs.close();
                    rs = stmt.executeQuery("SELECT * FROM registered_emoji_server WHERE title='" + title + "'");
                    if (rs.next()) {
                        ret.append(output.getString("error-emoji-title-used"));
                        System.out.print("emoji not registered");
                    } else {
                        stmt.execute("INSERT INTO registered_emoji_server(guildid, title) VALUES (" + guildId + ",'" + title + "')");
                        ret.append(output.getString("emoji-guild-registered"));
                        System.out.print("emoji registered");
                    }
                }
                stmt.close();
            }catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
        return ret.toString();
    }

    public String unRegisterGuild(long guildId,ResourceBundle output){
        StringBuilder ret=new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try
        {
            stmt=conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM registered_emoji_server WHERE guildid="+guildId);
            if(rs.next()) {
                rs.close();
                stmt.execute("DELETE FROM disabled_emoji_servers WHERE emoji_guildID=" + guildId);
                stmt.execute("DELETE FROM registered_emoji_server WHERE guildid=" + guildId);
                System.out.print("emoji unregistered");
                ret.append(output.getString("emoji-guild-unregistered"));
            }else{
                System.out.print("emoji not unregistered");
                ret.append(output.getString("error-emoji-unregistered"));
            }
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public String getEmoji(String arg,long guildid,JDA api){
        String ret = null;
        Statement stmt;
        ResultSet rs;
        String args[] = arg.split("\\.");
        try
        {
            stmt=conn.createStatement();
            rs = stmt.executeQuery("SELECT R.guildid FROM registered_emoji_server R WHERE title='"+args[0]+"' AND R.guildid NOT IN (SELECT emoji_guildid FROM disabled_emoji_servers D WHERE D.guildid="+guildid+")");
            if(rs.next()) {
                Guild guild = api.getGuildById(rs.getLong(1));
                List<Emote> emoji = guild.getEmotesByName(args[1],false);
                if(emoji.size()==1)
                {
                    ret = emoji.get(0).getAsMention();
                }
            }
            rs.close();
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret;
    }

    public String getEmojiList(String title,JDA api){
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try
        {
            stmt=conn.createStatement();
            rs = stmt.executeQuery("SELECT guildid FROM registered_emoji_server WHERE title='"+title+"'");
            if(rs.next()) {
                Guild guild = api.getGuildById(rs.getLong(1));
                if(guild!=null) {
                    ret.append(guild.getName());
                    List<Emote> emoji = guild.getEmotes();
                    for (Emote emote : emoji) {
                        ret.append("\n");
                        ret.append(title).append(".");
                        ret.append(emote.getName());
                        ret.append("   ").append(emote.getAsMention());
                    }
                }
            }
            rs.close();
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public String printServers(long guildid,JDA api){
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try{
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT R.guildid,title FROM registered_emoji_server R WHERE R.guildid NOT IN (SELECT emoji_guildid FROM disabled_emoji_servers D WHERE D.guildid="+guildid+")");
            while (rs.next()){
                Guild guild = api.getGuildById(rs.getLong(1));
                if(guild!=null) {
                    ret.append("\n");
                    ret.append(rs.getString(2));
                    ret.append("   ");
                    ret.append(guild.getName());
                }
            }
            rs.close();
            rs = stmt.executeQuery("SELECT R.guildid,title FROM registered_emoji_server R WHERE R.guildid IN (SELECT emoji_guildid FROM disabled_emoji_servers D WHERE D.guildid="+guildid+")");
            while (rs.next()){
                Guild guild = api.getGuildById(rs.getLong(1));
                if(guild!=null) {
                    ret.append("\n");
                    ret.append("~~");
                    ret.append(rs.getString(2));
                    ret.append("   ");
                    ret.append(guild.getName());
                    ret.append("~~");
                }
            }
            rs.close();
            stmt.close();
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public String disableGuild(long guildId,String title,ResourceBundle output) {
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT guildid FROM registered_emoji_server WHERE title='"+title+"'");
            if(rs.next()) {
                long id = rs.getLong(1);
                rs.close();
                rs = stmt.executeQuery("SELECT * FROM disabled_emoji_servers WHERE guildid=" + guildId + " AND " +
                        "emoji_guildid="+id);
                if (rs.next()) {
                    ret.append(output.getString("error-disabled"));
                    System.out.print("emoji server already disabled");
                    rs.close();
                } else {
                    rs.close();
                    stmt.execute("INSERT INTO disabled_emoji_servers(guildid, emoji_guildid) VALUES ("+guildId+","+id+")");
                    ret.append(output.getString("disable-success"));
                    System.out.print(title+" server disabled");
                }
            }else{
                ret.append(output.getString("error-disabled-404"));
                System.out.print("emoji server not exist");
            }
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public String enableGuild(long guildId,String title,ResourceBundle output)
    {
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT guildid FROM registered_emoji_server WHERE title='"+title+"'");
            if(rs.next()) {
                long id = rs.getLong(1);
                rs.close();
                stmt.execute("DELETE FROM disabled_emoji_servers WHERE guildid="+guildId+" AND emoji_guildid="+id);
                ret.append(output.getString("enable-success"));
                System.out.print(title+" server enabled");
            }else{
                ret.append(output.getString("error-enable-404"));
                System.out.print("emoji server not exist");
            }
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public String printStatus(Guild guild,ResourceBundle output){
        String title=null;
        long disabled=0;
        boolean status=false;
        long modroles=0;
        boolean found=false;
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT G.enabled,R.title,RO.count,DS.count " +
                    "FROM guilds G " +
                    "FULL OUTER JOIN registered_emoji_server R ON G.guildid = R.guildid "+
                    "FULL OUTER JOIN ("+
                    "SELECT guildid,COUNT(*) as count "+
                    "FROM roles RL "+
                    "GROUP BY guildid "+
                    ") as RO ON G.guildid = RO.guildid "+
                    "FULL OUTER JOIN ("+
                    "SELECT guildid,COUNT(*) as count "+
                    "FROM disabled_emoji_servers DSA "+
                    "GROUP BY guildid "+
                    ")as DS ON G.guildid = DS.guildid "+
                    "WHERE G.guildid="+guild.getIdLong());
            if(rs.next()) {
                status=rs.getBoolean(1);
                title=rs.getString(2);
                modroles=rs.getLong(3);
                disabled=rs.getLong(4);
                found=true;
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        if(found){
            ret.append(output.getString("status-head")).append("\n");
            ret.append(output.getString("status-emoji")).append(" ").append(output.getString(status?"enabled":"disabled")).append("\n");
            ret.append(output.getString("status-registered")).append(" ").append(output.getString((title!=null)?"registered":"unregistered")).append("\n");
            ret.append(output.getString("status-title")).append(" ").append(title!=null?title:"").append("\n");
            ret.append(output.getString("status-modroles")).append(" ").append(modroles).append("\n");
            ret.append(output.getString("status-disabled")).append(" ").append(disabled).append("\n");
        }

        return ret.toString();
    }

    public boolean emojiEnabled(Guild guild){
        boolean ret=false;
        try{
            Statement stmt;
            ResultSet rs;
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT enabled FROM guilds WHERE guildid="+guild.getIdLong());
            if(rs.next()){
                boolean enabled = rs.getBoolean(1);
                rs.close();
                ret=enabled;
            }else
                rs.close();
            stmt.close();
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return false;
        }
        return ret;
    }

    BotGuild(Connection actconn)
    {
        this.conn = actconn;
    }

    public void autoModRole(Guild guild)
    {
        Statement stmt;
        ResultSet rs;
        long guildId = guild.getIdLong();
        for (Role role : guild.getRoles())
        {
            if(role.isManaged())
                continue;
            if(role.hasPermission(Permission.ADMINISTRATOR) ||
                    role.hasPermission(Permission.MANAGE_SERVER) ||
                    role.hasPermission(Permission.MANAGE_ROLES))
                try {
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery("SELECT * FROM roles WHERE guildid="+guildId+" AND roleid="+role.getIdLong());
                    if(!rs.next()) {
                        rs.close();
                        stmt.execute("INSERT INTO roles (guildid,roleid,rolename) VALUES (" + guildId + "," + role.getIdLong() + ",'" + role.getName() + "')");
                        stmt.execute("COMMIT");
                    }
                    rs.close();
                    stmt.close();
                }catch (SQLException ex) {
                    System.out.println("SQLException: " + ex.getMessage());
                    System.out.println("SQLState: " + ex.getSQLState());
                    System.out.println("VendorError: " + ex.getErrorCode());
                }
        }
    }

    public boolean onRoleDeleted(Role role)
    {
        boolean ret= false;
        Statement stmt ;
        try {
            stmt = conn.createStatement();
            if(stmt.execute("SELECT * FROM roles WHERE guildid="+role.getGuild().getIdLong()+" AND roleid="+role.getIdLong())) {
                stmt.execute("DELETE FROM roles WHERE guildid=" + role.getGuild().getIdLong() + " AND roleid=" + role.getIdLong());
                stmt.execute("COMMIT");
                ret = true;
            }
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret;
    }

}
