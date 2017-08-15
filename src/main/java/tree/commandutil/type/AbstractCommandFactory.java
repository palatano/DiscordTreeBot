package tree.commandutil.type;

/**
 * Created by Valued Customer on 7/31/2017.
 */
public abstract class AbstractCommandFactory {

    public abstract TextCommand getTextCommand(String textType);

    public abstract PictureCommand getPictureCommand(String pictureType);

    public abstract AnalysisCommand getAnalysisCommand(String analysisType);

    public abstract VoiceCommand getVoiceCommand(String voiceType);

    public abstract MusicCommand getMusicCommand(String musicType);
}
