package commandutil.type;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

/**
 * Created by Admin on 7/29/2017.
 */
public interface Command {

    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member);

    public String getCommandName();

    public String help();
}
