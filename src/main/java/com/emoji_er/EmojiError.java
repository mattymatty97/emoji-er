package com.emoji_er;

public class EmojiError extends Error{
    EmojiError(){
        super();
    }
    EmojiError(String msg){
        super(msg);
    }
}
