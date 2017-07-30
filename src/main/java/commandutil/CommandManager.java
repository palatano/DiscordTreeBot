package commandutil;

import command.analysis.DateFindUtil;
import command.util.MessageUtil;
import commandutil.type.Command;
import commandutil.util.CommandRegistry;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Admin on 7/28/2017.
 */
public class CommandManager {
    private static final String[] channelIDList = {"249764885246902272", "247929469552033792",
            "269577202016845824", "247135478069854209", "247248468626636800",
            "248243893273886720", "247134894558281730"};
    private static final String[] rulesInfo = {"247109092567547905"};
    private static boolean followUpCommand = false;
    public static DateFindUtil dateFinder;
    private boolean nugPicAllowed = true;
    private int numNugCount = 0;
//    private CommandRegistry commandRegistry;

    public static void init() {
        CommandRegistry.setCommandRegistry();
        //discUtil = new DailyUniqueUsers();
        dateFinder = new DateFindUtil();
       // timerInitialize();
    }

    private void timerInitialize() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                nugPicAllowed = true;
                numNugCount = 0;
            }
        }, 0, 30000);
    }

    public static boolean messageCommand(Message message) {
        String msgText = message.getContent();
        String[] args = CommandParser.parseMessage(msgText);
        Command command = CommandRegistry.commandRegistry.getCommand(args[0]);
        if (command == null) {
            System.out.println("Error retrieving command from the list.");
            return false;
        }
        command.execute(message.getGuild(), message.getChannel(), message, message.getMember());
        return false;
    }
/*

    private boolean testingOnly(MessageChannel msgChan) {
        return msgChan.getId().equals("337641574249005065");
    }

    private boolean inBotChannel(MessageChannel msgChan) {
        return msgChan.getId().equals("249791455592316930");
    }

    private void sendPrivateMessage(String s, User user) {

    }

    public void userCommand(MessageReceivedEvent event) {
        String msgContent = event.getMessage().getContent();
        if (msgContent.startsWith("!dateJoined")) {
            dateFinder.getDateJoined(event);
        } else if (msgContent.equals("!nug") &&
                inBotChannel(event.getTextChannel())) {
            if (!nugPicAllowed) {
                event.getTextChannel()
                        .sendMessage("Too many !nug commands. Chill out bro.").queue();
                return;
            }
            if (++numNugCount >= 3) {
                nugPicAllowed = false;
            }
            dataUtil.writeRandomNugPhoto(event.getTextChannel());
        }
        return;
    }

    public void adminCommand(MessageReceivedEvent event) {
        String msgContent = event.getMessage().getContent();
        if (msgContent.equals("!test")) {
            /* Test if reaction word is created.
            event.getMessage().addReaction("\uD83D\uDE02").queue();
        } else if (msgContent.startsWith("!writeSingle") &&
                MessageUtil.getCheckIfValidDate(msgContent)) {
            /* Get Message History from channel.
            discUtil.getDailyHistory(event.getTextChannel(), msgContent,
                    MessageUtil.checkIfWrite(msgContent));
        } else if (msgContent.startsWith("!writeAll") &&
                MessageUtil.getCheckIfValidDate(msgContent)) {
            dataUtil.resetMap();
            for (String channelID : rulesInfo) {
                // Get the channel and message history to iterate over.
                TextChannel channel = event.getGuild().getTextChannelById(channelID);
                discUtil.getDailyHistory(channel, "!get " +
                        msgContent.replace("!writeData ", ""), false);
                // Set the message history to null to reset for the next operation.
            }
            dataUtil.writeAllChannelDataExcel(event.getTextChannel());
        } else if (msgContent.equals("!nug")) {
            if (!nugPicAllowed) {
                event.getTextChannel()
                        .sendMessage("Too many !nug commands. Chill out bro.").queue();
                return;
            }
            if (++numNugCount >= 3) {
                nugPicAllowed = false;
            }
            dataUtil.writeRandomNugPhoto(event.getTextChannel());
        } else if (msgContent.startsWith("!dateJoined")) {
            dateFinder.getDateJoined(event);
            return;
        } else if (msgContent.startsWith("!sendMsg")) {
            String[] arguments = getArguments(msgContent, 2);
            if (arguments == null) {
                return;
            }
            sendPrivateMessage(arguments[1], event.getAuthor());
        }
    }*/
}
