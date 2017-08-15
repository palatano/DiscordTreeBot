package tree.commandutil.type;

import tree.command.analysis.*;

/**
 * Created by Valued Customer on 7/31/2017.
 */
public class AnalysisCommandFactory extends AbstractCommandFactory {

    @Override
    public PictureCommand getPictureCommand(String pictureType) {
        throw new UnsupportedClassVersionError();
    }

    @Override
    public AnalysisCommand getAnalysisCommand(String analysisType) {
        switch (analysisType) {
            case "joindate":
                return new JoinDateCommand("joindate");
            case "uniqueusers":
                return new UniqueUserCommand("uniqueusers");
            case "search":
                return new GoogleSearchCommand("search");
            case "ping":
                return new PingCommand("ping");
            case "youtube":
                return new YoutubeCommand("youtube");
            default:
                return null;
        }
    }

    @Override
    public TextCommand getTextCommand(String textType) {
        throw new UnsupportedClassVersionError();
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
