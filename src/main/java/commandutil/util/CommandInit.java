package commandutil.util;

import command.etc.TestCommand;
import commandutil.type.AbstractCommandFactory;
import commandutil.type.Command;
import commandutil.type.FactoryProducer;
import commandutil.type.TextCommand;

import java.util.Map;

/**
 * Created by Admin on 7/29/2017.
 */
public class CommandInit {

    public static void addCommands(Map<String, Command> commandList) {
        AbstractCommandFactory abstractFactory = FactoryProducer.getFactory("TEXT");

        Command testCommand = abstractFactory.getTextCommand("test");
        commandList.put(testCommand.getCommandName(),
                testCommand);

    }
}
