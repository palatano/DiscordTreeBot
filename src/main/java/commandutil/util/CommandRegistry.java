package commandutil.util;

import commandutil.type.Command;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Admin on 7/29/2017.
 */
public class CommandRegistry {
    private static Map<String, Command> commandList;
    public static CommandRegistry commandRegistry;

    public static void setCommandRegistry() {
        // Add in the commands that can be used with CommandInit.
        commandList = new HashMap<>();
        CommandInit.addCommands(commandList);
        commandRegistry = new CommandRegistry();
    }

    public Command getCommand(String commandName) {
        return commandList.get(commandName);
    }

    public void addCommand() {

    }

    public void removeCommand() {

    }
}
