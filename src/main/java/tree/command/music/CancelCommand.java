package tree.command.music;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.command.analysis.InfoCommand;
import tree.command.util.MenuUtil;
import tree.command.util.api.YoutubeMusicUtil;
import tree.commandutil.type.Command;
import tree.commandutil.type.MusicCommand;
import tree.commandutil.util.CommandRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Valued Customer on 8/14/2017.
 */
public class CancelCommand implements MusicCommand {
    private String commandName;
    private YoutubeMusicUtil ytUtil;

    public CancelCommand(String commandName) {
        this.commandName = commandName;
        ytUtil = YoutubeMusicUtil.getInstance();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        // If exists, in the add or req command, cancel the menu,
        // remove it, and cancel the scheduler if it exists.
        AddCommand addCommand = (AddCommand) CommandRegistry.getCommand("add");
        RequestCommand reqCommand = (RequestCommand) CommandRegistry.getCommand("req");
        InfoCommand infoCommand = (InfoCommand) CommandRegistry.getCommand("info");
        MenuUtil menuUtil = ytUtil.getMenuUtil();
        // See if a menu exists. If not return.
        if (addCommand.hasMenu() && menuUtil.inSameMessageChannel(msgChan, "add")) {
            addCommand.reset(msgChan);
            menuUtil.deleteMenu(msgChan, addCommand.getCommandName());
        } else if (reqCommand.hasMenu() && menuUtil.inSameMessageChannel(msgChan, "req")) {
            reqCommand.reset(msgChan);
            menuUtil.deleteMenu(msgChan, reqCommand.getCommandName());
        } else if (infoCommand.waitingForChoice() && menuUtil.inSameMessageChannel(msgChan, "info")) {
            infoCommand.reset(msgChan);
            menuUtil.deleteMenu(msgChan, infoCommand.getCommandName());
        } else {
            message.addReaction("\u274E").queue();
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
