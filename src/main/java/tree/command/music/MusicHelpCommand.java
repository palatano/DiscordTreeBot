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
public class MusicHelpCommand implements MusicCommand {
    private static final String[] musicCommands = {"add", "skip", "pause", "list", "unpause",
            "leave", "join", "cnl", "req", "undo", "np"};
    private static final String[] basicMusicCommands = {"add", "req", "np", "skip"};
    private String commandName;

    public MusicHelpCommand(String commmandName) {
        this.commandName = commmandName;
    }

    private boolean isBasicCommand(String command) {
        for (String basicCommand : basicMusicCommands) {
            if (command.equals(basicCommand)) {
                return true;
            }
        }
        return false;
    }

    private MessageEmbed getCommandsHelp() {
        EmbedBuilder embed = new EmbedBuilder();
        String description = "";
        embed.setTitle("Music Commands for TreeBot:\n");
        String basicCommandsList = "**Basic commands:**\n";
        String advancedCommandList = "**Advanced commands:**\n";

        for (String musicCommand : CommandRegistry.musicCommands) {
            Command command = CommandRegistry.getCommand(musicCommand);
            if (isBasicCommand(command.getCommandName())) {
                basicCommandsList += CommandManager.bulletToken + command.help() + "\n";
            } else {
                advancedCommandList += CommandManager.bulletToken + command.help() + "\n";
            }
        }

        description = basicCommandsList + "\n" + advancedCommandList;

        embed.setDescription(description);
        return embed.build();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        MessageEmbed helpMessage = getCommandsHelp();
        msgChan.sendMessage(helpMessage).queue();
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Music command list.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

}
