package tree.event;

import tree.commandutil.CommandManager;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created by Admin on 7/29/2017.
 */
public class TreeListener extends ListenerAdapter {

    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getTextChannel() == null) {
            return;
        }
//        if (!testingOnly(event.getTextChannel())) {
//            return;
//        }
        if (!inBotChannel(event)) {
            return;
        }
        Message msg = event.getMessage();
        if (!msg.getContent().startsWith("&")) {
            return;
        }
        // Get tree.event text, and then transform it to tree.command to be send to tree.command manager.
        CommandManager.messageCommand(msg);
    }

    private boolean testingOnly(MessageChannel msgChan) {
        return msgChan.getId().equals("337641574249005065");
    }

    private boolean inBotChannel(MessageReceivedEvent event) {
        if (event.getGuild().getName().equals("/r/trees")) {
            return event.getTextChannel()
                    .getId()
                    .equals("249791455592316930");
        }
        return true;
    }

}
