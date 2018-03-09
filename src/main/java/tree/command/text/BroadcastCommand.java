package tree.command.text;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import tree.commandutil.type.TextCommand;

import java.util.List;

/**
 * Created by Valued Customer on 9/22/2017.
 */
public class BroadcastCommand implements TextCommand {
    private String commandName;
    private static final long[] blacklistedChannels = {110373943822540800L, 125227483518861312L};

    public BroadcastCommand(String commandName) {
        this.commandName = commandName;
    }

    private String getMessage(String[] args) {
        String out = "";
        for (int i = 1; i < args.length; i++) {
            out += args[i] + " ";
        }
        return out.trim();
    }

    private boolean isBlacklisted(long id) {
        for (long channelId : blacklistedChannels) {
            if (id == channelId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        String msgToSend = getMessage(args);

        JDA jda = guild.getJDA();
        List<Guild> guilds = jda.getGuilds();
        for (Guild g : guilds) {
            if (isBlacklisted(g.getIdLong())) {
                continue;
            }

            List<TextChannel> channels = g.getTextChannels();
            for (TextChannel channel : channels) {
                if (channel.canTalk()) {
                    channel.sendMessage(msgToSend).queue();
                    break;
                }
            }
        }
    }

    @Override
    public String help() {
        return "Broadcasts a message to all guilds for update and maintenance issues.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
