package tree.commandutil.type;

import tree.command.text.CommandsCommand;
import tree.command.text.TestCommand;
import tree.command.voice.EchoCommand;
import tree.command.voice.VoiceSearchCommand;

/**
 * Created by Valued Customer on 8/4/2017.
 */
public class VoiceCommandFactory extends AbstractCommandFactory {

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
        throw new UnsupportedClassVersionError();
    }

    @Override
    public VoiceCommand getVoiceCommand(String voiceType) {
        switch (voiceType) {
            case "voicesearch":
                return new VoiceSearchCommand("voicesearch");
            case "echo":
                return new EchoCommand("echo");
            default:
                return null;
        }
    }

    @Override
    public MusicCommand getMusicCommand(String musicType) {
        throw new UnsupportedClassVersionError();
    }
}
