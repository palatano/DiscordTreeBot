package tree.command.text;

import javafx.scene.text.Text;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import tree.command.util.MessageUtil;
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
public class HelpCommand implements TextCommand {
    private String commandName;
    private static final String[] basicMusicCommands = {"add", "req", "np", "skip"};

    public HelpCommand(String commmandName) {
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
        embed.setTitle("Welcome to TreeBot. See the following options or type for more information.");
        embed.setDescription("``;bot`` - Displays links for website and support and other info.\n" +
                "``;help music`` - Music command list.\n" +
                "``;help picture`` - Pictures command list.\n" +
                "``;help analysis`` - Analysis and searching command list.\n" +
                "``;help voice`` - Voice command list.\n" +
                "``;help misc`` - Text command list.");
        return embed.build();
    }

    private boolean isBasicCommand(String command) {
        for (String basicCommand : basicMusicCommands) {
            if (command.equals(basicCommand)) {
                return true;
            }
        }
        return false;
    }

    private MessageEmbed getSpecificCommandList(Guild guild, MessageChannel msgChan,
                                                String type) {
        EmbedBuilder embed = new EmbedBuilder();
        String description = "";
        switch (type) {
            case "music":
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
                break;
            case "picture":
                embed.setTitle("Picture Commands for TreeBot:\n");

                for (String pictureCommand : CommandRegistry.pictureCommands) {
                    Command command = CommandRegistry.getCommand(pictureCommand);
                    description += CommandManager.bulletToken + command.help() + "\n";
                }

                embed.setDescription(description);
                break;
            case "analysis":
                embed.setTitle("Analysis Commands for TreeBot:\n");

                for (String analysisCommand : CommandRegistry.analysisCommands) {
                    Command command = CommandRegistry.getCommand(analysisCommand);
                    description += CommandManager.bulletToken + command.help() + "\n";
                }

                embed.setDescription(description);
                break;
            case "voice":
                embed.setTitle("Voice Commands for TreeBot:\n");

                for (String voiceCommand : CommandRegistry.voiceCommands) {
                    Command command = CommandRegistry.getCommand(voiceCommand);
                    description += CommandManager.bulletToken + command.help() + "\n";
                }

                embed.setDescription(description);
                break;
            case "misc":
                embed.setTitle("Miscellaneous Commands for TreeBot:\n");

                for (String miscCommand : CommandRegistry.textCommands) {
                    Command command = CommandRegistry.getCommand(miscCommand);
                    description += CommandManager.bulletToken + command.help() + "\n";
                }

                embed.setDescription(description);
                break;
            default:
                return null;
        }
        return embed.build();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        MessageEmbed helpMessage = null;
        if (args.length == 1) {
            helpMessage = getCommandsHelp();
            msgChan.sendMessage(helpMessage).queue();
            return;
        } else if (args.length == 2) {
            helpMessage = getSpecificCommandList(guild, msgChan, args[1]);
            if (helpMessage == null) {
                MessageUtil.sendError("Invalid entry. Valid entries are" +
                "``;help music``, ``;help picture``, ``;help analysis``, ``;help voice``, and ``;help misc``", msgChan);
                return;
            }
        } else {
            MessageUtil.sendError("Too many arguments.", msgChan);
            return;
        }

        final MessageEmbed send = helpMessage;
        member.getUser()
                .openPrivateChannel()
                .queue(channel -> channel.sendMessage(send).queue());
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Help menu.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
