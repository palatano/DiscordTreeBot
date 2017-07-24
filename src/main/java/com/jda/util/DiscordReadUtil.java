package com.jda.util;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageHistory;
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
    DataUtil dataUtil;

    public DiscordReadUtil(DataUtil dataUtil) {
        userMap = new HashMap<>();
        this.dataUtil = dataUtil;
    }

    public boolean inSameDay(Message msg, int day) {
        OffsetDateTime msgDate = msg.getCreationTime().minusHours(4);
        return msgDate.getDayOfMonth() == day;
    }

    /**
     * Get the first batch of messages that correspond to the specified day.
     * @param day - the given day.
     * @param msgHistory - the specified message channel.
     * @return The first batch of messages to iterate over.
     */
    public List<Message> getFirstMessagesBySpecifiedDay(int day, MessageHistory msgHistory) {
        // Get the message history, if it exists.
        List<Message> messageList = getMessages(msgHistory);
        if (messageList.isEmpty()) {
            System.out.println("No history available in channel.");
            return messageList;
        }
        // Make sure the specified date is not later than today.
        Message firstMsg = messageList.get(0);
        if (firstMsg.getCreationTime().minusHours(4).getDayOfMonth() < day) {
            System.out.println("Specified day is later than the most recent message. Error.");
            return null;
        }
        // Attempt to find the messages until it matches the day.
        Message currMsg = messageList.get(messageList.size() - 1);
        while (currMsg.getCreationTime().minusHours(4).getDayOfMonth() > day) {
            messageList = getMessages(msgHistory);
            currMsg = messageList.get(messageList.size() - 1);
        }
        return messageList;
    }


    public void iterateMessagesBySpecifiedDay(int day, MessageHistory msgHistory,
                                              List<Message> messageList) {
        // Print out the messages.
        MessageChannel msgChan = msgHistory.getChannel();
        for (Message msg : messageList) {
            if (inSameDay(msg, day)) {
                String userMsg = MessageUtil.userMsg(msg);
                if (userMsg == null) {
                    continue;
                }
                System.out.println(MessageUtil.timeStamp(msg) + MessageUtil.userMsg(msg));
                dataUtil.putUniqueUser(msg, msgChan);
            }
        }
        Message lastMsg = messageList.get(messageList.size() - 1);
        if (!inSameDay(lastMsg, day)) {
            return;
        } else {
            iterateMessagesBySpecifiedDay(day, msgHistory, getMessages(msgHistory));
        }
    }

    /**
     * Gets the history for the channel, between the interval 00:00:00 to 23:59:59 of the
     * same day. TESTED on #treehouse momentarily.
     * @param{event} - the event that is called upon for the history.
     * @param{timeStamp} - the time stamp of the user.
     */
    public void getDailyHistory(MessageChannel msgChan, String currMsg, boolean toWrite) {
        currMsg = currMsg.replace("!get ", "");
        String[] listStrings = currMsg.split("/");
        int[] dateValues = MessageUtil.parseDate(listStrings);
        dataUtil.createMap();
        MessageHistory msgHistory = new MessageHistory(msgChan);
        List<Message> messageList = getFirstMessagesBySpecifiedDay(dateValues[1], msgHistory);
        // If no history or specified is later than today, return.
        if (messageList == null) {
            return;
        } else if (messageList.isEmpty()) {
            return;
        }
        iterateMessagesBySpecifiedDay(dateValues[1], msgHistory, messageList);
        dataUtil.putUniqueUserMap(msgChan.getName());
        dataUtil.writeChannelDataExcel(msgChan.getName(), dateValues, toWrite, msgChan);
    }

    /**
     * Get the messages from a specified channel.
     * @param msgHistory - desired text channel.
     * @return list of messages.
     */
    public List<Message> getMessages(MessageHistory msgHistory) {
        return msgHistory.retrievePast(100).complete();
    }

}