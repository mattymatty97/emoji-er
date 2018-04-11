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
    private EmojiGuild emojiGuild;
    public static boolean deleted = false;

    @Override
    public void onReady(ReadyEvent event) {
        List<Guild> guilds = event.getJDA().getSelfUser().getMutualGuilds();
        Statement stmt1 , stmt2;
        ResultSet rs;
        try {
            stmt1 = conn.createStatement();
            stmt2 = conn.createStatement();
            rs = stmt1.executeQuery("SELECT guildid FROM guilds");
            while (rs.next()){
                boolean found = false;
                for (Guild guild: guilds){
                    if(guild.getIdLong()==rs.getLong(1)){
                        found=true;
                        break;
                    }
                }
                if(!found){
                    stmt2.execute("DELETE FROM disabled_emoji_servers WHERE emoji_guildiD=" + rs.getLong(1) + " OR guildid=" + rs.getLong(1));
                    stmt2.execute("DELETE FROM registered_emoji_server WHERE guildid=" + rs.getLong(1));
                    stmt2.execute("DELETE FROM roles WHERE guildid=" + rs.getLong(1));
                    stmt2.execute("DELETE FROM guilds WHERE guildid=" + rs.getLong(1));
                    stmt2.execute("COMMIT");
                }
            }
            stmt2.close();
            stmt1.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        updateServerCount(event.getJDA());
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

            updateDatabase(guild);
            //name of sender server
            String guildname = event.getGuild().getName();
            //get sender member
            Member member = event.getMember();
            //get channel to send
            MessageChannel channel = event.getChannel();
            //get message
            Message message = event.getMessage();
            //get text
            if (message.getContent().matches(System.getenv("DEFAULT_EMOJI_PREFIX")+"emoji\\.\\w+"+System.getenv("DEFAULT_EMOJI_PREFIX"))||message.getContent().matches(System.getenv("DEFAULT_EMOJI_PREFIX")+"emoji\\.\\w+"+System.getenv("DEFAULT_EMOJI_PREFIX")+".+")) {
                String args[] = message.getContent().split(" ");
                String command = args[0].split(System.getenv("DEFAULT_EMOJI_PREFIX"))[1].split("\\.")[1];
                switch (command){
//------USER---------------------HELP--------------------------------------

                    case "help":
                        System.out.println("help shown in guild: '" + guildname + "'");
                        PrintHelp(channel, member, guild);
                        break;

//------USER--------------------PING---------------------------------------

                    case "ping":
                        System.out.println("Ping executed in guild: '" + guildname + "'");
                        channel.sendMessage(output.getString("pong")).queue();
                        break;
//------USER-------------------LIST----------------------------------------
                    case "list":
                        if(args.length==2) {
                            channel.sendMessage(output.getString("emoji-list")+"\n"+ emojiGuild.getEmojiList(args[1].replace(" ",""),event.getJDA())).queue();
                            System.out.println("emoji list shown in guild: '" + guildname + "'");
                        }else{
                            channel.sendMessage(output.getString("error-emoji-list")).queue();
                            System.out.println("error emoji list in guild: '" + guildname + "'");
                        }
                        break;
//------USER-------------------SERVER--------------------------------------
                    case "servers":
                        String result = emojiGuild.printServers(event.getJDA());
                        channel.sendMessage(output.getString("emoji-server-list")+"\n"+result).queue();
                        System.out.println("emoji server list shown in guild: '" + guildname + "'");
                        break;
//------MOD--------------------REGISTER------------------------------------
                    case "register":
                        if (member.isOwner() || botGuild.memberIsMod(member,guild.getIdLong())) {
                            if(args.length==2){
                                if(args[1].length()<=10){
                                    channel.sendMessage(emojiGuild.registerGuild(guild.getIdLong(),args[1],output)).queue();
                                }else{
                                    System.out.println("emoji register failed in guild: '" + guildname + "'");
                                    channel.sendMessage(output.getString("error-long-title")).queue();
                                }
                            }else{
                                System.out.println("command syntax in guild: '" + guildname + "'");
                                channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                            }
                        }else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            System.out.println("no permission in guild: '" + guildname + "'");
                        }
                        break;
//------MOD------------------UNREGISTER------------------------------------
                    case "unregister":
                        if (member.isOwner() || botGuild.memberIsMod(member,guild.getIdLong())) {
                            if(args.length==1){
                                channel.sendMessage(emojiGuild.unRegisterGuild(guild.getIdLong(),output)).queue();
                            }else{
                                System.out.println("command syntax in guild: '" + guildname + "'");
                                channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                            }
                        }else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            System.out.println("no permission in guild: '" + guildname + "'");
                        }
                        break;
//------MOD------------------MODROLE---------------------------------------
                    case "modrole":
                        //if member is allowed
                        if (member.isOwner() || botGuild.memberIsMod(member,guild.getIdLong())) {
                            //if there are other arguments
                            if (args.length>1) {
                                //get mentioned roles
                                List<Role> mentions = message.getMentionedRoles();
                                //test on second arg
                                switch (args[1]) {
                                    case "add":
                                        //if there is a mentioned role
                                        if (mentions.size() == 1) {
                                            //call class method to add roles
                                            System.out.println("adding modrole '" + mentions.get(0).getName() + "' to guild '" + guildname + "'");
                                            channel.sendMessage(botGuild.addModRole(mentions.get(0),guild.getIdLong(),output)).queue();
                                        } else {
                                            System.out.println("modrole syntax in guild: '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                        }
                                        break;
                                    case "remove":
                                        //if there is a mentioned user
                                        if (mentions.size() == 1) {
                                            //call class method to remove roles
                                            System.out.println("removing modrole '" + mentions.get(0).getName() + "' from guild '" + guildname + "'");
                                            channel.sendMessage(botGuild.removeModRole(mentions.get(0),guild.getIdLong(),output)).queue();
                                        } else {
                                            System.out.println("modrole syntax in guild: '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                        }
                                        break;
                                    case "clear":
                                        channel.sendMessage(botGuild.clearModrole(guild.getIdLong(),output)).queue();
                                        break;
                                    case "auto":
                                        botGuild.autoModRole(event.getGuild());
                                        channel.sendMessage(output.getString("modrole-auto")).queue();
                                        System.out.println("auto adding moroles to guild: "+guildname);
                                    case "list":
                                        //list all modroles
                                        System.out.println("listing modroles in guild: '" + guildname + "'");
                                        channel.sendMessage(botGuild.listModrole(guild,output)).queue();
                                        break;
                                    default:
                                        System.out.println("command syntax in guild: '" + guildname + "'");
                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                }

                            }
                            break;
                        } else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            System.out.println("no permission in guild: '" + guildname + "'");
                        }
                        break;
                }
            } else {
                String args[] = message.getRawContent().split(System.getenv("DEFAULT_EMOJI_PREFIX"));
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
                                emoji = emojiGuild.getEmoji(arg, event.getJDA());
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
                    if (PermissionUtil.checkPermission(event.getGuild().getTextChannelById(channel.getId()), event.getGuild().getSelfMember(), Permission.MESSAGE_MANAGE)) {
                        message.delete().queue();
                    }
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(member.getColor());
                    eb.setFooter(member.getEffectiveName(), member.getUser().getAvatarUrl());
                    channel.sendMessage(eb.build()).queue();
                    channel.sendMessage(ret.toString()).queue();
                    return;
                }
            }
        } else

        {
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

            if(botGuild.onRoleDeleted(event.getRole())) {
                deleted = false;
                System.out.println("role deleted in guild: " + guildname);
                try {
                    TextChannel channel =  event.getGuild().getDefaultChannel();
                    channel.sendMessage(output.getString("event-role-deleted")).queue();
                    channel.sendMessage(output.getString("event-role-deleted-2")).queue();
                } catch (InsufficientPermissionException ex) {
                    event.getGuild().getOwner().getUser().openPrivateChannel().queue((channel) ->
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
        //search for existent informations class for server
        botGuild.autoModRole(event.getGuild());
        System.out.println("guild " + event.getGuild().getName() + " added");
        try {
            event.getGuild().getDefaultChannel().sendMessage(output.getString("event-join")).queue();
        } catch (InsufficientPermissionException ex) {
            event.getGuild().getOwner().getUser().openPrivateChannel().queue((channel) ->
            {
                channel.sendMessage(output.getString("event-join")).queue();
            });
        }
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("INSERT INTO guilds(guildid, guildname) VALUES ("+event.getGuild().getIdLong()+",'"+event.getGuild().getName()+"')");
            stmt.execute("COMMIT");
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        updateServerCount(event.getJDA());
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DELETE FROM disabled_emoji_servers WHERE emoji_guildiD=" + event.getGuild().getIdLong() + " OR guildid=" + event.getGuild().getIdLong());
            stmt.execute("DELETE FROM registered_emoji_server WHERE guildid=" + event.getGuild().getIdLong());
            stmt.execute("DELETE FROM roles WHERE guildid=" + event.getGuild().getIdLong());
            stmt.execute("DELETE FROM guilds WHERE guildid=" + event.getGuild().getIdLong());
            stmt.execute("COMMIT");
            stmt.close();
            System.out.println("guild " + event.getGuild().getName() + " has been removed");
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
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

            //if is allowed to use mod commands
            if (member.isOwner() || botGuild.memberIsMod(member,guild.getIdLong())) {
                helpMsg.addBlankField(false);
                helpMsg.addField("MOD commands:", "", false);

                helpMsg.addField("modrole", output.getString("help-def-modrole"), false);

                helpMsg.addField("register", output.getString("help-def-register"), false);

                helpMsg.addField("unregister", output.getString("help-def-unregister"), false);

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
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return false;
    }

    public MyListener(Connection conn, List<BotGuild> savedGuilds) {
        this.conn = conn;
        this.botGuild = new BotGuild(conn);
        this.emojiGuild = new EmojiGuild(conn);
    }

    private static void guildDeleteDB(Connection conn, Long guildId) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guilds WHERE guildid=" + guildId);
            List<Long> to_remove = new ArrayList<>();
            if (rs.next()) {
                rs.close();
                rs = stmt.executeQuery("SELECT groupid FROM groups WHERE guildid=" + guildId);
                while (rs.next()) {
                    to_remove.add(rs.getLong(1));
                }
                rs.close();
                for (Long id : to_remove) {
                    stmt.execute("DELETE FROM grouproles WHERE groupid=" + id);
                }
                stmt.execute("DELETE FROM groups WHERE guildid=" + guildId);
                stmt.execute("DELETE FROM roles WHERE guildid=" + guildId);
            } else {
                rs.close();
            }
            stmt.execute("DELETE FROM guilds WHERE guildid=" + guildId);
            stmt.execute("COMMIT");
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
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
            new OkHttpClient().newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDatabase(Guild guild){
        try {
            Statement stmt1 = conn.createStatement();
            ResultSet rs;
            rs = stmt1.executeQuery("SELECT * FROM guilds WHERE guildid="+guild.getIdLong());
            if(rs.next()){
                rs.close();
                stmt1.execute("UPDATE guilds SET guildname='"+guild.getName()+"' WHERE guildid="+guild.getIdLong());
                stmt1.execute("COMMIT");
            }else {
                rs.close();
                stmt1.execute("INSERT INTO guilds(guildid, guildname) VALUES (" + guild.getIdLong() + ",'" + guild.getName() + "')");
                stmt1.execute("COMMIT");
            }
            stmt1.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }


}
