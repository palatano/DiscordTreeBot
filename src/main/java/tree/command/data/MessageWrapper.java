package tree.command.data;

import net.dv8tion.jda.core.entities.Message;

/**
 * Created by Valued Customer on 9/11/2017.
 */
public class MessageWrapper {
    private Message message;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
