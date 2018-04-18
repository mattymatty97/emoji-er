package com.emoji_er;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.PermissionUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.List;

public class MyListener extends ListenerAdapter {
    private Connection conn;
    private BotGuild botGuild;
    public static boolean deleted = false;

    @Override
    public void onReady(ReadyEvent event) {
        String sql = "";
        List<Guild> guilds = event.getJDA().getSelfUser().getMutualGuilds();
        Statement stmt1, stmt2;
        ResultSet rs;
        try {
            stmt1 = conn.createStatement();
            stmt2 = conn.createStatement();
            sql = "SELECT guildid FROM guilds";
            rs = stmt1.executeQuery(sql);
            while (rs.next()) {
                boolean found = false;
                for (Guild guild : guilds) {
                    if (guild.getIdLong() == rs.getLong(1)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    sql = "DELETE FROM disabled_emoji_servers WHERE emoji_guildiD=" + rs.getLong(1) + " OR guildid=" + rs.getLong(1);
                    stmt2.execute(sql);
                    sql = "DELETE FROM registered_emoji_server WHERE guildid=" + rs.getLong(1);
                    stmt2.execute(sql);
                    sql = "DELETE FROM roles WHERE guildid=" + rs.getLong(1);
                    stmt2.execute(sql);
                    sql = "DELETE FROM guilds WHERE guildid=" + rs.getLong(1);
                    stmt2.execute(sql);
                    stmt2.execute("COMMIT");
                }
            }
            stmt2.close();
            stmt1.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in : " + sql);
            Logger.logGeneral("SQLException: " + ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            Logger.logGeneral(ex.getStackTrace()[0].toString());
        }
        updateServerCount(event.getJDA());
        Logger.logGeneral("------------SYSTEM READY---------------\r\n");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //locales generation (dynamic strings from file selectionable by language)
        ResourceBundle output = ResourceBundle.getBundle("messages");
        if (checkConnection()) {
            //if is a direct message exit immediately
            if (!event.isFromType(ChannelType.TEXT)) return;
            //if is a bot exit immediately
            if (event.getAuthor().isBot()) return;
            //if i cant write
            if (!PermissionUtil.checkPermission(event.getTextChannel(), event.getGuild().getSelfMember(), Permission.MESSAGE_WRITE))
                return;

            Guild guild = event.getGuild();

            updateDatabase(guild, output);
            //name of sender server
            String guildname = event.getGuild().getName();
            //get sender member
            Member member = event.getMember();
            //get channel to send
            MessageChannel channel = event.getChannel();
            //get message
            Message message = event.getMessage();
            //get id
            long messageId = message.getIdLong();
            if (message.getContent().matches(System.getenv("DEFAULT_EMOJI_PREFIX") + "emoji\\.\\w+" + System.getenv("DEFAULT_EMOJI_PREFIX")) || message.getContent().matches(System.getenv("DEFAULT_EMOJI_PREFIX") + "emoji\\.\\w+" + System.getenv("DEFAULT_EMOJI_PREFIX") + " .+")) {
                String args[] = message.getContent().split(" ");
                String command = args[0].split(System.getenv("DEFAULT_EMOJI_PREFIX"))[1].split("\\.")[1];
                switch (command) {
//------USER---------------------HELP--------------------------------------

                    case "help":
                        Logger.logMessage("help", message);
                        PrintHelp(channel, member, guild);
                        Logger.logReponse("help shown", guild, messageId);
                        break;

//------USER--------------------PING---------------------------------------

                    case "ping":
                        Logger.logMessage("Ping", message);
                        channel.sendMessage(output.getString("pong")).queue();
                        Logger.logReponse("Ping shown", guild, messageId);
                        break;
//------USER-------------------LIST----------------------------------------
                    case "list":
                        Logger.logMessage("list", message);
                        if (args.length >= 2) {
                            SendMsg(channel,output.getString("emoji-list") + "\n" + botGuild.getEmojiList(args[1].replace(" ", ""), event.getJDA()));
                            Logger.logReponse("emoji list shown", guild, messageId);
                        } else {
                            channel.sendMessage(output.getString("error-emoji-list")).queue();
                            Logger.logReponse("error emoji list", guild, messageId);
                        }
                        break;
//------USER-------------------SERVER--------------------------------------
                    case "servers":
                        Logger.logMessage("servers", message);
                        String result = botGuild.printServers(guild.getIdLong(), event.getJDA());
                        SendMsg(channel,output.getString("emoji-server-list") + "\n" + result);
                        Logger.logReponse("server list shown", guild, messageId);
                        break;
//------MOD--------------------REGISTER------------------------------------
                    case "register":
                        Logger.logMessage("register", message);
                        if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                            if (args.length >= 2) {
                                if (args[1].length() <= 10) {
                                    channel.sendMessage(botGuild.registerGuild(guild, args[1], output, messageId)).queue();
                                } else {
                                    Logger.logReponse("long server title", guild, messageId);
                                    channel.sendMessage(output.getString("error-long-title")).queue();
                                }
                            } else {
                                Logger.logReponse("syntax", guild, messageId);
                            }
                        } else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            Logger.logReponse("user not allowed", guild, messageId);
                        }
                        break;
//------MOD------------------UNREGISTER------------------------------------
                    case "unregister":
                        Logger.logMessage("unregister", message);
                        if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                            channel.sendMessage(botGuild.unRegisterGuild(guild, output, messageId)).queue();
                        } else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            Logger.logReponse("user not allowed", guild, messageId);
                        }
                        break;
//------MOD------------------MODROLE---------------------------------------
                    case "modrole":
                        //if member is allowed
                        if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                            //if there are other arguments
                            if (args.length > 1) {
                                //get mentioned roles
                                List<Role> mentions = message.getMentionedRoles();
                                //test on second arg
                                switch (args[1]) {
                                    case "add":
                                        //if there is a mentioned role
                                        Logger.logMessage("modrole add", message);
                                        if (mentions.size() >= 1) {
                                            //call class method to add roles
                                            channel.sendMessage(botGuild.addModRole(mentions.get(0), guild, output, messageId)).queue();
                                        }
                                        break;
                                    case "remove":
                                        //if there is a mentioned user
                                        Logger.logMessage("modrole remove", message);
                                        if (mentions.size() >= 1) {
                                            //call class method to remove roles
                                            channel.sendMessage(botGuild.removeModRole(mentions.get(0), guild, output, messageId)).queue();
                                        }
                                        break;
                                    case "clear":
                                        Logger.logMessage("modrole clear", message);
                                        channel.sendMessage(botGuild.clearModrole(guild, output, messageId)).queue();
                                        Logger.logReponse("modroles cleared", guild, messageId);
                                        break;
                                    case "auto":
                                        Logger.logMessage("modrole auto", message);
                                        botGuild.autoModRole(event.getGuild());
                                        channel.sendMessage(output.getString("modrole-auto")).queue();
                                        Logger.logReponse("modroles updated", guild, messageId);
                                    case "list":
                                        //list all modroles
                                        Logger.logMessage("modrole list", message);
                                        SendMsg(channel,botGuild.listModrole(guild, output, messageId));
                                        break;
                                }

                            }
                            break;
                        } else {
                            Logger.logMessage("modrole", message);
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            Logger.logReponse("user not allowed", guild, messageId);
                        }
                        break;
//------MOD------------------TOGGLE---------------------------------------
                    case "toggle":
                        Logger.logMessage("toggle", message);
                        if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                            channel.sendMessage(botGuild.toggleEmoji(guild, output, messageId)).queue();
                        } else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            Logger.logReponse("user not allowed", guild, messageId);
                        }
                        break;
