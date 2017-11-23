package tree.command.analysis;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import tree.command.util.DataUtil;
import tree.command.util.MessageUtil;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.type.AnalysisCommand;

import java.util.List;

public class MusicStatsCommand implements AnalysisCommand {
    private String commandName;

    public MusicStatsCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        // We want to get the data from all guilds.
        JDA jda = guild.getJDA();
        List<Guild> guilds = guild.getJDA().getGuilds();
        String guildMusicData = "";

        // Here's the information we track globally.
        int globalNumConnections = 0;
        int globalNumMusicUsers = 0;
        int globalNumMusicSongs = 0;
        long globalLongestPlaylist = 0;
        String longestTimeStamp = "Empty";

        // Let's analyze the voice channel that the bot is connected to.
        for (Guild currGuild : guilds) {
            Member selfMember = currGuild.getSelfMember();
            VoiceChannel voiceChannel = selfMember.getVoiceState().getChannel();
            if (voiceChannel == null) {
                continue;
            }
            globalNumConnections++;

            // What we want to keep track of.
            int numMusicUsers = 0;
            int numSongs = 0;
            String playlistTimestamp = "";
            String musicUserNames = "";
            guildMusicData += "--- Information for " + currGuild.getName() + " ---\n";

            // Check each voice state.
            List<GuildVoiceState> voiceStates = currGuild.getVoiceStates();
            for (GuildVoiceState state : voiceStates) {
                if (voiceChannel.equals(state.getChannel()) &&
                        !state.getMember().equals(selfMember)) {
                    numMusicUsers++;
                    globalNumMusicUsers++;
                    musicUserNames += state.getMember().getEffectiveName() + ", ";
                }
            }

            // Check out the playlist data.
            AudioPlayerAdapter player = AudioPlayerAdapter.audioPlayerAdapter;
            GuildMusicManager musicManager = player.getGuildAudioPlayer(currGuild);
            numSongs = musicManager.scheduler.getNumberOfTracks();
            globalNumMusicSongs++;
            playlistTimestamp = musicManager.scheduler.getTimestampOfPlaylist();
            musicUserNames = MessageUtil.removeLastComma(musicUserNames);

            long playlistLength = musicManager.scheduler.getLengthOfPlaylist();
            globalLongestPlaylist = Math.max(globalLongestPlaylist, playlistLength);
            if (globalLongestPlaylist == playlistLength) {
                longestTimeStamp = playlistTimestamp;
            }

            guildMusicData += "Number of music users: " + numMusicUsers + "\n"
                    + "Music user names: " + musicUserNames + "\n"
                    + "Number of songs: " + numSongs + " \n"
                    + "Length of playlist: " + playlistTimestamp + "\n";

        }
        DataUtil dataUtil = DataUtil.getInstance();
        dataUtil.writeGuildDataToFile("music_data", guildMusicData);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Global Music Stats for TreeBot");
        embed.addField("Number of Music Connections",
                String.valueOf(globalNumConnections), true);
        embed.addField("Number of Current Music Users   ",
                String.valueOf(globalNumMusicUsers), true);
        embed.addField("Number of Songs Queued", String.valueOf(globalNumMusicSongs), true);
        embed.addField("Longest Playlist", longestTimeStamp, true);
        msgChan.sendMessage(embed.build()).queue();
    }

    @Override
    public String help() {
        return "Gives the music stats for the bot.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
