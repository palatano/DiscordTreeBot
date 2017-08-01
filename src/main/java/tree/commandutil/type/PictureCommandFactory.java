package tree.commandutil.type;

import tree.command.picture.NuggetCommand;

/**
 * Created by Valued Customer on 7/31/2017.
 */
public class PictureCommandFactory extends AbstractCommandFactory {

    @Override
    public PictureCommand getPictureCommand(String pictureType) {
        switch (pictureType) {
            case "nugget":
                return new NuggetCommand("nugget");
            default:
                return null;
        }
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
