package tree.command.music;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import tree.commandutil.CommandManager;
import tree.commandutil.type.Command;
import tree.commandutil.type.MusicCommand;
import tree.commandutil.util.CommandRegistry;

import java.util.Map;
import java.util.Set;

/**
 * Created by Valued Customer on 8/13/2017.
 */
public class MusicCommandsCommand implements MusicCommand {
    private static final String[] musicCommands = {"add", "skip", "pause", "list", "unpause",
            "leave", "join", "musicCommands", "cnl", "req", "undo"};
    private String commandName;

    public MusicCommandsCommand(String commmandName) {
        this.commandName = commmandName;
    }

    private boolean isAdminCommand(String commandName) {
        for (String com : CommandManager.adminCommands) {
            if (com.equals(commandName)) {
                return true;
            }
        }
        return false;
    }

    private MessageEmbed getCommandsHelp() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Music commands for TreeBot (by palat):");
        embed.setDescription("Type \";\" before the commands to use them.");
        for (String musicCommandName : musicCommands) {
            Command command = CommandRegistry.getCommand(musicCommandName);
            if (isAdminCommand(commandName)) {
                continue;
            }
            embed.addField(command.getCommandName(), command.help(), true);
        }
        return embed.build();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        MessageEmbed helpMessage = getCommandsHelp();
        msgChan.sendMessage(helpMessage).queue();
    }

    @Override
    public String help() {
        return "Music command list.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

}
