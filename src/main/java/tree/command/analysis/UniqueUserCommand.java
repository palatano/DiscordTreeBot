package tree.command.analysis;

import net.dv8tion.jda.core.entities.*;
import org.apache.commons.lang3.time.StopWatch;
import tree.command.util.MessageUtil;
import tree.command.data.SerializableMessageHistory;
import tree.command.util.DataUtil;

//import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.*;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;

import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Valued Customer on 7/20/2017.
 */
public class UniqueUserCommand implements AnalysisCommand {
    private String commandName;
    private static final long[] channelList = {249764885246902272L, 247929469552033792L,
    269577202016845824L, 247135478069854209L, 247248468626636800L, 248243893273886720L,
    247134894558281730L};
    Map<String, String> userMap;
    DataUtil dataUtil;
    SerializableMessageHistory msgHistory;
    StopWatch stopwatch;
    private Calendar cal;

    public UniqueUserCommand(String commandName) {
        this.commandName = commandName;
        userMap = new HashMap<>();
        this.dataUtil = new DataUtil();
        msgHistory = null;
    }

    public boolean inSameDay(Message msg, Calendar cal) {
        OffsetDateTime msgDate = msg.getCreationTime().minusHours(4);
        return msgDate.getDayOfYear() == cal.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Get the first batch of messages that correspond to the specified day.
     * @param cal - calendar representing the date to search with.
     * @param msgHistory - the specified message channel.
     * @return The first batch of messages to iterate over.
     */
    public List<Message> getFirstMessagesBySpecifiedDay(Calendar cal, MessageHistory msgHistory) {
        // Get the message history, if it exists.
        List<Message> messageList = getMessages(msgHistory);
        if (messageList.isEmpty()) {
            System.out.println("No history available in channel.");
            return messageList;
        }
        // Make sure the specified date is not later than today.
        Message firstMsg = messageList.get(0);
        OffsetDateTime msgDate = firstMsg.getCreationTime().minusHours(4);
        if (MessageUtil.isInvalidFutureDate(cal, msgDate)) {
            System.out.println("Specified day is later than the most recent message. Error.");
            return null;
        }
        // Attempt to find the messages until it matches the day.
        Message currMsg = messageList.get(messageList.size() - 1);
        int currDay = currMsg.getCreationTime().minusHours(4).getDayOfMonth();
        while (MessageUtil.msgDateAfterCal(cal, msgDate) && messageList.size() == 100) {
            messageList = getMessages(msgHistory);
            currMsg = messageList.get(messageList.size() - 1);
            System.out.println("Day is " + currDay + " with time " +
                    stopwatch.toString());
            msgDate = currMsg.getCreationTime().minusHours(4);
            currDay = msgDate.getDayOfMonth();
        }
        return messageList;
    }


    public void iterateMessagesBySpecifiedDay(Calendar cal, MessageHistory msgHistory,
                                              List<Message> messageList) {
        // Print out the messages.
        MessageChannel msgChan = msgHistory.getChannel();
        for (Message msg : messageList) {
            if (inSameDay(msg, cal)) {
                String userMsg = MessageUtil.userMsg(msg);
                if (userMsg == null) {
                    continue;
                }
                System.out.println(MessageUtil.timeStamp(msg) + MessageUtil.userMsg(msg));
                dataUtil.putUniqueUser(msg, msgChan);
            }
        }
        if (messageList.isEmpty()) {
            System.out.println("History is either empty or have already hit the end of the message history.");
            return;
        }
        Message lastMsg = messageList.get(messageList.size() - 1);
        if (!inSameDay(lastMsg, cal)) {
            return;
        } else {
            iterateMessagesBySpecifiedDay(cal, msgHistory, getMessages(msgHistory));
        }
    }

    /**
     * Gets the history for the channel, between the interval 00:00:00 to 23:59:59 of the
     * same day. TESTED on #treehouse momentarily.
     * @param{event} - the event that is called upon for the history.
     * @param{timeStamp} - the time stamp of the user.
     */
    public void getDailyHistory(MessageChannel msgChan, String currMsg, boolean toWrite) {
        // Get the date from the tree.command.
        currMsg = currMsg.replace("&uniqueusers ", "");
        String[] listStrings = currMsg.split("/");
        cal = MessageUtil.parseDate(listStrings);
        // Create a new data structure for storing channel message data and retrieve the previous
        // message history, if it exists.
        dataUtil.createUniqueUsersMap();
        List<Message> messageList = null;
        msgHistory = new SerializableMessageHistory(msgChan);
        stopwatch = new StopWatch();
        stopwatch.start();
        messageList = getFirstMessagesBySpecifiedDay(cal, msgHistory);
        stopwatch.stop();
        //System.out.println(stopwatch.toString());
        // If no history or specified is later than today, return.
        if (messageList == null) {
            return;
        } else if (messageList.isEmpty()) {
            return;
        }
        dataUtil.setDate(cal);
        iterateMessagesBySpecifiedDay(cal, msgHistory, messageList);
        dataUtil.putUniqueUserMap(msgChan.getName());
        dataUtil.writeChannelDataExcel(msgChan.getName(), cal, toWrite, msgChan);
    }

    /**
     * Get the messages from a specified channel.
     * @param msgHistory - desired text channel.
     * @return list of messages.
     */
    public List<Message> getMessages(MessageHistory msgHistory) {
        return msgHistory.retrievePast(100).complete();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (!MessageUtil.getCheckIfValidDate(message.getContent())) {
            return;
        }
        for (long channelID : channelList) {
            MessageChannel messageChannel = guild.getTextChannelById(channelID);
            getDailyHistory(messageChannel, message.getContent(), false);
            dataUtil.writeChannelDataExcel(messageChannel.getName(), cal, false, msgChan);
        }
        dataUtil.writeAllChannelDataExcel(msgChan);
//        guild.getJDA().get
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + " [date]``: Gives an excel file with the unique user name, time stamp, and message.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}