package com.emoji_er;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

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
            try {
                stmt = conn.createStatement();
                if(stmt.execute("SELECT * FROM roles WHERE guildid="+guild+" AND roleid="+role.getIdLong())) {
                    stmt.execute("DELETE FROM roles WHERE guildid=" + guild + " AND roleid=" + role.getIdLong());
                    stmt.execute("COMMIT");
                    ret = output.getString("modrole-remove");
                }else{
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
            try {
                stmt = conn.createStatement();
                if(!stmt.execute("SELECT * FROM roles WHERE guildid="+guild+" AND roleid="+role.getIdLong())) {
                    stmt.execute("INSERT INTO roles (guildid,roleid,rolename) VALUES (" + guild + "," + role.getIdLong() + ",'" + role.getName() + "')");
                    stmt.execute("COMMIT");
                    ret = output.getString("modrole-add");
                }else{
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

    BotGuild(Connection actconn)
    {
        this.conn = actconn;
    }

    public void autoModRole(Guild guild)
    {
        Statement stmt;
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
                    if(!stmt.execute("SELECT * FROM roles WHERE guildid="+guildId+" AND roleid="+role.getIdLong())) {
                        stmt.execute("INSERT INTO roles (guildid,roleid,rolename) VALUES (" + guildId + "," + role.getIdLong() + ",'" + role.getName() + "')");
                        stmt.execute("COMMIT");
                    }
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
