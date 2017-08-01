package tree.commandutil.type;

/**
 * Created by Valued Customer on 7/31/2017.
 */
public class FactoryProducer {

    public static AbstractCommandFactory getFactory(String option) {
        switch (option) {
            case "TEXT":
                return new TextCommandFactory();
            case "PICTURE":
                return new PictureCommandFactory();
            case "ANALYSIS":
                return new AnalysisCommandFactory();
            default:
                return null;
        }
    }
}
