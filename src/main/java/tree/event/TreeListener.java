package tree.event;

import net.dv8tion.jda.core.events.guild.GenericGuildEvent;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import tree.Config;
import tree.commandutil.CommandManager;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created by Admin on 7/29/2017.
 */
public class TreeListener extends ListenerAdapter {
    private static final long[] TESTING_CHANNELS = {314495018079617025L, 345931676746121216L, 337641574249005065L};
    private static final long[] TREES_CHANNELS = {249791455592316930L, 269577202016845824L, 346493255896268802L};

    public void onMessageReceived(MessageReceivedEvent event) {
        if (event == null || event.getChannel() == null) {
            return;
        }
        // If testing only, only allow commands in testing server.
        if (Config.CONFIG.isTesting()) {
            if (!testingOnly(event.getTextChannel())) {
                return;
            }
        } else {
            if (event.getGuild() == null || !inTreesChannel(event)) {
                return;
            }
        }
        Message msg = event.getMessage();
        if (!msg.getContent().startsWith(";")) {
            return;
        }
        CommandManager.messageCommand(msg);
    }

    private boolean testingOnly(MessageChannel msgChan) {
        for (long id : TESTING_CHANNELS) {
            if (msgChan.getIdLong() == id) {
                return true;
            }
        }
        return false;
    }

    private boolean inTreesChannel(MessageReceivedEvent event) {
        if (event.getGuild().getName().equals("/r/trees")) {
            for (long id : TREES_CHANNELS) {
                if (event.getTextChannel().getIdLong() == id) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean inBotChannel(MessageReceivedEvent event) {
        if (event.getGuild().getName().equals("/r/trees")) {
            return event.getTextChannel()
                    .getId()
                    .equals("249791455592316930");
        }
        return true;
    }

    private boolean inMusicChannel(MessageReceivedEvent event) {
        if (event.getGuild().getName().equals("/r/trees")) {
            return event.getTextChannel()
                    .getId()
                    .equals("269577202016845824");
        }
        return true;
    }

    private boolean inMusicBetaChannel(MessageReceivedEvent event) {
        if (event.getGuild().getName().equals("/r/trees")) {
            return event.getTextChannel()
                    .getId()
                    .equals("346493255896268802");
        }
        return true;
    }

}
