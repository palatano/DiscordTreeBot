package commandutil.util;

import command.etc.TestCommand;
import commandutil.type.Command;

import java.util.Map;

/**
 * Created by Admin on 7/29/2017.
 */
public class CommandInit {

    public static void addCommands(Map<String, Command> commandList) {
        commandList.put("test", new TestCommand("test"));

    }
}
