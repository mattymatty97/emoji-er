package com.emoji_er;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author mattymatty
 * local class for storing per guild informations.
 */
public class BotGuild {
        private PreparedStatement[] rmModStmt = new PreparedStatement[2];
        private PreparedStatement[] adModStmt = new PreparedStatement[2];
        private  PreparedStatement clModStmt;
        private  PreparedStatement lsModStmt;
        private  PreparedStatement[] tgStmt = new PreparedStatement[2];
        private  PreparedStatement[] rgStmt = new PreparedStatement[3];
        private  PreparedStatement[] unRgStmt = new PreparedStatement[3];
        private  PreparedStatement emStmt;
        private  PreparedStatement emLsStmt;
        private  PreparedStatement pSerStmt;
        private  PreparedStatement[] disStmt = new PreparedStatement[3];
        private  PreparedStatement[] enStmt = new PreparedStatement[2];
        private  PreparedStatement pStaStmt;
        private  PreparedStatement isModStmt;
        private  PreparedStatement eEnStmt;
        private  PreparedStatement[] delRolStmt = new PreparedStatement[2];
        private  PreparedStatement[] aModStmt = new PreparedStatement[2];

        private List<PreparedStatement> stmts = new ArrayList<>(29);



    public String removeModRole(Role role, Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        String ret;
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = rmModStmt[0];
            sql = "SELECT * FROM roles WHERE guildid=" + guild.getId() + " AND roleid=" + role.getIdLong();
            synchronized (rmModStmt) {
                stmt.setLong(1, guild.getIdLong());
                stmt.setLong(2, role.getIdLong());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    rs.close();
                    stmt = rmModStmt[1];
                    sql = "DELETE FROM roles WHERE guildid=" + guild.getId() + " AND roleid=" + role.getIdLong();
                    stmt.setLong(1, guild.getIdLong());
                    stmt.setLong(2, role.getIdLong());
                    stmt.executeUpdate();
                    stmt.getConnection().commit();
                    ret = output.getString("modrole-remove");
                    Logger.logger.logReponse("removed role " + role.getName(), guild, messageId);
                } else {
                    rs.close();
                    ret = output.getString("error-modrole-missing");
                    Logger.logger.logReponse("role not modrole", guild, messageId);
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret;
    }



    public String addModRole(Role role, Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        String ret;
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = adModStmt[0];
            sql = "SELECT * FROM roles WHERE guildid=" + guild.getId() + " AND roleid=" + role.getIdLong();
            synchronized (adModStmt) {
                stmt.setLong(1, guild.getIdLong());
                stmt.setLong(2, role.getIdLong());
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    rs.close();
                    stmt = adModStmt[1];
                    sql = "INSERT INTO roles (guildid,roleid,rolename) VALUES (" + guild.getId() + "," + role.getIdLong() + ",'" + role.getName().replaceAll("[\',\"]", "") + "')";
                    stmt.setLong(1, guild.getIdLong());
                    stmt.setLong(2, role.getIdLong());
                    stmt.setString(3, role.getName().replaceAll("[\',\"]", ""));
                    stmt.execute();
                    stmt.getConnection().commit();
                    ret = output.getString("modrole-add");
                    Logger.logger.logReponse("added role " + role.getName(), guild, messageId);
                } else {
                    rs.close();
                    ret = output.getString("error-modrole-exists");
                    Logger.logger.logReponse("role is modrole", guild, messageId);
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret;
    }

    public String clearModrole(Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        String ret;
        PreparedStatement stmt;
        try {
            stmt = clModStmt;
            sql = "DELETE FROM roles WHERE guildid=" + guild.getId();
            synchronized (clModStmt) {
                stmt.setLong(1, guild.getIdLong());
                stmt.executeUpdate();
                stmt.getConnection().commit();
            }
            ret = output.getString("modrole-clear");
            Logger.logger.logReponse("cleared modroles", guild, messageId);
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret;
    }

    public String listModrole(Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder(output.getString("modrole-list"));
        PreparedStatement stmt;
        try {
            stmt = lsModStmt;
            sql = "SELECT roleid FROM roles WHERE guildid=" + guild.getIdLong();
            syncModroleList(guild, ret, stmt);
        } catch (SQLException ex) {
            return sqlError(sql,ex);
        }
        Logger.logger.logReponse("listed modroles", guild, messageId);
        return ret.toString();
    }

    public String toggleEmoji(Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder(output.getString("toggle-head")).append(" ");
        boolean enabled;
        try {
            PreparedStatement stmt;
            ResultSet rs;
            stmt = tgStmt[0];
            sql = "SELECT enabled FROM guilds WHERE guildid=" + guild.getIdLong();
            synchronized (tgStmt) {
                stmt.setLong(1, guild.getIdLong());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    enabled = rs.getBoolean(1);
                    rs.close();
                    sql = "UPDATE guilds SET enabled=" + !enabled + " WHERE guildid=" + guild.getIdLong();
                    stmt = tgStmt[1];
                    stmt.setBoolean(1, !enabled);
                    stmt.setLong(2, guild.getIdLong());
                    stmt.executeUpdate();
                    stmt.getConnection().commit();
                    ret.append(output.getString(enabled ? "disabled" : "enabled"));
                    Logger.logger.logReponse("Emoji" + (!enabled ? "ENABLED" : "DISABLED)"), guild, messageId);
                } else
                    rs.close();
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret.toString();
    }

    public String registerGuild(Guild guild, String title, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        if(!title.matches("[\\w\\d]+")) {
            ret.append(output.getString("error-title-unallowed"));
            Logger.logger.logReponse("unallowed char", guild, messageId);
        }else{
            if (title.contains("emoji")) {
                ret.append(output.getString("error-title-emoji"));
            } else
                try {
                    stmt = rgStmt[0];
                    sql = "SELECT * FROM registered_emoji_server WHERE guildid=" + guild.getId();
                    synchronized (rgStmt) {
                        stmt.setLong(1, guild.getIdLong());
                        rs = stmt.executeQuery();
                        if (rs.next()) {
                            ret.append(output.getString("error-emoji-registered"));
                            Logger.logger.logReponse("guild found", guild, messageId);
                            rs.close();
                        } else {
                            rs.close();
                            stmt = rgStmt[1];
                            sql = "SELECT * FROM registered_emoji_server WHERE title='" + title + "'";
                            stmt.setString(1, title);
                            rs = stmt.executeQuery();
                            if (rs.next()) {
                                ret.append(output.getString("error-emoji-title-used"));
                                Logger.logger.logReponse("title used", guild, messageId);
                            } else {
                                stmt = rgStmt[2];
                                sql = "INSERT INTO registered_emoji_server(guildid, title) VALUES (" + guild.getId() + ",'" + title + "')";
                                stmt.setLong(1, guild.getIdLong());
                                stmt.setString(2, title);
                                stmt.executeUpdate();
                                stmt.getConnection().commit();
                                ret.append(output.getString("emoji-guild-registered"));
                                Logger.logger.logReponse("guild registered", guild, messageId);
                            }
                        }
                    }
                } catch (SQLException ex) {
                    return sqlError(sql, ex);
                }
        }
        return ret.toString();
    }

    public String unRegisterGuild(Guild guild, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = unRgStmt[0];
            sql = "SELECT * FROM registered_emoji_server WHERE guildid=" + guild.getId();
            synchronized (unRgStmt) {
                stmt.setLong(1, guild.getIdLong());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    rs.close();
                    stmt = unRgStmt[1];
                    sql = "DELETE FROM disabled_emoji_servers WHERE emoji_guildID=" + guild.getId();
                    stmt.setLong(1, guild.getIdLong());
                    stmt.executeUpdate();
                    stmt = unRgStmt[2];
                    stmt.setLong(1, guild.getIdLong());
                    sql = "DELETE FROM registered_emoji_server WHERE guildid=" + guild.getId();
                    stmt.executeUpdate();
                    stmt.getConnection().commit();
                    Logger.logger.logReponse("guild unregistered", guild, messageId);
                    ret.append(output.getString("emoji-guild-unregistered"));
                } else {
                    Logger.logger.logReponse("guild was not registered", guild, messageId);
                    ret.append(output.getString("error-emoji-unregistered"));
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret.toString();
    }

    public String getEmoji(String arg, long guildId, JDA api) {
        String sql = "";
        String ret = null;
        PreparedStatement stmt;
        ResultSet rs;
        String args[] = arg.split("\\.");
        try {
            stmt = emStmt;
            sql = "SELECT R.guildid " +
                    "FROM registered_emoji_server R " +
                    "WHERE title='" + args[0] + "' " +
                    "AND R.guildid NOT IN (" +
                    "SELECT emoji_guildid " +
                    "FROM disabled_emoji_servers D " +
                    "WHERE D.guildid=" + guildId + ")";
            synchronized (emStmt) {
                stmt.setString(1, args[0]);
                stmt.setLong(2, guildId);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    Guild guild = api.getGuildById(rs.getLong(1));
                    List<Emote> emoji = guild.getEmotesByName(args[1], false);
                    if (emoji.size() == 1) {
                        ret = emoji.get(0).getAsMention();
                    }
                }
                rs.close();
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret;
    }

    public String getEmojiList(String title, JDA api) throws EmojiError{
        String sql = "";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = emLsStmt;
            sql = "SELECT guildid FROM registered_emoji_server WHERE title='" + title + "'";
            synchronized (emStmt) {
                stmt.setString(1, title);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    Guild guild = api.getGuildById(rs.getLong(1));
                    if (guild != null) {
                        ret.append(guild.getName());
                        List<Emote> emoji = guild.getEmotes();
                        for (Emote emote : emoji) {
                            ret.append("\n");
                            ret.append(emote.getAsMention());
                            ret.append("   :").append(title).append(".");
                            ret.append(emote.getName()).append(":");

                        }
                    }
                    rs.close();
                }else{
                    rs.close();
                    throw new EmojiError("error-list-404");
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret.toString();
    }

    public String printServers(long guildId, JDA api) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = pSerStmt;
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
            synchronized (pSerStmt) {
                stmt.setLong(1, guildId);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    Guild guild = api.getGuildById(rs.getLong(1));
                    if (guild != null) {
                        ret.append("\n");
                        if (rs.getBoolean(3))
                            ret.append("-");
                        else
                            ret.append("+");
                        ret.append(rs.getString(2));
                        for (int i = 0; i < (15 - rs.getString(2).length()); i++)
                            ret.append(" ");
                        ret.append(" ");
                        ret.append(guild.getName());
                    }
                }
                rs.close();
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret.toString();
    }

    public String disableGuild(Guild guild, String title, ResourceBundle output, long messageId) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = disStmt[0];
            sql = "SELECT guildid FROM registered_emoji_server WHERE title='" + title + "'";
            synchronized (disStmt) {
                stmt.setString(1, title);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    long id = rs.getLong(1);
                    rs.close();
                    stmt = disStmt[1];
                    sql = "SELECT * FROM disabled_emoji_servers WHERE guildid=" + guild.getId() + " AND " + "emoji_guildid=" + id;
                    stmt.setLong(1, guild.getIdLong());
                    stmt.setLong(2, id);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        ret.append(output.getString("error-disabled"));
                        Logger.logger.logReponse("guild is disabled", guild, messageId);
                        rs.close();
                    } else {
                        rs.close();
                        stmt = disStmt[2];
                        sql = "INSERT INTO disabled_emoji_servers(guildid, emoji_guildid) VALUES (" + guild.getId() + "," + id + ")";
                        stmt.setLong(1, guild.getIdLong());
                        stmt.setLong(2, id);
                        stmt.execute();
                        stmt.getConnection().commit();
                        ret.append(output.getString("disable-success"));
                        Logger.logger.logReponse(title + " server disabled", guild, messageId);
                    }
                } else {
                    ret.append(output.getString("error-disabled-404"));
                    Logger.logger.logReponse("guild not registered", guild, messageId);
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret.toString();
    }

    public String enableGuild(Guild guild, String title, ResourceBundle output, long messageId) {
        String sql="";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = enStmt[0];
            sql = "SELECT guildid FROM registered_emoji_server WHERE title='" + title + "'";
            synchronized (enStmt) {
                stmt.setString(1, title);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    long id = rs.getLong(1);
                    rs.close();
                    stmt = enStmt[1];
                    sql = "DELETE FROM disabled_emoji_servers WHERE guildid=" + guild.getId() + " AND emoji_guildid=" + id;
                    stmt.setLong(1, guild.getIdLong());
                    stmt.setLong(2, id);
                    stmt.execute();
                    stmt.getConnection().commit();
                    ret.append(output.getString("enable-success"));
                    Logger.logger.logReponse(title + " server enabled", guild, messageId);
                } else {
                    ret.append(output.getString("error-enable-404"));
                    Logger.logger.logReponse("guild not registered", guild, messageId);
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
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
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = pStaStmt;
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
            synchronized (pStaStmt) {
                stmt.setLong(1, guild.getIdLong());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    status = rs.getBoolean(1);
                    title = rs.getString(2);
                    modroles = rs.getLong(3);
                    disabled = rs.getLong(4);
                    found = true;
                }
                rs.close();
            }
        } catch (SQLException ex) {
            sqlError(sql, ex);
            return ret.build();
        }
        if (found) {
            ret.setTitle(output.getString("status-head"));
            ret.addField(output.getString("status-emoji"), output.getString(status ? "enabled" : "disabled"), false);
            ret.addField(output.getString("status-registered"), output.getString((title != null) ? "registered" : "unregistered"), false);
            ret.addField(output.getString("status-title"), title != null ? title : "", false);
            ret.addField(output.getString("status-modroles"), Long.toString(modroles), false);
            ret.addField(output.getString("status-disabled"), Long.toString(disabled), false);
            Logger.logger.logReponse("printed status", guild, messageId);
        }

        return ret.build();
    }



    public boolean memberIsMod(Member member, long guild) {
        String sql = "";
        List<Role> roles = member.getRoles();
        PreparedStatement stmt;
        ResultSet rs;
        if(member.getUser().getIdLong()==Long.parseLong(System.getenv("OWNER_ID")))
            return true;
        try {
            stmt = isModStmt;
            sql = "SELECT roleid FROM roles WHERE guildid=" + guild;
            synchronized (isModStmt) {
                stmt.setLong(1, guild);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    for (Role role : roles) {
                        if (role.getIdLong() == rs.getLong(1)) {
                            rs.close();
                            return true;
                        }
                    }
                }
                rs.close();
            }
        } catch (SQLException ex) {
            sqlError(sql,ex);
        }

        return false;
    }

    public boolean emojiEnabled(Guild guild) {
        String sql = "";
        boolean ret = false;
        try {
            PreparedStatement stmt;
            ResultSet rs;
            stmt = eEnStmt;
            sql = "SELECT enabled FROM guilds WHERE guildid=" + guild.getIdLong();
            synchronized (eEnStmt) {
                stmt.setLong(1, guild.getIdLong());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    boolean enabled = rs.getBoolean(1);
                    rs.close();
                    ret = enabled;
                } else
                    rs.close();
            }
        } catch (SQLException ex) {
            sqlError(sql,ex);
            return false;
        }
        return ret;
    }

    public void autoModRole(Guild guild) {
        String sql = "";
        PreparedStatement stmt;
        ResultSet rs;
        long guildId = guild.getIdLong();
        for (Role role : guild.getRoles()) {
            if (role.isManaged())
                continue;
            if (role.hasPermission(Permission.ADMINISTRATOR) ||
                    role.hasPermission(Permission.MANAGE_SERVER) ||
                    role.hasPermission(Permission.MANAGE_ROLES))
                try {
                    stmt = aModStmt[0];
                    sql = "SELECT * FROM roles WHERE guildid=" + guildId + " AND roleid=" + role.getIdLong();
                    synchronized (aModStmt) {
                        stmt.setLong(1, guildId);
                        stmt.setLong(2, role.getIdLong());
                        rs = stmt.executeQuery();
                        if (!rs.next()) {
                            rs.close();
                            stmt = aModStmt[1];
                            sql = "INSERT INTO roles (guildid,roleid,rolename) VALUES (" + guildId + "," + role.getIdLong() + ",'" + role.getName().replaceAll("[\',\"]", "") + "')";
                            stmt.setLong(1, guildId);
                            stmt.setLong(2, role.getIdLong());
                            stmt.setString(3, role.getName().replaceAll("[\',\"]", ""));
                            stmt.execute();
                            stmt.getConnection().commit();
                        }
                        rs.close();
                    }
                } catch (SQLException ex) {
                    sqlError(sql,ex);
                }
        }
    }

    public boolean onRoleDeleted(Role role) {
        String sql = "";
        boolean ret = false;
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = delRolStmt[0];
            sql = "SELECT * FROM roles WHERE guildid=" + role.getGuild().getIdLong() + " AND roleid=" + role.getIdLong();
            synchronized (delRolStmt) {
                stmt.setLong(1, role.getGuild().getIdLong());
                stmt.setLong(2, role.getIdLong());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    rs.close();
                    stmt = delRolStmt[1];
                    sql = "DELETE FROM roles WHERE guildid=" + role.getGuild().getIdLong() + " AND roleid=" + role.getIdLong();
                    stmt.setLong(1, role.getGuild().getIdLong());
                    stmt.setLong(2, role.getIdLong());
                    stmt.executeUpdate();
                    stmt.getConnection().commit();
                    ret = true;
                }
            }
        } catch (SQLException ex) {
            sqlError(sql,ex);
        }
        return ret;
    }



