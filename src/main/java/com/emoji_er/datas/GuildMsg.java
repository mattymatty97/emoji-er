package com.emoji_er.datas;

import net.dv8tion.jda.core.entities.Guild;

import java.util.Date;

public class GuildMsg implements Datas{
        final private String text;
        final private boolean reponse;
        final private Guild guild;
        final private Date current;

    public GuildMsg(Date current,String text, Guild guild, boolean reponse) {
        this.text = text;
        this.reponse = reponse;
        this.guild = guild;
        this.current=current;
    }

    @Override
    public String getText() {
            return text;
    }

    @Override
    public Date getDate() {
        return current;
    }

    public boolean isReponse() {
        return reponse;
    }

    public Guild getGuild() {
        return guild;
    }
}
