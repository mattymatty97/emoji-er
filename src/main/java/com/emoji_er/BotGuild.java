package com.emoji_er;

import java.awt.*;
import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

/**
 * @author mattymatty
 * local class for storing per guild informations.
 */
public class BotGuild {
    private Connection conn;

    public String removeModRole(Role role, Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        String ret;
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT * FROM roles WHERE guildid=" + guild.getId() + " AND roleid=" + role.getIdLong();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                rs.close();
                sql = "DELETE FROM roles WHERE guildid=" + guild.getId() + " AND roleid=" + role.getIdLong();
                stmt.execute(sql);
                stmt.execute("COMMIT");
                ret = output.getString("modrole-remove");
                Logger.logReponse("removed role " + role.getName(), guild, messageId);
            } else {
                rs.close();
                ret = output.getString("error-modrole-missing");
                Logger.logReponse("role not modrole", guild, messageId);
            }
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in : " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            
            return null;
        }
        return ret;
    }

    public String addModRole(Role role, Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        String ret;
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT * FROM roles WHERE guildid=" + guild.getId() + " AND roleid=" + role.getIdLong();
            rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                rs.close();
                sql = "INSERT INTO roles (guildid,roleid,rolename) VALUES (" + guild.getId() + "," + role.getIdLong() + ",'" + role.getName().replaceAll("[\',\"]","") + "')";
                stmt.execute(sql);
                stmt.execute("COMMIT");
                ret = output.getString("modrole-add");
                Logger.logReponse("added role " + role.getName(), guild, messageId);
            } else {
                rs.close();
                ret = output.getString("error-modrole-exists");
                Logger.logReponse("role is modrole", guild, messageId);
            }
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return "";
        }
        return ret;
    }

    public String clearModrole(Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        String ret;
        Statement stmt;
        try {
            stmt = conn.createStatement();
            sql = "DELETE FROM roles WHERE guildid=" + guild.getId();
            stmt.execute(sql);
            stmt.execute("COMMIT ");
            ret = output.getString("modrole-clear");
            Logger.logReponse("cleared modroles", guild, messageId);
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return "";
        }
        return ret;
    }

    public String listModrole(Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder(output.getString("modrole-list"));
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT roleid FROM roles WHERE guildid=" + guild.getIdLong();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Role role = guild.getRoleById(rs.getLong(1));
                if (role != null) {
                    ret.append("\n");
                    ret.append(role.getName());
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            Logger.logGeneral(ex.getStackTrace()[1].toString());
            return "";
        }
        Logger.logReponse("listed modroles", guild, messageId);
        return ret.toString();
    }

    public String toggleEmoji(Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder(output.getString("toggle-head")).append(" ");
        try {
            Statement stmt;
            ResultSet rs;
            stmt = conn.createStatement();
            sql = "SELECT enabled FROM guilds WHERE guildid=" + guild.getIdLong();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                boolean enabled = rs.getBoolean(1);
                rs.close();
                sql = "UPDATE guilds SET enabled=" + !enabled + " WHERE guildid=" + guild.getIdLong();
                stmt.execute(sql);
                ret.append(output.getString(enabled ? "disabled" : "enabled"));
                Logger.logReponse("Emoji" + (!enabled ? "ENABLED" : "DISABLED)"), guild, messageId);
            } else
                rs.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return "";
        }
        return ret.toString();
    }

    public String registerGuild(Guild guild, String title, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        if(!title.matches("[\\w\\d]+")) {
            ret.append(output.getString("error-title-unallowed"));
        }else{
            if (title.contains("emoji")) {
                ret.append(output.getString("error-title-emoji"));
            } else
                try {
                    stmt = conn.createStatement();
                    sql = "SELECT * FROM registered_emoji_server WHERE guildid=" + guild.getId();
                    rs = stmt.executeQuery(sql);
                    if (rs.next()) {
                        ret.append(output.getString("error-emoji-registered"));
                        Logger.logReponse("guild found", guild, messageId);
                        rs.close();
                    } else {
                        rs.close();
                        sql = "SELECT * FROM registered_emoji_server WHERE title='" + title + "'";
                        rs = stmt.executeQuery(sql);
                        if (rs.next()) {
                            ret.append(output.getString("error-emoji-title-used"));
                            Logger.logReponse("title used", guild, messageId);
                        } else {
                            sql = "INSERT INTO registered_emoji_server(guildid, title) VALUES (" + guild.getId() + ",'" + title + "')";
                            stmt.execute(sql);
                            ret.append(output.getString("emoji-guild-registered"));
                            Logger.logReponse("guild registered", guild, messageId);
                        }
                    }
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.logGeneral("SQLError in: " + sql);
                    Logger.logGeneral(ex.getMessage());
                    Logger.logGeneral("SQLState: " + ex.getSQLState());
                    Logger.logGeneral("VendorError: " + ex.getErrorCode());
                    return "";
                }
        }
        return ret.toString();
    }

    public String unRegisterGuild(Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT * FROM registered_emoji_server WHERE guildid=" + guild.getId();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                rs.close();
                sql = "DELETE FROM disabled_emoji_servers WHERE emoji_guildID=" + guild.getId();
                stmt.execute(sql);
                sql = "DELETE FROM registered_emoji_server WHERE guildid=" + guild.getId();
                stmt.execute(sql);
                Logger.logReponse("guild unregistered", guild, messageId);
                ret.append(output.getString("emoji-guild-unregistered"));
            } else {
                Logger.logReponse("guild was not registered", guild, messageId);
                ret.append(output.getString("error-emoji-unregistered"));
            }
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return "";
        }
        return ret.toString();
    }

    public String getEmoji(String arg, long guildId, JDA api) {
        String sql = "";
        String ret = null;
        Statement stmt;
        ResultSet rs;
        String args[] = arg.split("\\.");
        try {
            stmt = conn.createStatement();
            sql = "SELECT R.guildid " +
                    "FROM registered_emoji_server R " +
                    "WHERE title='" + args[0] + "' " +
                    "AND R.guildid NOT IN (" +
                    "SELECT emoji_guildid " +
                    "FROM disabled_emoji_servers D " +
                    "WHERE D.guildid=" + guildId + ")";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                Guild guild = api.getGuildById(rs.getLong(1));
                List<Emote> emoji = guild.getEmotesByName(args[1], false);
                if (emoji.size() == 1) {
                    ret = emoji.get(0).getAsMention();
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return "";
        }
        return ret;
    }

    public String getEmojiList(String title, JDA api) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT guildid FROM registered_emoji_server WHERE title='" + title + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                Guild guild = api.getGuildById(rs.getLong(1));
                if (guild != null) {
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
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return "";
        }
        return ret.toString();
    }

    public String printServers(long guildId, JDA api, ResourceBundle output) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT R.guildid ,R.title," +
                    "(CASE " +
                    "WHEN guildid IN (" +
                    "SELECT emoji_guildid " +
                    "FROM disabled_emoji_servers D " +
                    "WHERE D.guildid=" + guildId + " )" +
                    "THEN TRUE " +
                    "ELSE FALSE " +
                    "END) as disabled " +
                    "FROM registered_emoji_server R " +
                    "ORDER BY disabled";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Guild guild = api.getGuildById(rs.getLong(1));
                if (guild != null) {
                    ret.append("\n");
                    if (rs.getBoolean(3))
                        ret.append("~~");
                    ret.append(rs.getString(2));
                    for(int i=0;i<(15-rs.getString(2).length());i++)
                        ret.append(" ");
                    ret.append(" ");
                    ret.append(guild.getName());
                    if (rs.getBoolean(3))
                        ret.append("~~");
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return "";
        }
        return ret.toString();
    }

    public String disableGuild(Guild guild, String title, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT guildid FROM registered_emoji_server WHERE title='" + title + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                long id = rs.getLong(1);
                rs.close();
                sql = "SELECT * FROM disabled_emoji_servers WHERE guildid=" + guild.getId() + " AND " + "emoji_guildid=" + id;
                rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    ret.append(output.getString("error-disabled"));
                    Logger.logReponse("guild is disabled", guild, messageId);
                    rs.close();
                } else {
                    rs.close();
                    sql = "INSERT INTO disabled_emoji_servers(guildid, emoji_guildid) VALUES (" + guild.getId() + "," + id + ")";
                    stmt.execute(sql);
                    ret.append(output.getString("disable-success"));
                    Logger.logReponse(title + " server disabled", guild, messageId);
                }
            } else {
                ret.append(output.getString("error-disabled-404"));
                Logger.logReponse("guild not registered", guild, messageId);
            }
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return "";
        }
        return ret.toString();
    }

    public String enableGuild(Guild guild, String title, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT guildid FROM registered_emoji_server WHERE title='" + title + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                long id = rs.getLong(1);
                rs.close();
                sql = "DELETE FROM disabled_emoji_servers WHERE guildid=" + guild.getId() + " AND emoji_guildid=" + id;
                stmt.execute(sql);
                ret.append(output.getString("enable-success"));
                Logger.logReponse(title + " server enabled", guild, messageId);
            } else {
                ret.append(output.getString("error-enable-404"));
                Logger.logReponse("guild not registered", guild, messageId);
            }
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return "";
        }
        return ret.toString();
    }

    public MessageEmbed printStatus(Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        String title = null;
        long disabled = 0;
        boolean status = false;
        long modroles = 0;
        boolean found = false;
        EmbedBuilder ret = new EmbedBuilder();
        ret.setColor(Color.GREEN);
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT G.enabled,R.title,RO.count,DS.count " +
                    "FROM guilds G " +
                    "FULL OUTER JOIN registered_emoji_server R ON G.guildid = R.guildid " +
                    "FULL OUTER JOIN (" +
                    "SELECT guildid,COUNT(*) as count " +
                    "FROM roles RL " +
                    "GROUP BY guildid " +
                    ") as RO ON G.guildid = RO.guildid " +
                    "FULL OUTER JOIN (" +
                    "SELECT guildid,COUNT(*) as count " +
                    "FROM disabled_emoji_servers DSA " +
                    "GROUP BY guildid " +
                    ")as DS ON G.guildid = DS.guildid " +
                    "WHERE G.guildid=" + guild.getIdLong();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                status = rs.getBoolean(1);
                title = rs.getString(2);
                modroles = rs.getLong(3);
                disabled = rs.getLong(4);
                found = true;
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            
            return ret.build();
        }
        if (found) {
            ret.setTitle(output.getString("status-head"));
            ret.addField(output.getString("status-emoji"), output.getString(status ? "enabled" : "disabled"), false);
            ret.addField(output.getString("status-registered"), output.getString((title != null) ? "registered" : "unregistered"), false);
            ret.addField(output.getString("status-title"), title != null ? title : "", false);
            ret.addField(output.getString("status-modroles"), Long.toString(modroles), false);
            ret.addField(output.getString("status-disabled"), Long.toString(disabled), false);
            Logger.logReponse("printed status", guild, messageId);
        }

        return ret.build();
    }


    BotGuild(Connection actconn) {
        this.conn = actconn;
    }

    public boolean memberIsMod(Member member, long guild) {
        String sql = "";
        List<Role> roles = member.getRoles();
        Statement stmt;
        ResultSet rs;
        if(member.getUser().getIdLong()==Long.parseLong(System.getenv("OWNER_ID")))
            return true;
        try {
            stmt = conn.createStatement();
            sql = "SELECT roleid FROM roles WHERE guildid=" + guild;
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                for (Role role : roles) {
                    if (role.getIdLong() == rs.getLong(1)) {
                        rs.close();
                        stmt.close();
                        return true;
                    }
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
        }

        return false;
    }

    public boolean emojiEnabled(Guild guild) {
        String sql = "";
        boolean ret = false;
        try {
            Statement stmt;
            ResultSet rs;
            stmt = conn.createStatement();
            sql = "SELECT enabled FROM guilds WHERE guildid=" + guild.getIdLong();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                boolean enabled = rs.getBoolean(1);
                rs.close();
                ret = enabled;
            } else
                rs.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("AQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            return false;
        }
        return ret;
    }

    public void autoModRole(Guild guild) {
        String sql = "";
        Statement stmt;
        ResultSet rs;
        long guildId = guild.getIdLong();
        for (Role role : guild.getRoles()) {
            if (role.isManaged())
                continue;
            if (role.hasPermission(Permission.ADMINISTRATOR) ||
                    role.hasPermission(Permission.MANAGE_SERVER) ||
                    role.hasPermission(Permission.MANAGE_ROLES))
                try {
                    stmt = conn.createStatement();
                    sql = "SELECT * FROM roles WHERE guildid=" + guildId + " AND roleid=" + role.getIdLong();
                    rs = stmt.executeQuery(sql);
                    if (!rs.next()) {
                        rs.close();
                        sql = "INSERT INTO roles (guildid,roleid,rolename) VALUES (" + guildId + "," + role.getIdLong() + ",'" + role.getName().replaceAll("[\',\"]","") + "')";
                        stmt.execute(sql);
                        stmt.execute("COMMIT");
                    }
                    rs.close();
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.logGeneral("SQLError in: " + sql);
                    Logger.logGeneral(ex.getMessage());
                    Logger.logGeneral("SQLState: " + ex.getSQLState());
                    Logger.logGeneral("VendorError: " + ex.getErrorCode());
                }
        }
    }

    public boolean onRoleDeleted(Role role) {
        String sql = "";
        boolean ret = false;
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            sql = "SELECT * FROM roles WHERE guildid=" + role.getGuild().getIdLong() + " AND roleid=" + role.getIdLong();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                rs.close();
                sql = "DELETE FROM roles WHERE guildid=" + role.getGuild().getIdLong() + " AND roleid=" + role.getIdLong();
                stmt.execute(sql);
                stmt.execute("COMMIT");
                ret = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral(ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
        }
        return ret;
    }

}
