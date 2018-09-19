package com.emoji_er;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.utils.PermissionUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.logging.log4j.core.util.SystemMillisClock;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;


import static org.fusesource.jansi.Ansi.ansi;

public class MyListener implements EventListener {
    private Connection conn;
    private BotGuild botGuild;
    private static ExecutorService eventThreads = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>()){

        Map<Thread,Integer> threads = new HashMap<>();

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t,r);
            int index;
            synchronized (Global.eventQueue) {
                index = Optional.ofNullable(Global.eventQueue.poll()).orElse(-1);
            }
            if(index==-1)
                index=Global.maxEventCtn++;

            t.setName("Event Thread: " + index);
            threads.put(t,index);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r,t);
            synchronized (Global.eventQueue) {
                Thread thread = Thread.currentThread();
                int index = threads.get(thread);
                Global.eventQueue.add(index);
            }
        }
    };

    @Override
    public void onEvent(Event event)
    {
        if (event instanceof ReadyEvent)
            onReady((ReadyEvent) event);
        else if (event instanceof MessageReceivedEvent) {
            MessageReceivedEvent ev = (MessageReceivedEvent) event;
            if (!ev.isFromType(ChannelType.TEXT)) return;
            //if is a bot exit immediately
            if (ev.getAuthor().isBot()) return;
            //if i cant write
            if (!PermissionUtil.checkPermission(ev.getTextChannel(), ev.getGuild().getSelfMember(), Permission.MESSAGE_WRITE))
                return;

            MessageChannel channel = ev.getChannel();
            //get message
            Message message = ev.getMessage();
            if (message.getContentDisplay().matches(".*"+ System.getenv("DEFAULT_EMOJI_PREFIX") +"\\w+\\.\\w+"+ System.getenv("DEFAULT_EMOJI_PREFIX") + ".*"))
                eventThreads.execute(() -> onMessageReceived((MessageReceivedEvent) event));
        }
        else if (event instanceof RoleDeleteEvent)
            eventThreads.execute(() -> onRoleDelete((RoleDeleteEvent) event));
        else if (event instanceof GuildJoinEvent)
            eventThreads.execute(() -> onGuildJoin((GuildJoinEvent) event));
        else if (event instanceof GuildLeaveEvent)
            eventThreads.execute(() -> onGuildLeave((GuildLeaveEvent) event));
    }

    private void onReady(ReadyEvent event) {
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
                    stmt2.getConnection().commit();
                }
            }
            stmt2.close();
            stmt1.close();
        } catch (SQLException ex) {
            try {
                conn.rollback();
            }catch (SQLException ignored){}
            Logger.logger.logError("SQLError in : " + sql);
            Logger.logger.logError("SQLException: " + ex.getMessage());
            Logger.logger.logError("SQLState: " + ex.getSQLState());
            Logger.logger.logError("VendorError: " + ex.getErrorCode());

        }
        updateServerCount(event.getJDA());
        Logger.logger.logGeneral("------------SYSTEM READY---------------\r\n");
        Logger.started = true;
    }

    @SuppressWarnings("Duplicates")
    private void onMessageReceived(MessageReceivedEvent event) {
        //locales generation (dynamic strings from file selectionable by language)
        ResourceBundle output = ResourceBundle.getBundle("messages");
        ScheduledFuture typing;
        if (checkConnection()) {
            if (!PermissionUtil.checkPermission(event.getTextChannel(), event.getGuild().getSelfMember(), Permission.MESSAGE_EMBED_LINKS)) {
                event.getTextChannel().sendMessage("Error i'm unable to send embedded messages, pls change my permissions! ( EMBED_LIKNS )").queue();
                return;
            }
            if (!PermissionUtil.checkPermission(event.getTextChannel(), event.getGuild().getSelfMember(), Permission.MESSAGE_EXT_EMOJI)) {
                event.getTextChannel().sendMessage("Error i'm unable to send external emoticons, pls change my permissions! ( USE_EXTERNAL_EMOJI )").queue();
                return;
            }
            Guild guild = event.getGuild();

            updateDatabase(guild, output);
            //name of sender server
            String guildname = event.getGuild().getName();
            //get sender member
            Member member = event.getMember();
            //get channel to send
            TextChannel channel = event.getTextChannel();
            //get message
            Message message = event.getMessage();
            //get id
            long messageId = message.getIdLong();

                if (message.getContentDisplay().matches(System.getenv("DEFAULT_EMOJI_PREFIX") + "emoji\\.\\w+" + System.getenv("DEFAULT_EMOJI_PREFIX")) || message.getContentDisplay().matches(System.getenv("DEFAULT_EMOJI_PREFIX") + "emoji\\.\\w+" + System.getenv("DEFAULT_EMOJI_PREFIX") + " .+")) {

                    String args[] = message.getContentRaw().split(" +");
                    String command = args[0].split(System.getenv("DEFAULT_EMOJI_PREFIX"))[1].split("\\.")[1];
                    switch (command) {
//------USER---------------------HELP--------------------------------------

                        case "help":
                            Logger.logger.logMessage("help", message);
                            PrintHelp(channel, member, guild);
                            Logger.logger.logReponse("help shown", guild, messageId);
                            break;

//------USER--------------------PING---------------------------------------

                        case "ping":
                            Logger.logger.logMessage("Ping", message);
                            channel.sendMessage(output.getString("pong")).queue();
                            Logger.logger.logReponse("Ping shown", guild, messageId);
                            MessageChannel listen = Global.getGbl().getListener();
                            if (listen != null) {
                                listen.sendMessage(new EmbedBuilder()
                                        .setAuthor(guildname, null, guild.getIconUrl())
                                        .addField("ID", guild.getId(), false).build()).queue();
                                Global.getGbl().setListener(null);
                            }
                            break;
//------USER--------------------PING---------------------------------------

                        case "invite":
                            Logger.logger.logMessage("invite", message);
                            channel.sendMessage(output.getString("invite")).queue();
                            Logger.logger.logReponse("invite link sent", guild, messageId);
                            break;

//------USER-------------------LIST----------------------------------------
                        case "list":
                            typing = channel.sendTyping().queueAfter(1,TimeUnit.SECONDS);
                            Logger.logger.logMessage("list", message);
                            if (args.length >= 2) {
                                try {
                                    String ret = botGuild.getEmojiList(args[1].replace(" ", ""), event.getJDA());
                                    SendMsg(channel, output.getString("emoji-list") + "\n" + ret);
                                    Logger.logger.logReponse("emoji list shown", guild, messageId);
                                }catch(EmojiError err){
                                    channel.sendMessage(output.getString(err.getMessage())).queue();
                                    Logger.logger.logReponse(err.getMessage(), guild, messageId);
                                }
                            } else {
                                channel.sendMessage(output.getString("error-emoji-list")).queue();
                                Logger.logger.logReponse("error emoji list", guild, messageId);
                            }
                            typing.cancel(true);
                            break;
//------USER-------------------SERVER--------------------------------------
                        case "servers":
                            typing = channel.sendTyping().queueAfter(1,TimeUnit.SECONDS);
                            Logger.logger.logMessage("servers", message);
                            String result = botGuild.printServers(guild.getIdLong(), event.getJDA());
                            SendMsg(channel, output.getString("emoji-server-list") + "\n" + result,"diff");
                            Logger.logger.logReponse("server list shown", guild, messageId);
                            typing.cancel(true);
                            break;
//------USER-------------------REACT---------------------------------------
                        case "react": {
                            if(args.length>1 && message.getContentDisplay().split(" +")[1].matches(":?(\\w+\\.)?\\w+:?")){
                                String arg = message.getContentDisplay().split(" +")[1];
                                Logger.logger.logMessage("react", message);

                                TextChannel reactChannel;
                                if (args.length>2 && message.getMentionedChannels().size()==1){
                                    reactChannel = message.getMentionedChannels().get(0);
                                }else{
                                    reactChannel = channel;
                                }

                                Emote emoji_d;
                                if(arg.matches(":?\\w+\\.\\w+:?"))
                                    emoji_d = botGuild.getEmoji(arg.replace(":",""), guild.getIdLong(), event.getJDA());
                                else
                                    try {
                                        emoji_d = guild.getEmotesByName(arg.replace(":",""),true).get(0);
                                    }catch (IndexOutOfBoundsException ex){
                                        emoji_d = null;
                                    }

                                if(emoji_d==null){
                                    channel.sendMessage(output.getString("error-emoji-404")).queue();
                                    Logger.logger.logReponse("emoji not found", guild, messageId);
                                    break;
                                }

                                final Emote emoji = emoji_d;

                                try {
                                    message.delete().complete();
                                }catch (Exception ignored){}

                                MessageHistory history = new MessageHistory(reactChannel);
                                history.retrievePast(10).complete();
                                List<Message> messages = history.getRetrievedHistory();

                                Message m = channel.sendMessage(output.getString("emoji-react-success").replace("{time}","6").replace("{user}",member.getAsMention())).complete();

                                messages.forEach(m2 -> m2.addReaction(emoji).complete());
                                for (int ctn=5;ctn>0;ctn--){
                                        m.editMessage(output.getString("emoji-react-success").replace("{time}",String.valueOf(ctn)).replace("{user}",member.getAsMention())).queueAfter(5-ctn,TimeUnit.SECONDS);
                                }

                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException ex) {
                                    return;
                                }

                                m.delete().queue();

                                List<MessageReaction> reactions = messages.stream().map(m2 -> m2.getChannel().getMessageById(m2.getId()).complete()).flatMap((Message m2)-> m2.getReactions().stream()).collect(Collectors.toList());
                                reactions.forEach(r ->r.removeReaction().queue());

                                Logger.logger.logReponse("success", guild, messageId);
                            }
                        }
                        break;
//------MOD--------------------REGISTER------------------------------------
                        case "register":
                            typing = channel.sendTyping().queueAfter(1,TimeUnit.SECONDS);
                            Logger.logger.logMessage("register", message);
                            if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                                if (args.length >= 2) {
                                    if (args[1].length() <= 10) {
                                        channel.sendMessage(botGuild.registerGuild(guild, args[1], output, messageId)).queue();
                                    } else {
                                        Logger.logger.logReponse("long server title", guild, messageId);
                                        channel.sendMessage(output.getString("error-long-title")).queue();
                                    }
                                } else {
                                    Logger.logger.logReponse("syntax", guild, messageId);
                                }
                            } else {
                                channel.sendMessage(output.getString("error-user-permission")).queue();
                                Logger.logger.logReponse("user not allowed", guild, messageId);
                            }
                            typing.cancel(true);
                            break;
//------MOD------------------UNREGISTER------------------------------------
                        case "unregister":
                            typing = channel.sendTyping().queueAfter(1,TimeUnit.SECONDS);
                            Logger.logger.logMessage("unregister", message);
                            if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                                channel.sendMessage(botGuild.unRegisterGuild(guild, output, messageId)).queue();
                            } else {
                                channel.sendMessage(output.getString("error-user-permission")).queue();
                                Logger.logger.logReponse("user not allowed", guild, messageId);
                            }
                            typing.cancel(true);
                            break;
//------MOD------------------MODROLE---------------------------------------
                        case "modrole":
                            typing = channel.sendTyping().queueAfter(1,TimeUnit.SECONDS);
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
                                            Logger.logger.logMessage("modrole add", message);
                                            if (mentions.size() >= 1) {
                                                //call class method to add roles
                                                channel.sendMessage(botGuild.addModRole(mentions.get(0), guild, output, messageId)).queue();
                                            }
                                            break;
                                        case "remove":
                                            //if there is a mentioned user
                                            Logger.logger.logMessage("modrole remove", message);
                                            if (mentions.size() >= 1) {
                                                //call class method to remove roles
                                                channel.sendMessage(botGuild.removeModRole(mentions.get(0), guild, output, messageId)).queue();
                                            }
                                            break;
                                        case "clear":
                                            Logger.logger.logMessage("modrole clear", message);
                                            channel.sendMessage(botGuild.clearModrole(guild, output, messageId)).queue();
                                            Logger.logger.logReponse("modroles cleared", guild, messageId);
                                            break;
                                        case "auto":
                                            Logger.logger.logMessage("modrole auto", message);
                                            botGuild.autoModRole(event.getGuild());
                                            channel.sendMessage(output.getString("modrole-auto")).queue();
                                            Logger.logger.logReponse("modroles updated", guild, messageId);
                                        case "list":
                                            //list all modroles
                                            Logger.logger.logMessage("modrole list", message);
                                            SendMsg(channel, botGuild.listModrole(guild, output, messageId));
                                            break;
                                    }

                                }
                                break;
                            } else {
                                Logger.logger.logMessage("modrole", message);
                                channel.sendMessage(output.getString("error-user-permission")).queue();
                                Logger.logger.logReponse("user not allowed", guild, messageId);
                            }
                            typing.cancel(true);
                            break;
