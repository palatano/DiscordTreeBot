package commandutil.type;

import command.etc.TestCommand;

/**
 * Created by Valued Customer on 7/31/2017.
 */
public class PictureCommandFactory extends AbstractCommandFactory {

    @Override
    public PictureCommand getPictureCommand(String pictureType) {
        if (pictureType.equals("nug")) {
            return null; // TODO: this.
        }
        return null;
    }

    @Override
    public AnalysisCommand getAnalysisCommand(String analysisType) {
        throw new UnsupportedClassVersionError();
    }

    @Override
    public TextCommand getTextCommand(String textType) {
        throw new UnsupportedClassVersionError();
    }
}
