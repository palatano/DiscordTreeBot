package tree.command.analysis;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.commandutil.type.AnalysisCommand;

/**
 * Created by Valued Customer on 9/10/2017.
 */
public class ShutdownCommand implements AnalysisCommand {
    private String commandName;

    public ShutdownCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        guild.getJDA().shutdownNow();
    }

    @Override
    public String help() {
        return "Shutdown the bot.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
