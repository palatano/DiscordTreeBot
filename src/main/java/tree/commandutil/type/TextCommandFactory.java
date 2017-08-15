package tree.commandutil.type;

import tree.command.text.CommandsCommand;
import tree.command.text.TestCommand;
import tree.command.voice.VoiceSearchCommand;

/**
 * Created by Valued Customer on 7/31/2017.
 */
public class TextCommandFactory extends AbstractCommandFactory {

    @Override
    public PictureCommand getPictureCommand(String pictureType) {
        throw new UnsupportedClassVersionError();
    }

    @Override
    public AnalysisCommand getAnalysisCommand(String analysisType) {
        throw new UnsupportedClassVersionError();
    }

    @Override
    public TextCommand getTextCommand(String textType) {
        switch (textType) {
            case "test":
                return new TestCommand("test");
            case "commands":
                return new CommandsCommand("commands");
            default:
                return null;
        }
    }

    @Override
    public VoiceCommand getVoiceCommand(String voiceType) {
        throw new UnsupportedClassVersionError();
    }

    @Override
    public MusicCommand getMusicCommand(String musicType) {
        throw new UnsupportedClassVersionError();
    }
}
