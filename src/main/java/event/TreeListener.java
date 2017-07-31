package event;

import commandutil.CommandManager;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created by Admin on 7/29/2017.
 */
public class TreeListener extends ListenerAdapter {

    private boolean isMainUser(MessageReceivedEvent event) {
        String name = event.getAuthor().getId();
        return name.equals("192372494202568706");
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (!isMainUser(event)) {
            return;
        }
        Message msg = event.getMessage();
        // Get event text, and then transform it to command to be send to command manager.
        CommandManager.messageCommand(msg);
    }

}