//------MOD------------------TOGGLE---------------------------------------
                        case "toggle":
                            typing = channel.sendTyping().queueAfter(1,TimeUnit.SECONDS);
                            Logger.logger.logMessage("toggle", message);
                            if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                                channel.sendMessage(botGuild.toggleEmoji(guild, output, messageId)).queue();
                            } else {
                                channel.sendMessage(output.getString("error-user-permission")).queue();
                                Logger.logger.logReponse("user not allowed", guild, messageId);
                            }
                            typing.cancel(true);
                            break;
//------MOD------------------STATUS---------------------------------------
                        case "status":
                            typing = channel.sendTyping().queueAfter(1,TimeUnit.SECONDS);
                            Logger.logger.logMessage("status", message);
                            channel.sendMessage(botGuild.printStatus(guild, output, messageId)).queue();
                            Logger.logger.logReponse("status shown",guild,messageId);
                            typing.cancel(true);
                            break;
//------MOD------------------ENABLE---------------------------------------
                        case "enable":
                            typing = channel.sendTyping().queueAfter(1,TimeUnit.SECONDS);
                            Logger.logger.logMessage("enable", message);
                            if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                                if (args.length >= 2) {
                                    channel.sendMessage(botGuild.enableGuild(guild, args[1], output, messageId)).queue();
                                } else {
                                    Logger.logger.logReponse("syntax", guild, messageId);
                                }
                            } else {
                                channel.sendMessage(output.getString("error-user-permission")).queue();
                                Logger.logger.logReponse("user not allowed", guild, messageId);
                            }
                            typing.cancel(true);
                            break;