//------MOD------------------STATUS---------------------------------------
                    case "status":
                        Logger.logMessage("status", message);
                        channel.sendMessage(botGuild.printStatus(guild, output, messageId)).queue();
                        break;
//------MOD------------------ENABLE---------------------------------------
                    case "enable":
                        Logger.logMessage("enable", message);
                        if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                            if (args.length >= 2) {
                                channel.sendMessage(botGuild.enableGuild(guild, args[1], output, messageId)).queue();
                            } else {
                                Logger.logReponse("syntax", guild, messageId);
                            }
                        } else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            Logger.logReponse("user not allowed", guild, messageId);
                        }
                        break;
//------MOD------------------DISABLE---------------------------------------
                    case "disable":
                        Logger.logMessage("disable", message);
                        if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                            if (args.length >= 2) {
                                channel.sendMessage(botGuild.disableGuild(guild, args[1], output, messageId)).queue();
                            } else {
                                Logger.logReponse("syntax", guild, messageId);
                            }
                        } else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            Logger.logReponse("user not allowed", guild, messageId);
                        }
                        break;
                }
                /*--------------------EMOJI REPLACEMENT------------------*/
            } else {
                if (botGuild.emojiEnabled(guild)) {
                    String args[] = message.getRawContent().split(System.getenv("DEFAULT_EMOJI_PREFIX"));
                    if (args.length >= 1) {
                        StringBuilder ret = new StringBuilder(args[0]);
                        boolean found = false;
                        boolean last = false;
                        boolean used = false;
                        if (args.length > 1) {
                            for (int i = 1; i < args.length; i++) {
                                String arg = args[i];
                                if (!last) {
                                    if (arg.matches("\\w+\\.\\w+")) {
                                        String[] param = arg.split("\\.");
                                        String emoji;
                                        emoji = botGuild.getEmoji(arg, guild.getIdLong(), event.getJDA());
                                        if (emoji != null) {
                                            ret.append(emoji);
                                            found = true;
                                            used = true;
                                        }
                                    }
                                }
                                if (!found) {
                                    if (!last)
                                        ret.append(System.getenv("DEFAULT_EMOJI_PREFIX"));
                                    ret.append(arg);
                                }
                                last = found;
                                found = false;
                            }
                        }
                        if (used) {
                            Logger.logMessage("an emoji", message);
                            if (PermissionUtil.checkPermission(event.getGuild().getTextChannelById(channel.getId()), event.getGuild().getSelfMember(), Permission.MESSAGE_MANAGE)) {
                                message.delete().queue();
                                Logger.logReponse("message deleted", guild, messageId);
                            }
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setColor(member.getColor());
                            eb.setFooter(member.getEffectiveName(), member.getUser().getAvatarUrl());
                            channel.sendMessage(eb.build()).queue();
                            channel.sendMessage(ret.toString()).queue();
                            Logger.logReponse("message reposted", guild, messageId);
                        }
                    }
                }
            }
        } else {
            event.getJDA().shutdown();
            Reconnector.reconnect();
        }

    }


    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        ResourceBundle output;
        if (checkConnection()) {
            String guildname = event.getGuild().getName();
            output = ResourceBundle.getBundle("messages");

            if (botGuild.onRoleDeleted(event.getRole())) {
                deleted = false;
                Logger.logEvent("role deleted in guild: ", event.getGuild());
                try {
                    TextChannel channel = event.getGuild().getDefaultChannel();
                    channel.sendMessage(output.getString("event-role-deleted")).queue();
                    channel.sendMessage(output.getString("event-role-deleted-2")).queue();
                } catch (InsufficientPermissionException ex) {
                    event.getGuild().getOwner().getUser().openPrivateChannel().queue((PrivateChannel channel) ->
                    {
                        channel.sendMessage(output.getString("event-role-deleted")).queue();
                        channel.sendMessage(output.getString("event-role-deleted-2")).queue();
                    });
                }

            }
        } else {
            event.getJDA().shutdown();
            Reconnector.reconnect();
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        ResourceBundle output = ResourceBundle.getBundle("messages");
        String sql = "";
        //search for existent informations class for server
        Logger.logEvent("GUILD HAS JOINED", event.getGuild());
        try {
            event.getGuild().getDefaultChannel().sendMessage(output.getString("event-join")).queue();
        } catch (InsufficientPermissionException ex) {
            event.getGuild().getOwner().getUser().openPrivateChannel().queue((PrivateChannel channel) ->
                    channel.sendMessage(output.getString("event-join")).queue());
        }
        try {
            Statement stmt = conn.createStatement();
            sql = "INSERT INTO guilds(guildid, guildname) VALUES (" + event.getGuild().getIdLong() + ",'" + event.getGuild().getName().replaceAll("[\',\"]","") + "')";
            stmt.execute(sql);
            stmt.execute("COMMIT");
            stmt.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in: " + sql);
            Logger.logGeneral("SQLException: " + ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            Logger.logGeneral(ex.getStackTrace()[0].toString());
        }
        botGuild.autoModRole(event.getGuild());
        updateServerCount(event.getJDA());
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        String sql = "";
        try {
            Statement stmt = conn.createStatement();
            sql = "DELETE FROM disabled_emoji_servers WHERE emoji_guildiD=" + event.getGuild().getIdLong() + " OR guildid=" + event.getGuild().getIdLong();
            stmt.execute(sql);
            sql = "DELETE FROM registered_emoji_server WHERE guildid=" + event.getGuild().getIdLong();
            stmt.execute(sql);
            sql = "DELETE FROM roles WHERE guildid=" + event.getGuild().getIdLong();
            stmt.execute(sql);
            sql = "DELETE FROM guilds WHERE guildid=" + event.getGuild().getIdLong();
            stmt.execute(sql);
            stmt.execute("COMMIT");
            stmt.close();
            Logger.logEvent("GUILD HAS LEAVED", event.getGuild());
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in : " + sql);
            Logger.logGeneral("SQLException: " + ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            Logger.logGeneral(ex.getStackTrace()[0].toString());
        }
        updateServerCount(event.getJDA());
    }

    //prints the help message
    private void PrintHelp(MessageChannel channel, Member member, Guild guild) {
        ResourceBundle output = ResourceBundle.getBundle("messages");
        EmbedBuilder helpMsg = new EmbedBuilder();
        helpMsg.setColor(Color.GREEN);
        //help is dynamic (different for every user)
        helpMsg.setTitle(output.getString("help-title"));
        helpMsg.setDescription(output.getString("help-description"));
        helpMsg.addField("help", output.getString("help-def-help"), false);
        helpMsg.addField("ping", output.getString("help-def-ping"), false);
        helpMsg.addField("list", output.getString("help-def-list"), false);
        helpMsg.addField("servers", output.getString("help-def-servers"), false);
        helpMsg.addField("status", output.getString("help-def-status"), false);

        //if is allowed to use mod commands
        if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
            helpMsg.addBlankField(false);
            helpMsg.addField("MOD commands:", "", false);

            helpMsg.addField("modrole", output.getString("help-def-modrole"), false);

            helpMsg.addField("register", output.getString("help-def-register"), false);

            helpMsg.addField("unregister", output.getString("help-def-unregister"), false);

            helpMsg.addField("toggle", output.getString("help-def-toggle"), false);

            helpMsg.addField("enable", output.getString("help-def-enable"), false);

            helpMsg.addField("disable", output.getString("help-def-disable"), false);

        }
        helpMsg.setFooter(output.getString("help-footer"), null);
        channel.sendMessage(helpMsg.build()).queue();
    }

    private boolean checkConnection() {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("SELECT 1");
            stmt.close();
            return true;
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in : SELECT 1");
            Logger.logGeneral("SQLException: " + ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            Logger.logGeneral(ex.getStackTrace()[0].toString());
        }
        return false;
    }

    public MyListener(Connection conn) {
        this.conn = conn;
        this.botGuild = new BotGuild(conn);
    }

    private void updateServerCount(JDA api) {
        String url = "https://discordbots.org/api/bots/" + api.getSelfUser().getId() + "/stats";
        String discordbots_key = System.getenv("DISCORDBOTS_KEY");

        JSONObject data = new JSONObject();
        data.put("server_count", api.getGuilds().size());

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), data.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "DiscordBot " + api.getSelfUser().getName())
                .addHeader("Authorization", discordbots_key)
                .build();

        try {
            new OkHttpClient().newCall(request).execute().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDatabase(Guild guild, ResourceBundle output) {
        String sql = "";
        try {
            Statement stmt1 = conn.createStatement();
            ResultSet rs;
            sql = "SELECT * FROM guilds WHERE guildid=" + guild.getIdLong();
            rs = stmt1.executeQuery(sql);
            if (rs.next()) {
                rs.close();
                sql = "UPDATE guilds SET guildname='" + guild.getName().replaceAll("[',\"]","") + "' WHERE guildid=" + guild.getIdLong();
                stmt1.execute(sql);
                stmt1.execute("COMMIT");
            } else {
                rs.close();
                sql = "INSERT INTO guilds(guildid, guildname) VALUES (" + guild.getIdLong() + ",'" + guild.getName().replaceAll("[',\"]","") + "')";
                stmt1.execute(sql);
                stmt1.execute("COMMIT");
                botGuild.autoModRole(guild);
                updateServerCount(guild.getJDA());
                try {
                    guild.getDefaultChannel().sendMessage(output.getString("event-join")).queue();
                } catch (InsufficientPermissionException ex) {
                    guild.getOwner().getUser().openPrivateChannel().queue((PrivateChannel channel) ->
                            channel.sendMessage(output.getString("event-join")).queue());
                }
            }
            stmt1.close();
        } catch (SQLException ex) {
            Logger.logGeneral("SQLError in : " + sql);
            Logger.logGeneral("SQLException: " + ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            Logger.logGeneral(ex.getStackTrace()[1].toString());
        }
    }

    private void SendMsg(MessageChannel channel,String text){
        //TODO: handle messages longer than 200 chars
        int messages= ((Double)Math.ceil(text.length()/200.0)).intValue();
        if(messages>1) {
            int s=0;
            for (int i = 0; i < messages; i++) {
                int p = s, a = s;
                while((a-s)<200 & a!= -1){
                    p=a;
                    a=text.indexOf("\n",p);
                }
                if(a==-1)
                    p=text.length();
                channel.sendMessage(text.substring(s,p)).queue();
                s=p;
            }
        }else{
            channel.sendMessage(text).queue();
        }
    }

}
