package com.emoji_er;

import java.util.HashMap;
import java.util.Map;

public class Global {
    private static Global gbl = new Global();
    public static Global getGbl(){
        return gbl;
    }

    private Map<Long,LogLinker> mapGuild;
    private Map<Long,LogLinker> mapChannel;

    public Map<Long, LogLinker> getMapGuild() {
        return mapGuild;
    }

    public Map<Long, LogLinker> getMapChannel() {
        return mapChannel;
    }

    private Global(){
        mapChannel= new HashMap<>();
        mapGuild= new HashMap<>();
    }
}
