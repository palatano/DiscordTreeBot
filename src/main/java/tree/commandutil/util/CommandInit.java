package tree.commandutil.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.TreeMain;
import tree.command.util.music.AudioPlayerAdapter;
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
    private static final String[] textCommands = {"test", "help", "bot"};
    private static final String[] pictureCommands = {"nugget"};
    private static final String[] analysisCommands = {"info", "uniqueusers", "search",
            "ping", "youtube", "set", "unset", "guildpermissions", "shutdown"};
    private static final String[] voiceCommands = {"voicesearch", "echo"};
    private static final String[] musicCommands = {"add", "skip", "pause", "list", "unpause",
            "leave", "join", "musichelp", "cnl", "req", "undo", "np"};

    private static Logger log = LoggerFactory.getLogger(CommandInit.class);

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
        for (String analysisCommand : analysisCommands) {
            Command currCommand = abstractFactory.getAnalysisCommand(analysisCommand);
            commandList.put(currCommand.getCommandName(), currCommand);
        }

        // Next, add the voice commands.
        abstractFactory = FactoryProducer.getFactory("VOICE");
        for (String voiceCommand : voiceCommands) {
            Command currCommand = abstractFactory.getVoiceCommand(voiceCommand);
            commandList.put(currCommand.getCommandName(), currCommand);
        }

        // Next, add the music commands.
        abstractFactory = FactoryProducer.getFactory("MUSIC");
        for (String musicCommand : musicCommands) {
            Command currCommand = abstractFactory.getMusicCommand(musicCommand);
            commandList.put(currCommand.getCommandName(), currCommand);
        }

        log.info("All commands successfully initialized.");
    }
}
