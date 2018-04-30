package com.emoji_er;

import net.dv8tion.jda.core.entities.MessageChannel;

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

    private Global(){
        mapChannel= new HashMap<>();
        mapGuild= new HashMap<>();
    }
}
