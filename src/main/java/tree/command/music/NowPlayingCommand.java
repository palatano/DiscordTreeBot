package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.CommandManager;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

/**
 * Created by Valued Customer on 8/24/2017.
 */
public class NowPlayingCommand implements MusicCommand {
    private String commandName;
    private Logger logger = LoggerFactory.getLogger(NowPlayingCommand.class);

    public NowPlayingCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        String commandWithToken = CommandManager.botToken + getCommandName();
        if (args.length != 1) {
            LoggerUtil.logMessage(logger, message, "No arguments allowed for this command.");
            message.addReaction("\u274E").queue();
            return;
        }
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayerAdapter.getGuildAudioPlayer(guild);
        if (!musicManager.scheduler.isEmpty() && musicManager.player.getPlayingTrack() != null) {
            showCurrentSong(musicManager, msgChan);
        } else {
            msgChan.sendMessage("No songs are currently playing.").queue();
        }
    }

    private void showCurrentSong(GuildMusicManager musicManager, MessageChannel msgChan) {
        MessageEmbed currentSong = musicManager.scheduler.showCurrentSong();
        msgChan.sendMessage(currentSong).queue();
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Displays the current song that is playing.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
