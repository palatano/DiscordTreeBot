package com.jda.util;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.time.OffsetDateTime;
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

    public boolean inSameDay(Message msg, int day) {
        OffsetDateTime msgDate = msg.getCreationTime().minusHours(4);
        return msgDate.getDayOfMonth() == day;
    }

    /**
     * Get the first batch of messages that correspond to the specified day.
     * @param day - the given day.
     * @param msgChan - the specified message channel.
     * @return The first batch of messages to iterate over.
     */
    public List<Message> getFirstMessagesBySpecifiedDay(int day, MessageChannel msgChan) {
        List<Message> messageList = getMessages(msgChan);
        Message firstMsg = messageList.get(0);
        if (firstMsg.getCreationTime().getDayOfMonth() < day) {
            System.out.println("Specified day is later than the most recent message. Error.");
            return null;
        } else if (messageList.isEmpty()) {
            System.out.println("No history available in channel.");
            return messageList;
        }
        Message currMsg = messageList.get(messageList.size() - 1);
        if (!inSameDay(currMsg, day)) {
            getFirstMessagesBySpecifiedDay(day, msgChan);
        }
        return messageList;
    }


    public void iterateMessagesBySpecifiedDay(int day, MessageChannel msgChan,
                                              List<Message> messageList) {
        for (Message msg : messageList) {
            if (inSameDay(msg, day)) {
                System.out.println(MessageUtil.timeStamp(msg) + MessageUtil.userMsg(msg));
            }
        }
    }

    /**
     * Gets the history for the channel, between the interval 00:00:00 to 23:59:59 of the
     * same day. TESTED on #treehouse momentarily.
     * @param{event} - the event that is called upon for the history.
     * @param{timeStamp} - the time stamp of the user.
     */
    public void getDailyHistory(MessageChannel msgChan, String currMsg) {
        currMsg = currMsg.replace("!get ", "");
        String[] listStrings = currMsg.split("/");
        int[] dateValues = MessageUtil.parseDate(listStrings);
        List<Message> messageList = getFirstMessagesBySpecifiedDay(dateValues[1], msgChan);
        if (messageList == null) {
            return;
        } else if (messageList.isEmpty()) {
            return;
        }
        iterateMessagesBySpecifiedDay(dateValues[1], msgChan, messageList);
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
