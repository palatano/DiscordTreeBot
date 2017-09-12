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
public class SkipCommand implements MusicCommand {
    private String commandName;
    private AudioPlayerAdapter audioPlayerAdapter;
    private static Logger logger = LoggerFactory.getLogger(AddCommand.class);

    public SkipCommand(String commandName) {
        this.commandName = commandName;
        audioPlayerAdapter = AudioPlayerAdapter.audioPlayerAdapter;
    }

    private void skipSong(Guild guild, MessageChannel msgChan, Message message, Member member) {
        audioPlayerAdapter.skipTrack(message.getTextChannel());
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length != 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
        }
        skipSong(guild, msgChan, message, member);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Skip the current song.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
