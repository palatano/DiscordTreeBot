package tree.command.music;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.commandutil.type.Command;
import tree.commandutil.type.MusicCommand;
import tree.commandutil.util.CommandRegistry;

/**
 * Created by Valued Customer on 8/14/2017.
 */
public class CancelCommand implements MusicCommand {
    private String commandName;

    public CancelCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        // If exists, in the add or req command, cancel the menu,
        // remove it, and cancel the scheduler if it exists.
        AddCommand addCommand = (AddCommand) CommandRegistry.getCommand("add");
        RequestCommand reqCommand = (RequestCommand) CommandRegistry.getCommand("req");
        // See if a menu exists. If not return.
        if (addCommand.hasMenu()) {
            addCommand.cancelMenu(guild, msgChan);
        }
        if (reqCommand.hasMenu()) {
            reqCommand.cancelMenu(guild, msgChan);
        }

    }

    @Override
    public String help() {
        return "Cancels the current menu, if open.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
