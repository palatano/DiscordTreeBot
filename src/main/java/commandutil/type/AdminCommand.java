package commandutil.type;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

/**
 * Created by Admin on 7/29/2017.
 */
public class AdminCommand implements Command {
    private String commandName;

    public AdminCommand(String commandName) {
        this.commandName = commandName;
    }

    public String help() {
        return null;
    }

    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member) {

    }

    public String getCommandName() {
        return commandName;
    }
}
