package tree.commandutil.util;

import tree.commandutil.type.Command;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Admin on 7/29/2017.
 */
public class CommandRegistry {
    private static Map<String, Command> commandList;
    public static final String[] textCommands = {"test", "help", "bot", "broadcast"};
    public static final String[] pictureCommands = {"nugget"};
    public static final String[] analysisCommands = {"info", "uniqueusers", "google",
            "ping", "youtube", "set", "unset", "guildpermissions", "musicstats"};
    public static final String[] voiceCommands = {"voicesearch", "echo"};
    public static final String[] musicCommands = {"add", "skip", "pause", "list", "unpause",
            "leave", "join", "musichelp", "cnl", "req", "undo", "np"};

    public static void setCommandRegistry() {
        // Add in the commands that can be used with CommandInit.
        commandList = new HashMap<>();
        CommandInit.addCommands(commandList);
    }

    public static Command getCommand(String commandName) {
        return commandList.get(commandName);
    }

}
