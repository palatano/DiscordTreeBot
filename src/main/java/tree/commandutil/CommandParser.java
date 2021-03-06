package tree.commandutil;

/**
 * Created by Admin on 7/29/2017.
 */
public class CommandParser {

    private static boolean checkValidToken(String msgContent) {
        return msgContent.startsWith(";") && msgContent.length() != 1 && msgContent.charAt(1) != ';';
    }

    private static String[] getArguments(String msgContent) {
        String[] arguments = msgContent.split(" ");
        if (arguments[0] != null && checkValidToken(msgContent)) {
            arguments[0] = arguments[0].replaceAll(";", "");
        } else {
            return null;
        }
        return arguments;
    }

    public static String[] parseMessage(String msgText) {
        return getArguments(msgText);
    }
}