//------MOD------------------DISABLE---------------------------------------
                        case "disable":
                            typing = channel.sendTyping().queueAfter(1,TimeUnit.SECONDS);
                            Logger.logger.logMessage("disable", message);
                            if (member.isOwner() || botGuild.memberIsMod(member, guild.getIdLong())) {
                                if (args.length >= 2) {
                                    channel.sendMessage(botGuild.disableGuild(guild, args[1], output, messageId)).queue();
                                } else {
                                    Logger.logger.logReponse("syntax", guild, messageId);
                                }
                            } else {
                                channel.sendMessage(output.getString("error-user-permission")).queue();
                                Logger.logger.logReponse("user not allowed", guild, messageId);
                            }
                            typing.cancel(true);
                            break;
                    }
                    /*--------------------EMOJI REPLACEMENT------------------*/
                } else {
                    if (botGuild.emojiEnabled(guild)) {
                        String args[] = message.getContentDisplay().split(System.getenv("DEFAULT_EMOJI_PREFIX"));
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
                                            Emote emoji;
                                            emoji = botGuild.getEmoji(arg, guild.getIdLong(), event.getJDA());
                                            if (emoji != null) {
                                                ret.append(emoji.getAsMention());
                                                if(!used)
                                                    channel.sendTyping().complete();
                                                found = true;
                                                used = true;
                                            }
                                        }else if(arg.matches("\\w+")){
                                            Emote emoji;
                                            try {
                                                emoji = guild.getEmotesByName(arg,true).get(0);
                                            }catch (IndexOutOfBoundsException ex) {
                                                emoji = null;
                                            }
                                            if (emoji != null && !isNitro(member.getUser())) {
                                                ret.append(emoji.getAsMention());
                                                if(!used)
                                                    channel.sendTyping().complete();
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
                                Logger.logger.logMessage("an emoji", message);
                                if (PermissionUtil.checkPermission(event.getGuild().getTextChannelById(channel.getId()), event.getGuild().getSelfMember(), Permission.MESSAGE_MANAGE)) {
                                    message.delete().queue();
                                    Logger.logger.logReponse("message deleted", guild, messageId);
                                }
                                EmbedBuilder eb = new EmbedBuilder();
                                eb.setColor(member.getColor());
                                eb.setAuthor(member.getEffectiveName(), null, member.getUser().getAvatarUrl());
                                channel.sendMessage(eb.build()).queue();
                                channel.sendMessage(ret.toString()).queue();
                                Logger.logger.logReponse("message reposted", guild, messageId);
                            }
                        }
                    }
                }
        } else {
            event.getJDA().shutdown();
            Reconnector.reconnect();
        }

    }

    private boolean isNitro(User user)
    {
        return user instanceof SelfUser && ((SelfUser) user).isNitro();
    }

    private void onRoleDelete(RoleDeleteEvent event) {
        ResourceBundle output;
        if (checkConnection()) {
            output = ResourceBundle.getBundle("messages");

            if (botGuild.onRoleDeleted(event.getRole())) {
                Logger.logger.logEvent("role deleted in guild: ", event.getGuild());
                try {
                    TextChannel channel = Optional.ofNullable(event.getGuild().getDefaultChannel()).orElse(event.getGuild().getSystemChannel());
                    channel.sendMessage(output.getString("event-role-deleted")).queue();
                    channel.sendMessage(output.getString("event-role-deleted-2")).queue();
                } catch (InsufficientPermissionException ex) {
                    try {
                        event.getGuild().getOwner().getUser().openPrivateChannel().queue((PrivateChannel channel) ->
                        {
                            channel.sendMessage(output.getString("event-role-deleted")).queue();
                            channel.sendMessage(output.getString("event-role-deleted-2")).queue();
                        });
                    }catch (Exception ignored){}
                }

            }
        } else {
            event.getJDA().shutdown();
            Reconnector.reconnect();
        }
    }


    private void onGuildJoin(GuildJoinEvent event) {
        ResourceBundle output = ResourceBundle.getBundle("messages");
        String sql = "";
        //search for existent informations class for server
        Logger.logger.logEvent("GUILD HAS JOINED", event.getGuild());
        try {
            Optional.ofNullable(event.getGuild().getDefaultChannel()).orElse(event.getGuild().getSystemChannel())
                    .sendMessage(output.getString("event-join")).queue();
        } catch (InsufficientPermissionException ex) {
            try {
                event.getGuild().getOwner().getUser().openPrivateChannel().queue((PrivateChannel channel) ->
                        channel.sendMessage(output.getString("event-join")).queue());
            }catch (Exception ignored){}
        }
        try {
            Statement stmt = conn.createStatement();
            sql = "INSERT INTO guilds(guildid, guildname) VALUES (" + event.getGuild().getIdLong() + ",'" + event.getGuild().getName().replaceAll("[\',\"]", "") + "')";
            stmt.execute(sql);
            stmt.getConnection().commit();
            stmt.close();
        } catch (SQLException ex) {
            Logger.logger.logError("SQLError in: " + sql);
            Logger.logger.logError("SQLException: " + ex.getMessage());
            Logger.logger.logError("SQLState: " + ex.getSQLState());
            Logger.logger.logError("VendorError: " + ex.getErrorCode());

        }
        botGuild.autoModRole(event.getGuild());
        updateServerCount(event.getJDA());
    }


    private void onGuildLeave(GuildLeaveEvent event) {
        ResourceBundle output = ResourceBundle.getBundle("messages");
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
            stmt.getConnection().commit();
            stmt.close();
            Logger.logger.logEvent("GUILD HAS LEAVED", event.getGuild());
        } catch (SQLException ex) {
            Logger.logger.logGeneral("SQLError in : " + sql);
            Logger.logger.logError("SQLException: " + ex.getMessage());
            Logger.logger.logError("SQLState: " + ex.getSQLState());
            Logger.logger.logError("VendorError: " + ex.getErrorCode());
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
        helpMsg.addField("invite", output.getString("help-def-invite"), false);
        helpMsg.addField("list", output.getString("help-def-list"), false);
        helpMsg.addField("servers", output.getString("help-def-servers"), false);
        helpMsg.addField("status", output.getString("help-def-status"), false);
        helpMsg.addField("react", output.getString("help-def-react"), false);

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

            if (guildIsSupport(guild)) {
                helpMsg.addBlankField(false);

                helpMsg.addField("SUPPORT commands:", "", false);

                helpMsg.addField("console", output.getString("help-def-console"), false);

                helpMsg.addField("listen", output.getString("help-def-listen"), false);
            }
        }

        helpMsg.addField("",output.getString("help-last"),false);

        if (member.getUser().getIdLong() == Long.parseLong(System.getenv("OWNER_ID")))
            helpMsg.setFooter(output.getString("help-footer-owner"), member.getUser().getAvatarUrl());
        else
            helpMsg.setFooter(output.getString("help-footer"), guild.getIconUrl());
        channel.sendMessage(helpMsg.build()).queue();
    }

    private boolean checkConnection() {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("SELECT 1");
            stmt.close();
            return true;
        } catch (SQLException ex) {
            Logger.logger.logError("SQLError in : SELECT 1");
            Logger.logger.logError("SQLException: " + ex.getMessage());
            Logger.logger.logError("SQLState: " + ex.getSQLState());
            Logger.logger.logError("VendorError: " + ex.getErrorCode());
        }
        return false;
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
                sql = "UPDATE guilds SET guildname='" + guild.getName().replaceAll("[',\"]", "") + "' WHERE guildid=" + guild.getIdLong();
                stmt1.execute(sql);
                stmt1.execute("COMMIT");
            } else {
                rs.close();
                sql = "INSERT INTO guilds(guildid, guildname) VALUES (" + guild.getIdLong() + ",'" + guild.getName().replaceAll("[',\"]", "") + "')";
                stmt1.execute(sql);
                stmt1.getConnection().commit();
                botGuild.autoModRole(guild);
                updateServerCount(guild.getJDA());
                try {
                    Optional.ofNullable(guild.getDefaultChannel()).orElse(guild.getSystemChannel())
                            .sendMessage(output.getString("event-join")).queue();
                } catch (InsufficientPermissionException ex) {
                    try {
                        guild.getOwner().getUser().openPrivateChannel().queue((PrivateChannel channel) ->
                                channel.sendMessage(output.getString("event-join")).queue());
                    }catch(Exception ignored){}
                }
            }
            stmt1.close();
        } catch (SQLException ex) {
            Logger.logger.logError("SQLError in : " + sql);
            Logger.logger.logError("SQLException: " + ex.getMessage());
            Logger.logger.logError("SQLState: " + ex.getSQLState());
            Logger.logger.logError("VendorError: " + ex.getErrorCode());
            Logger.logger.logGeneral(ex.getStackTrace()[1].toString());
        }
    }

    private void SendMsg(MessageChannel channel, String text){
        SendMsg(channel,text,null);
    }

    private void SendMsg(MessageChannel channel, String text, String codeStyle) {
        boolean codeBlock = codeStyle!=null;
        long messages = Math.round((Math.ceil(text.length() / 1000.0)));
        if (messages > 1) {
            int s = 0;
            int p = s,a;
            while (p!=text.length()){
                a = s;
                while ((a - s) < 1000 & a != -1) {
                    p = a;
                    a = text.indexOf("\n", p + 1);
                }
                if (a == -1)
                    p = text.length();
                if(p>s)
                    channel.sendMessage(
                                    ((codeBlock)?"```"+codeStyle+"\n":"") +
                                    text.substring(s, p) +
                                    ((codeBlock)?"\n```":"")
                    ).queue();
                s = p;
            }
        } else {
            channel.sendMessage(text).queue();
        }
    }

    private boolean guildIsSupport(Guild guild) {
        return guild.getIdLong() == Long.parseLong(System.getenv("SUPPORT_GUILD_ID"));
    }

    private boolean guildIdIsValid(long guildId, Message message) {
        JDA jda = message.getJDA();
        return jda.getGuildById(guildId) != null;
    }

    public MyListener(Connection conn) {
        this.conn = conn;
        this.botGuild = new BotGuild(conn);
    }

    public void close(){
        System.err.println(ansi().fgRed().a("Closing Statements").reset());
        botGuild.close();
        System.err.println(ansi().fgGreen().a("Statements closed").reset());
        System.err.println();
        System.err.println(ansi().fgRed().a("Closing threads").reset());
        eventThreads.shutdown();
        System.err.println(ansi().fgGreen().a("Threads closed").reset());
        System.err.println();
        try {
            System.err.println(ansi().fgRed().a("Closing connection").reset());
            conn.close();
            System.err.println(ansi().fgGreen().a("Connection closed").reset());
        } catch (SQLException ignored) {
        }
    }
}
