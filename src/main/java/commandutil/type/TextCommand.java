package commandutil.type;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

/**
 * Created by Valued Customer on 7/31/2017.
 */
public interface TextCommand extends Command {

    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member);

    public String help(MessageChannel msgChan);

    public String getCommandName();
}