    //remote methods

    public String disableRemoteGuild(Guild guild, String title, ResourceBundle output, long messageId,Guild remote) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = disStmt[0];
            sql = "(remote) SELECT guildid FROM registered_emoji_server WHERE title='" + title + "'";
            synchronized (disStmt) {
                stmt.setString(1, title);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    long id = rs.getLong(1);
                    rs.close();
                    stmt = disStmt[1];
                    sql = "(remote) SELECT * FROM disabled_emoji_servers WHERE guildid=" + guild.getId() + " AND " + "emoji_guildid=" + id;
                    stmt.setLong(1, guild.getIdLong());
                    stmt.setLong(2, id);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        ret.append(output.getString("error-disabled"));
                        Logger.logger.logRemoteRep("guild is disabled", guild, messageId, remote);
                        rs.close();
                    } else {
                        rs.close();
                        stmt = disStmt[2];
                        sql = "(remote) INSERT INTO disabled_emoji_servers(guildid, emoji_guildid) VALUES (" + guild.getId() + "," + id + ")";
                        stmt.setLong(1, guild.getIdLong());
                        stmt.setLong(2, id);
                        stmt.executeUpdate();
                        stmt.getConnection().commit();
                        ret.append(output.getString("disable-success"));
                        Logger.logger.logRemoteRep(title + " server disabled", guild, messageId, remote);
                    }
                } else {
                    ret.append(output.getString("error-disabled-404"));
                    Logger.logger.logReponse("guild not registered", guild, messageId);
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret.toString();
    }

    public String enableRemoteGuild(Guild guild, String title, ResourceBundle output, long messageId,Guild remote) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = enStmt[0];
            sql = "(remote) SELECT guildid FROM registered_emoji_server WHERE title='" + title + "'";
            synchronized (enStmt) {
                stmt.setString(1, title);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    long id = rs.getLong(1);
                    rs.close();
                    stmt = enStmt[1];
                    sql = "(remote) DELETE FROM disabled_emoji_servers WHERE guildid=" + guild.getId() + " AND emoji_guildid=" + id;
                    stmt.setLong(1, guild.getIdLong());
                    stmt.setLong(2, id);
                    stmt.executeUpdate();
                    stmt.getConnection().commit();
                    ret.append(output.getString("enable-success"));
                    Logger.logger.logRemoteRep(title + " server enabled", guild, messageId, remote);
                } else {
                    ret.append(output.getString("error-enable-404"));
                    Logger.logger.logRemoteRep("guild not registered", guild, messageId, remote);
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret.toString();
    }

    public String registerRemoteGuild(Guild guild, String title, ResourceBundle output, long messageId,Guild remote) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        if(!title.matches("[\\w\\d]+")) {
            ret.append(output.getString("error-title-unallowed"));
            Logger.logger.logRemoteRep("un-allowed chars", guild, messageId,remote);
        }else{
            if (title.contains("emoji")) {
                ret.append(output.getString("error-title-emoji"));
            } else
                try {
                    stmt = rgStmt[0];
                    sql = "SELECT * FROM registered_emoji_server WHERE guildid=" + guild.getId();
                    synchronized (rgStmt) {
                        stmt.setLong(1, guild.getIdLong());
                        rs = stmt.executeQuery();
                        if (rs.next()) {
                            ret.append(output.getString("error-emoji-registered"));
                            Logger.logger.logRemoteRep("guild found", guild, messageId, remote);
                            rs.close();
                        } else {
                            rs.close();
                            stmt = rgStmt[1];
                            sql = "SELECT * FROM registered_emoji_server WHERE title='" + title + "'";
                            stmt.setString(1, title);
                            rs = stmt.executeQuery();
                            if (rs.next()) {
                                ret.append(output.getString("error-emoji-title-used"));
                                Logger.logger.logRemoteRep("title used", guild, messageId, remote);
                            } else {
                                stmt = rgStmt[2];
                                sql = "INSERT INTO registered_emoji_server(guildid, title) VALUES (" + guild.getId() + ",'" + title + "')";
                                stmt.setLong(1, guild.getIdLong());
                                stmt.setString(2, title);
                                stmt.executeUpdate();
                                stmt.getConnection().commit();
                                ret.append(output.getString("emoji-guild-registered"));
                                Logger.logger.logRemoteRep("guild registered", guild, messageId, remote);
                            }
                        }
                    }
                } catch (SQLException ex) {
                    return sqlError(sql,ex);
                }
        }
        return ret.toString();
    }

    public String unRegisterRemoteGuild(Guild guild, ResourceBundle output, long messageId,Guild remote) {
        String sql = "";
        StringBuilder ret = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = unRgStmt[0];
            sql = "(remote) SELECT * FROM registered_emoji_server WHERE guildid=" + guild.getId();
            synchronized (unRgStmt) {
                stmt.setLong(1, guild.getIdLong());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    rs.close();
                    stmt = unRgStmt[1];
                    sql = "(remote) DELETE FROM disabled_emoji_servers WHERE emoji_guildID=" + guild.getId();

                    stmt.setLong(1, guild.getIdLong());
                    stmt.executeUpdate();
                    stmt = unRgStmt[2];
                    sql = "(remote) DELETE FROM registered_emoji_server WHERE guildid=" + guild.getId();
                    stmt.setLong(1, guild.getIdLong());
                    stmt.executeUpdate();
                    stmt.getConnection().commit();
                    Logger.logger.logRemoteRep("guild unregistered", guild, messageId, remote);
                    ret.append(output.getString("emoji-guild-unregistered"));
                } else {
                    Logger.logger.logRemoteRep("guild was not registered", guild, messageId, remote);
                    ret.append(output.getString("error-emoji-unregistered"));
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret.toString();
    }

    public String removeRemoteModRole(Role role, Guild guild, ResourceBundle output, long messageId,Guild remote) {
        String sql = "";
        String ret;
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = rmModStmt[0];
            sql = "(remote) SELECT * FROM roles WHERE guildid=" + guild.getId() + " AND roleid=" + role.getIdLong();
            synchronized (rmModStmt) {
                stmt.setLong(1, guild.getIdLong());
                stmt.setLong(2, role.getIdLong());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    rs.close();
                    stmt = rmModStmt[1];
                    sql = "(remote) DELETE FROM roles WHERE guildid=" + guild.getId() + " AND roleid=" + role.getIdLong();
                    stmt.setLong(1, guild.getIdLong());
                    stmt.setLong(2, role.getIdLong());
                    stmt.executeUpdate();
                    stmt.getConnection().commit();
                    ret = output.getString("modrole-remove");
                    Logger.logger.logRemoteRep("removed role " + role.getName(), guild, messageId, remote);
                } else {
                    rs.close();
                    ret = output.getString("error-modrole-missing");
                    Logger.logger.logRemoteRep("role not modrole", guild, messageId, remote);
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret;
    }

    public String addRemoteModRole(Role role, Guild guild, ResourceBundle output, long messageId,Guild remote) {
        String sql = "";
        String ret;
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = adModStmt[0];
            sql = "(remote) SELECT * FROM roles WHERE guildid=" + guild.getId() + " AND roleid=" + role.getIdLong();
            synchronized (adModStmt) {
                stmt.setLong(1, guild.getIdLong());
                stmt.setLong(2, role.getIdLong());
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    rs.close();
                    stmt = adModStmt[1];
                    sql = "(remote) INSERT INTO roles (guildid,roleid,rolename) VALUES (" + guild.getId() + "," + role.getIdLong() + ",'" + role.getName().replaceAll("[\',\"]", "") + "')";
                    stmt.setLong(1, guild.getIdLong());
                    stmt.setLong(2, role.getIdLong());
                    stmt.setString(3, role.getName().replaceAll("[\',\"]", ""));
                    stmt.execute();
                    stmt.getConnection().commit();
                    ret = output.getString("modrole-add");
                    Logger.logger.logRemoteRep("added role " + role.getName(), guild, messageId, remote);
                } else {
                    rs.close();
                    ret = output.getString("error-modrole-exists");
                    Logger.logger.logRemoteRep("role is modrole", guild, messageId, remote);
                }
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret;
    }

    public String clearRemoteModrole(Guild guild, ResourceBundle output, long messageId,Guild remote) {
        String sql = "";
        String ret;
        PreparedStatement stmt;
        try {
            stmt = clModStmt;
            sql = "(remote) DELETE FROM roles WHERE guildid=" + guild.getId();
            synchronized (clModStmt) {
                stmt.setLong(1, guild.getIdLong());
                stmt.executeUpdate();
                stmt.getConnection().commit();
            }
            ret = output.getString("modrole-clear");
            Logger.logger.logRemoteRep("cleared modroles", guild, messageId,remote);
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret;
    }

    public String listRemoteModrole(Guild guild, ResourceBundle output, long messageId,Guild remote) {
        String sql = "";
        StringBuilder ret = new StringBuilder(output.getString("modrole-list"));
        PreparedStatement stmt;
        try {
            stmt = lsModStmt;
            sql = "(remote) SELECT roleid FROM roles WHERE guildid=" + guild.getIdLong();
            syncModroleList(guild, ret, stmt);
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        Logger.logger.logRemoteRep("listed modroles", guild, messageId,remote);
        return ret.toString();
    }



    public String toggleRemoteEmoji(Guild guild, ResourceBundle output, long messageId,Guild remote) {
        String sql = "";
        StringBuilder ret = new StringBuilder(output.getString("toggle-head")).append(" ");
        try {
            PreparedStatement stmt;
            ResultSet rs;
            stmt = tgStmt[0];
            sql = "(remote) SELECT enabled FROM guilds WHERE guildid=" + guild.getIdLong();
            synchronized (tgStmt) {
                stmt.setLong(1, guild.getIdLong());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    boolean enabled = rs.getBoolean(1);
                    rs.close();
                    stmt = tgStmt[1];
                    sql = "(remote) UPDATE guilds SET enabled=" + !enabled + " WHERE guildid=" + guild.getIdLong();
                    stmt.setBoolean(1, !enabled);
                    stmt.setLong(2, guild.getIdLong());
                    stmt.executeUpdate();
                    stmt.getConnection().commit();
                    ret.append(output.getString(enabled ? "disabled" : "enabled"));
                    Logger.logger.logRemoteRep("Emoji" + (!enabled ? "ENABLED" : "DISABLED)"), guild, messageId, remote);
                } else
                    rs.close();
            }
        } catch (SQLException ex) {
            return sqlError(sql, ex);
        }
        return ret.toString();
    }


    private void syncModroleList(Guild guild, StringBuilder ret, PreparedStatement stmt) throws SQLException {
        ResultSet rs;
        synchronized (lsModStmt){
            stmt.setLong(1,guild.getIdLong());
            rs = stmt.executeQuery();
            while (rs.next()) {
                Role role = guild.getRoleById(rs.getLong(1));
                if (role != null) {
                    ret.append("\n");
                    ret.append(role.getName());
                }
            }
            rs.close();
        }
    }

    private String sqlError(String sql, SQLException ex) {
        Logger.logger.logError("SQLError in : "+ sql);
        Logger.logger.logError(ex.getMessage());
        Logger.logger.logError("SQLState: " + ex.getSQLState());
        Logger.logger.logError("VendorError: " + ex.getErrorCode());
        return null;
    }


    BotGuild(Connection conn) {
        try {
            stmts.add(this.rmModStmt[0] = conn.prepareStatement("SELECT * FROM roles WHERE guildid=? AND roleid=?"));
            stmts.add(this.rmModStmt[1] = conn.prepareStatement("DELETE FROM roles WHERE guildid=? AND roleid=?"));
            stmts.add(this.adModStmt[0] = rmModStmt[0]);
            stmts.add(this.adModStmt[1] = conn.prepareStatement("INSERT INTO roles (guildid,roleid,rolename) VALUES (?,?,?)"));
            stmts.add(this.clModStmt    = conn.prepareStatement("DELETE FROM roles WHERE guildid=?"));
            stmts.add(this.lsModStmt    = conn.prepareStatement("SELECT roleid FROM roles WHERE guildid=?"));
            stmts.add(this.tgStmt[0]    = conn.prepareStatement("SELECT enabled FROM guilds WHERE guildid=?"));
            stmts.add(this.tgStmt[1]    = conn.prepareStatement("UPDATE guilds SET enabled=? WHERE guildid=?"));
            stmts.add(this.rgStmt[0]    = conn.prepareStatement("SELECT * FROM registered_emoji_server WHERE guildid=?"));
            stmts.add(this.rgStmt[1]    = conn.prepareStatement("SELECT * FROM registered_emoji_server WHERE title=?"));
            stmts.add(this.rgStmt[2]    = conn.prepareStatement("INSERT INTO registered_emoji_server(guildid, title) VALUES (?,?)"));
            stmts.add(this.unRgStmt[0]  = conn.prepareStatement("SELECT * FROM registered_emoji_server WHERE guildid=?"));
            stmts.add(this.unRgStmt[1]  = conn.prepareStatement("DELETE FROM disabled_emoji_servers WHERE emoji_guildID=?"));
            stmts.add(this.unRgStmt[2]  = conn.prepareStatement("DELETE FROM registered_emoji_server WHERE guildid=?"));
            stmts.add(this.emStmt       = conn.prepareStatement("SELECT R.guildid FROM registered_emoji_server R WHERE title=? AND R.guildid NOT IN (" +
                    "SELECT emoji_guildid FROM disabled_emoji_servers D WHERE D.guildid=?)"));
            stmts.add(this.emLsStmt     = conn.prepareStatement("SELECT guildid FROM registered_emoji_server WHERE title=?"));
            stmts.add(this.pSerStmt     = conn.prepareStatement("SELECT R.guildid ,R.title," +
                    "(CASE WHEN guildid IN (" +
                    "SELECT emoji_guildid " +
                    "FROM disabled_emoji_servers D " +
                    "WHERE D.guildid=? )" +
                    "THEN TRUE " +
                    "ELSE FALSE " +
                    "END) as disabled " +
                    "FROM registered_emoji_server R " +
                    "ORDER BY disabled"));
            stmts.add(this.disStmt[0]   = rgStmt[1]);
            stmts.add(this.disStmt[1]   = conn.prepareStatement("SELECT * FROM disabled_emoji_servers WHERE guildid=? AND emoji_guildid=?"));
            stmts.add(this.disStmt[2]   = conn.prepareStatement("INSERT INTO disabled_emoji_servers(guildid, emoji_guildid) VALUES (?,?)"));
            stmts.add(this.enStmt[0]    = disStmt[0]);
            stmts.add(this.enStmt[1]    = conn.prepareStatement("DELETE FROM disabled_emoji_servers WHERE guildid=? AND emoji_guildid=?"));
            stmts.add(this.pStaStmt     = conn.prepareStatement("SELECT G.enabled,R.title,RO.count,DS.count " +
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
                    "WHERE G.guildid=?"));
            stmts.add(this.isModStmt    = conn.prepareStatement("SELECT roleid FROM roles WHERE guildid=?"));
            stmts.add(this.eEnStmt      = conn.prepareStatement("SELECT enabled FROM guilds WHERE guildid=?"));
            stmts.add(this.aModStmt[0]  = conn.prepareStatement("SELECT * FROM roles WHERE guildid=? AND roleid=?"));
            stmts.add(this.aModStmt[1]  = conn.prepareStatement("INSERT INTO roles (guildid,roleid,rolename) VALUES (?,?,?)"));
            stmts.add(this.delRolStmt[0]= aModStmt[0]);
            stmts.add(this.delRolStmt[1]= conn.prepareStatement("DELETE FROM roles WHERE guildid=? AND roleid=?"));

        }catch (SQLException ex) {
            Logger.logger.logError("SQLError in SQL preparation");
            Logger.logger.logError(ex.getMessage());
            Logger.logger.logError("SQLState: " + ex.getSQLState());
            Logger.logger.logError("VendorError: " + ex.getErrorCode());
            System.exit(-1);
        }
    }

    public void close (){
        for (PreparedStatement stmt : stmts){
            try {
                stmt.close();
            } catch (SQLException ignored) {
            }
        }
    }


}
