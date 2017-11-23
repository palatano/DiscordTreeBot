package tree.command.text;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import tree.Config;
import tree.command.util.DataUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.TextCommand;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Valued Customer on 9/6/2017.
 */
public class BotInfoCommand implements TextCommand {
    private DataUtil dataUtil = DataUtil.getInstance();
    private String commandName;
    private static final String botURL = "https://bots.discord.pw/bots/337627312830939136";
    private static final String serverInviteLink = "https://discord.gg/5pTVpcM";
    private static final String botInviteLink = "https://discordapp.com/oauth2/authorize?client_id=337627312830939136&scope=bot&permissions=0";

    public BotInfoCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        EmbedBuilder embed = new EmbedBuilder();
        JDA jda = guild.getJDA();

        String url = jda.getSelfUser().getAvatarUrl();
        embed.setThumbnail(url);
        embed.setTitle("Bot Information for TreeBot:");

        List<Guild> guildList = jda.getGuilds();
        int numGuilds = guildList.size();
        int numUsers = jda.getUsers().size();
        int numMusicUsers = 0;
        int numConnectedUsers = 0;

        String output = "";
        for (Guild g : guildList) {
            String musicUsers = "Music Users: ";
            List<VoiceChannel> voiceChannelList = g.getVoiceChannels();
            VoiceChannel currentBotVoiceChannel = g.getAudioManager().getConnectedChannel();

            List<GuildVoiceState> guildVoiceStateList = g.getVoiceStates();
            for (GuildVoiceState guildVoiceState : guildVoiceStateList) {

                if (currentBotVoiceChannel == null) {
                    break;
                }

                if (currentBotVoiceChannel.equals(guildVoiceState.getChannel())) {
                    numMusicUsers++;
                    musicUsers += guildVoiceState.getMember().getEffectiveName() + ", ";
                }

                if (voiceChannelList.contains(guildVoiceState.getChannel())) {
                    numConnectedUsers++;
                }

            }

            int index = musicUsers.lastIndexOf(",");
            if (index != -1) {
                musicUsers = new StringBuilder(musicUsers).replace(index, index + 2, "").toString();
            }
            output += "----- " + g.getName() + " -----\n";
            output += "Size of server: " + g.getMembers().size() + "\n";
            output += "Server Owner: " + g.getOwner().getEffectiveName() + "\n";
            output += "Server Creation Date: " + g.getOwner().getJoinDate() + "\n";
            output += musicUsers + "\n\n";
        }

        dataUtil.writeGuildDataToFile("guildDataFile", output);

        long millis = System.currentTimeMillis() - Config.startTime;
        int day = (int) TimeUnit.MILLISECONDS.toDays(millis);
        String runTime = day + "d " +
                (TimeUnit.MILLISECONDS.toHours(millis) - (day * 24)) + "h " +
                (TimeUnit.MILLISECONDS.toMinutes(millis) - (TimeUnit.MILLISECONDS.toHours(millis) * 60)) + "m " +
                (TimeUnit.MILLISECONDS.toSeconds(millis) - (TimeUnit.MILLISECONDS.toMinutes(millis) * 60)) + "s";

        embed.setDescription("Developed by palat. Type ;help for more info.");//, discordInvite);
        embed.addField("Bot Website",  "[Website](" + botURL + ")", true);
        embed.addField("TreeBot Server", "[Server Link](" + serverInviteLink + ")", true);
        embed.addField("TreeBot Invite Link", "[Invite Link](" + botInviteLink + ")", true);

        embed.addField("Number of Servers", String.valueOf(numGuilds), true);
        embed.addField("Number of Users", String.valueOf(numUsers), true);
        embed.addField("Current Music Users", String.valueOf(numMusicUsers), true);
        embed.addField("Connected Voice Users", String.valueOf(numConnectedUsers), true);
        embed.addField("Runtime", runTime, true);

        msgChan.sendMessage(embed.build()).queue();
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Gives the bot information.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}

