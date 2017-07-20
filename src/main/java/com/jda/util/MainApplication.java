package com.jda.util;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.time.OffsetDateTime;

/**
 * Created by Valued Customer on 7/20/2017.
 */
public class MainApplication extends ListenerAdapter {

    public static void main(String[] args)
            throws LoginException, RateLimitedException, InterruptedException {
        JDA jda = new JDABuilder(AccountType.BOT)
                .setToken("MzM3NjI3MzEyODMwOTM5MTM2.DFJo8w.2LTfEowEWZtAGN7A7QvzXeNibf8")
                .buildBlocking();
        jda.addEventListener(new MainApplication());
    }

    private String getTime() {
        OffsetDateTime time = OffsetDateTime.now();
        int hour = time.getHour() > 12 ? time.getHour() % 12 : time.getHour();
        String minute = time.getMinute() < 10 ? "0" + time.getMinute() :
                Integer.toString(time.getMinute());
        String second = time.getSecond() < 10 ? "0" + time.getSecond() :
                Integer.toString(time.getSecond());
        String zone = time.getHour() > 12 ? "PM" : "AM";
        return hour + ":" + minute + ":" + second
                + " " + zone;
    }

    private String getDate() {
        OffsetDateTime date = OffsetDateTime.now();
        return date.getMonth() + " " + date.getDayOfMonth() + ", " + date.getYear();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.printf("[PM] %s: %s\n", event.getAuthor().getName(),
                    event.getMessage().getContent());
        }
        else {
            System.out.printf("[" + getDate() + "][" + getTime() + "]" +
                            "[Chan: %s] %s: %s\n",
                    event.getTextChannel().getName(),
                    event.getMember().getEffectiveName(),
                    event.getMessage().getContent());
        }
    }
}
