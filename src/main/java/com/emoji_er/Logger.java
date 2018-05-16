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
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class Logger implements Runnable{
    private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
    private static final DateFormat stf = new SimpleDateFormat("HH:mm:ss");
    private static String lastDate="0/0/0";
    private static Queue<Datas> queue = new LinkedList<>();
    public static Logger logger = new Logger();
    public static Thread tlogger = new Thread(logger);

    public void logMessage(String log,Message message){

        String time = stf.format(new Date());
        FileWriter fw;
        StringBuilder sb = new StringBuilder();
        Member sender = message.getMember();
        logGeneral("event in guild "+message.getGuild().getName()+" ["+message.getGuild().getId()+"]");

        sb.append("[").append(time).append("]\t");

        sb.append("messageId [").append(message.getId()).append("]\t| ");
        sb.append("User \"").append(sender.getEffectiveName()).append("\"(").append(sender.getUser().getId()).append(")");
        sb.append(" triggered ").append(log);
        System.out.println(sb.toString());

        queue.add(new Datas(sb.toString(),message.getGuild()));
        synchronized (this){notify();}

        LogLinker act = Global.getGbl().getMapGuild().get(message.getGuild().getIdLong());
        if(act!=null){
            EmbedBuilder builder = act.getMessage();
            builder.setAuthor(message.getAuthor().getName(),null,message.getAuthor().getAvatarUrl());
            builder.setDescription(message.getRawContent());
        }
    }

    public void logReponse(String log,Guild guild,long messageId){

        String time = stf.format(new Date());
        StringBuilder sb = new StringBuilder();

        sb.append("[").append(time).append("]\t");

        sb.append("messageId [").append(messageId).append("]\t| ").append(log);

        System.out.println(sb.toString());

        queue.add(new Datas(sb.toString(),guild));
        synchronized (this){notify();}

        LogLinker act = Global.getGbl().getMapGuild().get(guild.getIdLong());
        if(act!=null)
        {
            EmbedBuilder build = act.getMessage();
            build.addField("Reponse",log,false);
            act.getChannel().sendMessage(build.build()).queue();
            build.clearFields();
        }
    }

    public void logEvent(String log,Guild guild){

        String time = stf.format(new Date());
        StringBuilder sb = new StringBuilder();

        logGeneral(log+": "+guild.getName());

        sb.append("[").append(time).append("]\t");

        sb.append(log);

        queue.add(new Datas(sb.toString(),guild));
        synchronized (this){notify();}

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

    public void logGeneral(String log)
    {
        String time = stf.format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(time).append("]\t");
        sb.append(log);
        System.out.println(sb.toString());

        queue.add(new Datas(sb.toString()));
        synchronized (this){notify();}
    }

    public void logRemoteMsg(String log, Message message, Guild guild){

        String time = stf.format(new Date());
        StringBuilder sb = new StringBuilder();
        Member sender = message.getMember();
        logGeneral("event in guild "+message.getGuild().getName()+" ["+message.getGuild().getId()+"]");
                sb.append("[").append(time).append("]\t");

                sb.append("messageId [").append(message.getId()).append("]\t| ");
                sb.append("User \"").append(sender.getEffectiveName()).append("\"(").append(sender.getUser().getId()).append(")");
                sb.append(" triggered ").append(log);
                sb.append("[").append(guild.getId()).append("]");

                System.out.println(sb.toString());

                queue.add(new Datas(sb.toString(),message.getGuild(),guild,false));

                synchronized (this){notify();}
    }

    public void logRemoteRep(String log,Guild guild,long messageId,Guild remote){

        String time = stf.format(new Date());
        StringBuilder sb = new StringBuilder();

        sb.append("[").append(time).append("]\t");
        sb.append("messageId [").append(messageId).append("]\t| ").append(log);
        System.out.println(sb.toString());
        queue.add(new Datas(sb.toString(),guild,remote,true));
    }


    public void logInit()
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
                fw.flush();
                Global.getGbl().setFwGlobal(fw);
                lastDate=date;
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    private FileWriter openFile(Guild guild)
    {
        String date = sdf.format(new Date());
        File file = new File("./logs/"+date+"/"+guild.getIdLong()+".log");
        if (!date.equals(lastDate)) {
            closeFiles();
            lastDate=date;
        }

        FileWriter fw = Global.getGbl().getFwServers().get(guild.getIdLong());

        if(fw!=null){
            return fw;
        }

        if(file.getParentFile().exists() || file.getParentFile().mkdirs())
        {
            try {
                boolean existing=false;
                if(file.exists()){
                    existing=true;
                }
                fw = new FileWriter(file, true);
                if(!existing){
                    fw.append("FILE CREATED FOR GUILD:\r\n").append(guild.getName()).append("\r\n");
                }
                Global.getGbl().getFwServers().put(guild.getIdLong(),fw);
                return fw;
            }catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        return null;
    }

    private FileWriter openFile()
    {
        String date = sdf.format(new Date());
        String time = stf.format(new Date());
        File file = new File("./logs/"+date+"/BOT.log");
        if (!date.equals(lastDate)) {
            closeFiles();
            lastDate=date;
        }

        FileWriter fw = Global.getGbl().getFwGlobal();
        if(fw!=null)
            return fw;
        if(file.getParentFile().exists() || file.getParentFile().mkdirs())
        {
            try {
                fw = new FileWriter(file, true);
                Global.getGbl().setFwGlobal(fw);
                return fw;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void closeFiles(){
        Collection<FileWriter> fileWriters = Global.getGbl().getFwServers().values();
        for (FileWriter fw: fileWriters) {
            try {
                fw.flush();
                fw.close();
            }catch (IOException ignored){
            }
        }
        Global.getGbl().getFwServers().clear();
        FileWriter g = Global.getGbl().getFwGlobal();
        try {
            g.flush();
            g.close();
        } catch (IOException ignored) {
        }
        Global.getGbl().setFwGlobal(null);
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                while (true) {
                    while (queue.size() > 0) {
                        FileWriter fw, fw1, fw2;
                        Datas data = queue.poll();
                        if (data.isGlobal) {
                            if ((fw = openFile()) != null) {
                                try {
                                    fw.append(data.text);
                                    fw.flush();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        } else if (data.isRemote) {
                            if (data.isRep) {
                                if ((fw1 = openFile(data.guild)) != null && (fw2 = openFile(data.remote)) != null) {
                                    try {
                                        fw1.append(data.text).append("\r\n");
                                        fw2.append(data.text).append("\r\n");
                                        fw1.flush();
                                        fw2.flush();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            } else {
                                if ((fw = openFile(data.guild)) != null) {
                                    try {
                                        fw.append(data.text);
                                        fw.append(" on guild ").append(data.remote.getName());
                                        fw.append("\r\n");
                                        fw.flush();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                if ((fw = openFile(data.remote)) != null) {
                                    try {
                                        fw.append(data.text);
                                        fw.append(" remotely");
                                        fw.append("\r\n");
                                        fw.flush();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            if ((fw = openFile(data.guild)) != null) {
                                try {
                                    fw.append(data.text).append("\r\n");
                                    fw.flush();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                    wait();
                }
            } catch (InterruptedException ex) {
                System.err.println("Exiting logger daemon");
            }
        }
    }
}
