package commandutil.type;

/**
 * Created by Valued Customer on 7/31/2017.
 */
public class FactoryProducer {

    public static AbstractCommandFactory getFactory(String option) {
        if (option.equals("TEXT")) {
            return new TextCommandFactory();
        } else if (option.equals("PICTURE")) {
            return new PictureCommandFactory();
        } else if (option.equals("ANALYSIS")) {
            return new AnalysisCommandFactory();
        }
        throw new UnsupportedClassVersionError();
    }
}
