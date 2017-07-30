package commandutil;

import commandutil.type.Command;
import net.dv8tion.jda.core.entities.Message;

/**
 * Created by Admin on 7/29/2017.
 */
public class CommandParser {

    private static String[] getArguments(String msgContent) {
        String[] arguments = msgContent.split(" ");
        if (arguments[0] != null) {
            arguments[0] = arguments[0].replaceAll("&", "");
        } else {
            System.out.println("Something went wrong with parsing" +
                    " the command in CommandParser. Error.");
        }
        return arguments;
    }

    public static String[] parseMessage(String msgText) {
        return getArguments(msgText);
    }
}
