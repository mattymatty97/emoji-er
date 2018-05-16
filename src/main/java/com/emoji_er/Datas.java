package com.emoji_er;

import net.dv8tion.jda.core.entities.Guild;

class Datas{
    final boolean isGlobal;
    final boolean isRemote;
    final boolean isRep;
    final String  text;
    final Guild guild;
    final Guild   remote;

    public Datas(String text){
        this.text = text;
        this.isGlobal=true;
        this.isRemote=false;
        this.isRep = false;
        this.guild=null;
        this.remote=null;
    }

    public Datas(String text, Guild guild)  throws NullPointerException{
        if(guild==null){
            throw new NullPointerException();
        }
        this.text = text;
        this.guild = guild;
        this.isRep = false;
        this.isGlobal=false;
        this.isRemote=false;
        this.remote=null;
    }

    public Datas(String text, Guild guild, Guild remote,boolean isRep) throws NullPointerException {
        if(guild==null || remote==null){
            throw new NullPointerException();
        }
        this.text = text;
        this.guild = guild;
        this.remote = remote;
        this.isRep = isRep;
        this.isGlobal=false;
        this.isRemote=true;
    }
}

