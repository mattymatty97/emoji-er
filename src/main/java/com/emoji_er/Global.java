package com.emoji_er;

import net.dv8tion.jda.core.entities.MessageChannel;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class Global {
    private static Global gbl = new Global();
    public static Global getGbl(){
        return gbl;
    }

    private Map<Long,LogLinker> mapGuild;
    private Map<Long,LogLinker> mapChannel;
    private MessageChannel listener;
    private FileWriter fwGlobal;
    private Map<Long,FileWriter> fwServers;
    private MyListener eventlistener;

    public Map<Long, LogLinker> getMapGuild() {
        return mapGuild;
    }

    public Map<Long, LogLinker> getMapChannel() {
        return mapChannel;
    }

    public MessageChannel getListener() {
        return listener;
    }

    public void setListener(MessageChannel listener) {
        this.listener = listener;
    }

    public FileWriter getFwGlobal() {
        return fwGlobal;
    }

    public void setFwGlobal(FileWriter global) {
        this.fwGlobal = global;
    }

    public Map<Long, FileWriter> getFwServers() {
        return fwServers;
    }

    public MyListener getEventlistener() {
        return eventlistener;
    }

    public void setEventlistener(MyListener eventlistener) {
        this.eventlistener = eventlistener;
    }

    private Global(){
        mapChannel= new HashMap<>();
        mapGuild= new HashMap<>();
        fwServers=new HashMap<>();
    }
}
