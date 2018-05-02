package com.emoji_er;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
    private static final DateFormat stf = new SimpleDateFormat("HH:mm:ss");

    public static void logMessage(String log,Message message){

        String time = stf.format(new Date());
        FileWriter fw;
        StringBuilder sb = new StringBuilder();
        Member sender = message.getMember();
        logGeneral("event in guild "+message.getGuild().getName()+" ["+message.getGuild().getId()+"]");
        if((fw=openFile(message.getGuild()))!=null){
            try{
                fw.append("[").append(time).append("]\t");

                sb.append("messageId [").append(message.getId()).append("]\t| ");
                sb.append("User \"").append(sender.getEffectiveName()).append("\"(").append(sender.getUser().getId()).append(")");
                sb.append(" triggered ").append(log);

                fw.append(sb.toString()).append("\r\n");
                System.out.println(sb.toString());

                fw.flush();
                fw.close();
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        LogLinker act = Global.getGbl().getMapGuild().get(message.getGuild().getIdLong());
        if(act!=null){
            EmbedBuilder builder = act.getMessage();
            builder.setAuthor(message.getAuthor().getName(),null,message.getAuthor().getAvatarUrl());
            builder.setDescription(message.getRawContent());
        }
    }
    public static void logReponse(String log,Guild guild,long messageId){

        String time = stf.format(new Date());
        StringBuilder sb = new StringBuilder();
        FileWriter fw;
        if((fw=openFile(guild))!=null){
            try{
                fw.append("[").append(time).append("]\t");

                sb.append("messageId [").append(messageId).append("]\t| ").append(log);

                fw.append(sb.toString()).append("\r\n");
                System.out.println(sb.toString());

                fw.flush();
                fw.close();
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }

        LogLinker act = Global.getGbl().getMapGuild().get(guild.getIdLong());
        if(act!=null)
        {
            EmbedBuilder build = act.getMessage();
            build.addField("Reponse",log,false);
            act.getChannel().sendMessage(build.build()).queue();
            build.clearFields();
        }
    }
    public static void logEvent(String log,Guild guild){

        String time = stf.format(new Date());
        FileWriter fw;
        if((fw=openFile(guild))!=null){
            try{
                fw.append("[").append(time).append("]\t");

                fw.append(log).append("\r\n");

                logGeneral(log+": "+guild.getName());

                fw.flush();
                fw.close();
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        LogLinker act = Global.getGbl().getMapGuild().get(guild.getIdLong());
        if(act!=null)
        {
            EmbedBuilder build = act.getMessage();
            build.setAuthor(guild.getName(),null,guild.getIconUrl());
            build.setDescription("");
            build.addField("EVENT",log+": "+guild.getName(),false);
            act.getChannel().sendMessage(build.build()).queue();
            build.clearFields();
        }
    }

    public static void logGeneral(String log)
    {
        String date = sdf.format(new Date());
        String time = stf.format(new Date());
        File file = new File("./logs/"+date+"/BOT.log");
        if(file.getParentFile().exists() || file.getParentFile().mkdirs())
        {
            try {
                FileWriter fw;
                fw = new FileWriter(file, true);
                fw.append("[").append(time).append("]\t");
                fw.append(log).append("\r\n");
                System.out.println(log);
                fw.close();
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }


    public static void logRemoteMsg(String log, Message message, Guild guild){

        String time = stf.format(new Date());
        FileWriter fw;
        StringBuilder sb = new StringBuilder();
        Member sender = message.getMember();
        logGeneral("event in guild "+message.getGuild().getName()+" ["+message.getGuild().getId()+"]");
        if((fw=openFile(message.getGuild()))!=null){
            try{
                fw.append("[").append(time).append("]\t");

                sb.append("messageId [").append(message.getId()).append("]\t| ");
                sb.append("User \"").append(sender.getEffectiveName()).append("\"(").append(sender.getUser().getId()).append(")");
                sb.append(" triggered ").append(log).append(" on guild ").append(guild.getName());
                sb.append("[").append(guild.getId()).append("]");

                fw.append(sb.toString()).append("\r\n");
                System.out.println(sb.toString());

                fw.flush();
                fw.close();
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        if((fw=openFile(guild))!=null){
            try{
                sb.replace(0,sb.length(),"");
                fw.append("[").append(time).append("]\t");

                sb.append("messageId [").append(message.getId()).append("]\t| ");
                sb.append("User \"").append(sender.getEffectiveName()).append("\"(").append(sender.getUser().getId()).append(")");
                sb.append(" triggered ").append(log).append(" remotely");

                fw.append(sb.toString()).append("\r\n");
                //System.out.println(sb.toString());

                fw.flush();
                fw.close();
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    public static void logRemoteRep(String log,Guild guild,long messageId,Guild remote){

        String time = stf.format(new Date());
        StringBuilder sb = new StringBuilder();
        FileWriter fw1,fw2;
        if((fw1=openFile(guild))!=null && (fw2=openFile(remote))!=null){
            try{
                fw1.append("[").append(time).append("]\t");
                fw2.append("[").append(time).append("]\t");


                sb.append("messageId [").append(messageId).append("]\t| ").append(log);

                fw1.append(sb.toString()).append("\r\n");
                fw2.append(sb.toString()).append("\r\n");
                System.out.println(sb.toString());

                fw1.flush();
                fw2.flush();
                fw1.close();
                fw2.close();
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    public static void logInit()
    {
        String date = sdf.format(new Date());
        File file = new File("./logs/"+date+"/BOT.log");
        if(file.getParentFile().exists() || file.getParentFile().mkdirs())
        {
            try {
                FileWriter fw;
                fw = new FileWriter(file, true);
                fw.append("\r\n\r\n");
                System.out.println("\r\n\r\n");
                fw.close();
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }


    private static FileWriter openFile(Guild guild)
    {
        String date = sdf.format(new Date());
        File file = new File("./logs/"+date+"/"+guild.getIdLong()+".log");
        if(file.getParentFile().exists() || file.getParentFile().mkdirs())
        {
            try {
                FileWriter fw;
                boolean existing=false;
                if(file.exists())
                    existing=true;
                fw = new FileWriter(file, true);
                if(!existing){
                    fw.append("FILE CREATED FOR GUILD:\r\n").append(guild.getName()).append("\r\n");
                }
                return fw;
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        return null;
    }


}
