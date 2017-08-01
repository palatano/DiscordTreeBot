package tree.commandutil.util;

import tree.commandutil.type.AbstractCommandFactory;
import tree.commandutil.type.Command;
import tree.commandutil.type.FactoryProducer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Admin on 7/29/2017.
 */
public class CommandInit {
    private static final String[] textCommands = {"test", "commands"};
    private static final String[] pictureCommands = {"nugget"};
    private static final String[] analysisCommands = {"joindate", "uniqueusers"};

    public static void addCommands(Map<String, Command> commandList) {
        // Add all of the text commands first.
        AbstractCommandFactory abstractFactory = FactoryProducer.getFactory("TEXT");
        for (String textCommand : textCommands) {
            Command currCommand = abstractFactory.getTextCommand(textCommand);
            commandList.put(currCommand.getCommandName(), currCommand);
        }

        // Next, add the picture commands.
        abstractFactory = FactoryProducer.getFactory("PICTURE");
        for (String pictureCommand : pictureCommands) {
            Command currCommand = abstractFactory.getPictureCommand(pictureCommand);
            commandList.put(currCommand.getCommandName(), currCommand);
        }
        // Next, add the analysis commands.
        abstractFactory = FactoryProducer.getFactory("ANALYSIS");
        for (String pictureCommand : analysisCommands) {
            Command currCommand = abstractFactory.getAnalysisCommand(pictureCommand);
            commandList.put(currCommand.getCommandName(), currCommand);
        }

    }
}
