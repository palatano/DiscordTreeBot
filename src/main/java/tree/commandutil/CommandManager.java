package tree.commandutil;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import tree.command.util.MessageUtil;
import tree.commandutil.type.Command;
import tree.commandutil.type.TextCommand;
import tree.commandutil.util.CommandRegistry;
import net.dv8tion.jda.core.entities.*;

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
    private static final String[] adminCommands = {"uniqueusers"};
    private static boolean followUpCommand = false;
    private boolean nugPicAllowed = true;
    private int numNugCount = 0;

    public static void init() {
        CommandRegistry.setCommandRegistry();
    }

    private static boolean isMainUser(User user) {
        String name = user.getId();
        return name.equals("192372494202568706");
    }

    private static boolean userUsingAdmin(Command command, Message message) {
        for (String adminCommand : adminCommands) {
            if (command.getCommandName().equals(adminCommand) && !isMainUser(message.getAuthor())) {
                System.out.println(message.getAuthor() +
                        " tried to enter an admin command at " +
                        MessageUtil.timeStamp(message));
                return true;
            }
        }
        return false;
    }

    public static boolean messageCommand(Message message) {
        String msgText = message.getContent();
        String[] args = CommandParser.parseMessage(msgText);
        if (args == null) {
            System.out.println("Command not found.");
            return false;
        }
        Command command = CommandRegistry.getCommand(args[0]);
        if (command == null) {
            System.out.println("Error retrieving command from the list.");
            return false;
        }
        // Ensure that the user doesnt use an admin command.
        if (userUsingAdmin(command, message)) {
            return false;
        }
        command.execute(message.getGuild(), message.getChannel(), message, message.getMember(), args);
        return false;
    }


    private void sendPrivateMessage(String s, User user) {

    }
    /*

    public void userCommand(MessageReceivedEvent tree.event) {
        String msgContent = tree.event.getMessage().getContent();
        if (msgContent.startsWith("!dateJoined")) {
            dateFinder.getDateJoined(tree.event);
        } else if (msgContent.equals("!nug") &&
                inBotChannel(tree.event.getTextChannel())) {
            if (!nugPicAllowed) {
                tree.event.getTextChannel()
                        .sendMessage("Too many !nug commands. Chill out bro.").queue();
                return;
            }
            if (++numNugCount >= 3) {
                nugPicAllowed = false;
            }
            dataUtil.writeRandomNugPhoto(tree.event.getTextChannel());
        }
        return;
    }

    public void adminCommand(MessageReceivedEvent tree.event) {
        String msgContent = tree.event.getMessage().getContent();
        if (msgContent.equals("!test")) {
            /* Test if reaction word is created.
            tree.event.getMessage().addReaction("\uD83D\uDE02").queue();
        } else if (msgContent.startsWith("!writeSingle") &&
                MessageUtil.getCheckIfValidDate(msgContent)) {
            /* Get Message History from channel.
            discUtil.getDailyHistory(tree.event.getTextChannel(), msgContent,
                    MessageUtil.checkIfWrite(msgContent));
        } else if (msgContent.startsWith("!writeAll") &&
                MessageUtil.getCheckIfValidDate(msgContent)) {
            dataUtil.resetMap();
            for (String channelID : rulesInfo) {
                // Get the channel and message history to iterate over.
                TextChannel channel = tree.event.getGuild().getTextChannelById(channelID);
                discUtil.getDailyHistory(channel, "!get " +
                        msgContent.replace("!writeData ", ""), false);
                // Set the message history to null to reset for the next operation.
            }
            dataUtil.writeAllChannelDataExcel(tree.event.getTextChannel());
        } else if (msgContent.equals("!nug")) {
            if (!nugPicAllowed) {
                tree.event.getTextChannel()
                        .sendMessage("Too many !nug commands. Chill out bro.").queue();
                return;
            }
            if (++numNugCount >= 3) {
                nugPicAllowed = false;
            }
            dataUtil.writeRandomNugPhoto(tree.event.getTextChannel());
        } else if (msgContent.startsWith("!dateJoined")) {
            dateFinder.getDateJoined(tree.event);
            return;
        } else if (msgContent.startsWith("!sendMsg")) {
            String[] arguments = getArguments(msgContent, 2);
            if (arguments == null) {
                return;
            }
            sendPrivateMessage(arguments[1], tree.event.getAuthor());
        }
    }*/
}
