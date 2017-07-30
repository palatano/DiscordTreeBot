package command.etc;

import commandutil.type.AdminCommand;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

/**
 * Created by Admin on 7/29/2017.
 */
public class TestCommand extends AdminCommand {

    public TestCommand(String commandName) {
        super(commandName);
    }

    @Override
    public String help() {
        return "To use this command, all you have to do is type $help test";
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member) {
        message.addReaction("\uD83D\uDE02").queue();
    }


}
