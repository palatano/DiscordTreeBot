package com.jda.main;

import com.jda.util.DiscordReadUtil;
import com.jda.util.MessageUtil;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

/**
 * Created by Valued Customer on 7/20/2017.
 */
public class MainApplication extends ListenerAdapter {
    private DiscordReadUtil discUtil = new DiscordReadUtil();

    public static void main(String[] args)
            throws LoginException, RateLimitedException, InterruptedException {
        JDA jda = new JDABuilder(AccountType.BOT)
                .setToken("MzM3NjI3MzEyODMwOTM5MTM2.DFJo8w.2LTfEowEWZtAGN7A7QvzXeNibf8")
                .buildBlocking();
        jda.addEventListener(new MainApplication());
    }

    /**
     * Get the current message based on the !curr event.
     * @param event - event of a received message.
     */
    private void getCurrMessage(MessageReceivedEvent event) {
        String publicMessage = String.format(MessageUtil.timeStamp(event.getMessage()) +
                       MessageUtil.userMsg(event.getMessage()));
        String privateMessage = String.format("[PM] %s: %s\n", event.getAuthor().getName(),
                event.getMessage().getContent());
        if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.printf(privateMessage);
        }
        else {
            System.out.printf(publicMessage);
        }
    }

    /**
     * Confirm that the user is palat, so the commands won't be abused.
     * @param event - event of a received message.
     * @return boolean depending if user is palat.
     */
    private boolean isMainUser(MessageReceivedEvent event) {
        String name = event.getAuthor().getId();
        return name.equals("192372494202568706");
    }

    @Override
    /**
     * The main message listener for testing purposes.
     * @param{event} - event of a received message.
     */
    public void onMessageReceived(MessageReceivedEvent event) {
        /* Allow only the admin to access the following commands. */
        if (!isMainUser(event)) {
            return;
        }
        JDA jda = event.getJDA();
        String msgContent = event.getMessage().getContent();
        if (msgContent.equals("!test")) {
            /* Test if reaction word is created. */
            event.getMessage().addReaction("\uD83D\uDE02").queue();
        } else if (msgContent.equals("!get")) {
            /* Get Message History from channel. */
            discUtil.getDailyHistory(jda.getTextChannelById("247929469552033792"));
            /* Get current message from channel. */
        } else if (msgContent.equals("!curr")) {
            getCurrMessage(event);
        }

    }
}