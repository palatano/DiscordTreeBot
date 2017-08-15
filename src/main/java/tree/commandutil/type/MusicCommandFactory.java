package tree.commandutil.type;

import tree.command.music.*;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class MusicCommandFactory extends AbstractCommandFactory {

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
        throw new UnsupportedClassVersionError();
    }

    @Override
    public MusicCommand getMusicCommand(String musicType) {
        switch (musicType) {
            case "add":
                return new AddCommand("add");
            case "skip":
                return new SkipCommand("skip");
            case "pause":
                return new PauseCommand("pause");
            case "list":
                return new PlaylistCommand("list");
            case "unpause":
                return new UnpauseCommand("unpause");
            case "leave":
                return new LeaveCommand("leave");
            case "join":
                return new JoinCommand("join");
            case "musicCommands":
                return new MusicCommandsCommand("musicCommands");
            case "cnl":
                return new CancelCommand("cnl");
            case "req":
                return new RequestCommand("req");
            case "undo":
                return new UndoCommand("undo");
            default:
                return null;
        }
    }
}
