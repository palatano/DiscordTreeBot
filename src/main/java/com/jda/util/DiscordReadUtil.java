package com.jda.util;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Valued Customer on 7/20/2017.
 */
public class DiscordReadUtil {
    Map<String, String> userMap;

    public DiscordReadUtil() {
        userMap = new HashMap<>();
    }

    /**
     * Data analysis method for adding a new daily active user.
     * @param event - event of a received message.
     * @param publicMessage - message to be added. (MODIFY TO TIMESTAMP).
     */
    public void getPublicData(MessageReceivedEvent event, String publicMessage) {
        String userName = event.getMember().getEffectiveName();
        if (!userMap.containsKey(userName)) {
            userMap.put(userName, publicMessage);
        }
    }

    /**
     * Gets the history for the channel, between the interval 00:00:00 to 23:59:59 of the
     * same day. TESTED on #treehouse momentarily.
     * @param{event} - the event that is called upon for the history.
     * @param{timeStamp} - the time stamp of the user.
     */
    public void getDailyHistory(MessageChannel msgChan) {
        for (Message msg : getMessages(msgChan)) {
            System.out.println(MessageUtil.timeStamp(msg) + MessageUtil.userMsg(msg));
        }
    }

    /**
     * Get the messages from a specified channel.
     * @param channel - desired text channel.
     * @return list of messages.
     */
    public List<Message> getMessages(MessageChannel channel) {
        return channel.getIterableHistory().stream()
                .limit(100)
                .collect(Collectors.toList());
    }

}
