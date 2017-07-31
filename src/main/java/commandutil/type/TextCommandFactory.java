package commandutil.type;

import command.etc.TestCommand;

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
        if (textType == null) {
            return null;
        }

        if (textType.equals("test")) {
            return new TestCommand("test");
        }

        return null;
    }
}
