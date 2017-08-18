package tree.commandutil;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.TreeMain;
import tree.command.util.MessageUtil;
import tree.commandutil.type.Command;
import tree.commandutil.type.TextCommand;
import tree.commandutil.util.CommandRegistry;
import net.dv8tion.jda.core.entities.*;
import tree.util.LoggerUtil;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Admin on 7/28/2017.
 */
public class CommandManager {
    public static final String[] adminCommands = {"uniqueusers"};
    private static Logger log = LoggerFactory.getLogger(CommandManager.class);
    public static final String botToken = ";";
    public static final String bulletToken = "\u25C9";

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
            LoggerUtil.logMessage(log, message, "Command not found.");
            return false;
        }
        Command command = CommandRegistry.getCommand(args[0]);
        if (command == null) {
            LoggerUtil.logMessage(log, message, "Error retrieving command from the list.");
            return false;
        }
        // Ensure that the user doesnt use an admin command.
        if (userUsingAdmin(command, message)) {
            return false;
        }
        command.execute(message.getGuild(), message.getChannel(), message, message.getMember(), args);
        LoggerUtil.logMessage(log, message, command.getCommandName() + " executed.");
        return true;
    }

}
