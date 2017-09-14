package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.commandutil.CommandManager;
import tree.commandutil.type.MusicCommand;
import tree.util.LoggerUtil;

import java.util.List;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class PauseCommand implements MusicCommand {
    private String commandName;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);

    public PauseCommand(String commandName) {
        this.commandName = commandName;
    }

    private void stop(Guild guild, MessageChannel msgChan, Message message, Member member) {
        GuildMusicManager musicManager = AudioPlayerAdapter.audioPlayerAdapter
                .getGuildAudioPlayer(guild);
        msgChan.sendMessage("Song has now been paused. Type ``" +
                CommandManager.botToken +
                "unpause" +
                "`` to continue.").queue();
        musicManager.player.setPaused(true);
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length != 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
        }
        stop(guild, msgChan, message, member);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Pause the song.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
