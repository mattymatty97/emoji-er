package com.emoji_er;

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

    }
    public static void logEvent(String log,Guild guild){

        String time = stf.format(new Date());
        FileWriter fw;
        if((fw=openFile(guild))!=null){
            try{
                fw.append("[").append(time).append("]\t");

                fw.append(log).append("\r\n");

                System.out.println(log+" in guild:"+guild.getName());

                logGeneral(log+" in guild:"+guild.getName());

                fw.flush();
                fw.close();
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
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
