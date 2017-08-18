package tree.command.text;

import javafx.scene.text.Text;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import tree.commandutil.CommandManager;
import tree.commandutil.type.Command;
import tree.commandutil.type.TextCommand;
import tree.commandutil.util.CommandRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Admin on 7/31/2017.
 */
public class CommandsCommand implements TextCommand {
    private String commandName;


    public CommandsCommand(String commmandName) {
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
        StringBuilder sb = new StringBuilder();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Command list for TreeBot (by palat):");
        embed.setDescription("Type \";\" before the commands to use them.");
        Set<Map.Entry<String, Command>> commandEntries = CommandRegistry.commandListSet();
        for (Map.Entry commandEntry : commandEntries) {
            String commandName = (String) commandEntry.getKey();
            Command command = (Command) commandEntry.getValue();
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
        member.getUser()
                .openPrivateChannel()
                .queue(channel -> channel.sendMessage(helpMessage).queue());
    }

    @Override
    public String help() {
        return "Just type " +
                CommandManager.botToken +
                getCommandName() +
                " to get the command list sent to you via PM.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
