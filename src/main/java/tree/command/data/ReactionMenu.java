package tree.command.data;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

/**
 * Created by Valued Customer on 9/11/2017.
 */
public class ReactionMenu {
    private String commandName;
    private long userId;
    private MessageChannel msgChan;
    boolean isClicked;

    public String getCommandName() {
        return commandName;
    }

    public long getUserId() {
        return userId;
    }

    public MessageChannel getMsgChan() {
        return msgChan;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setMsgChan(MessageChannel msgChan) {
        this.msgChan = msgChan;
    }

    public ReactionMenu(String commandName, long userId,
                        MessageChannel msgChan) {
        this.commandName = commandName;
        this.userId = userId;
        this.msgChan = msgChan;
    }

    public void clicked() {
        isClicked = true;
    }

    public boolean isClicked() {
        return isClicked;
    }
}
