package tree.command.music;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.command.analysis.InfoCommand;
import tree.command.util.MenuUtil;
import tree.command.util.api.YoutubeMusicUtil;
import tree.commandutil.CommandManager;
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
        long userId = member.getUser().getIdLong();
        // See if a menu exists. If not return.
        if (addCommand.hasMenu(guild, userId) &&
                addCommand.inSameChannel(guild, userId, msgChan)) {
            addCommand.reset(guild, userId);
//            menuUtil.deleteMenu(msgChan, addCommand.getCommandName());
        } else if (reqCommand.hasMenu(guild, userId) &&
                reqCommand.inSameChannel(guild, userId, msgChan)) {
            reqCommand.reset(guild, userId);
//            menuUtil.deleteMenu(msgChan, reqCommand.getCommandName());
        } else if (infoCommand.waitingForChoice()) {
            infoCommand.reset(msgChan);
            menuUtil.deleteMenu(msgChan, infoCommand.getCommandName());
        } else {
            message.addReaction("\u274E").queue();
        }


    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Cancels the current menu, if open.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
