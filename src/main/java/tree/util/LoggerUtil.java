package tree.util;

import net.dv8tion.jda.core.entities.Message;
import org.slf4j.Logger;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class LoggerUtil {

    public static void logMessage(Logger log, Message msg, String infoMsg) {
        log.info("(" + msg.getGuild().getName() + ", #" + msg.getTextChannel().getName() + ", " +
                msg.getMember().getEffectiveName() + "): " + infoMsg);
    }

    public static void logError(Exception e, Logger log, Message msg) {
        log.info("(" + msg.getGuild().getName() + ", #" + msg.getTextChannel().getName() + ", " +
                msg.getMember().getEffectiveName() + "): " + e.getMessage());
    }

}
