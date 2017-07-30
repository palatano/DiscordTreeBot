package commandutil.type;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

/**
 * Created by Admin on 7/29/2017.
 */
public class UserCommand implements Command {
    private String commandName;

    public UserCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public String help() {
        return null;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member) {

    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
