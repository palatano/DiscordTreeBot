package tree.command.analysis;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;

import java.time.temporal.ChronoUnit;

/**
 * Created by Valued Customer on 8/3/2017.
 */
public class PingCommand implements AnalysisCommand {
    private String commandName;

    public PingCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member, String[] args) {
        msgChan.sendMessage("Getting ping...")
                .queue(msg -> msg.editMessage("Ping: " +
                        Long.toString(message.getCreationTime()
                        .until(msg.getCreationTime(),
                                ChronoUnit.MILLIS)) + "ms").queue());
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Returns the latency of the bot.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
