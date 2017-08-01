package tree.commandutil.type;

import tree.command.analysis.JoinDateCommand;
import tree.command.analysis.UniqueUserCommand;

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
            default:
                return null;
        }
    }

    @Override
    public TextCommand getTextCommand(String textType) {
        throw new UnsupportedClassVersionError();
    }

}