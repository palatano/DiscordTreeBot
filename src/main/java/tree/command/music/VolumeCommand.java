package tree.command.music;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import tree.Config;
import tree.command.util.MessageUtil;
import tree.commandutil.type.MusicCommand;

import java.io.File;

public class VolumeCommand implements MusicCommand{
    private String commandName;

    public VolumeCommand(String commandName) {
        this.commandName = commandName;
    }

    private String getOSPath() {
        String osName = Config.getOsName();
        if (osName.indexOf("win") >= 0) {
            return Config.CONFIG.getFilePath() + "discord-dau\\etc\\img\\";
        } else if (osName.indexOf("nux") >= 0){
            return Config.CONFIG.getFilePath() + "discord-dau/etc/img/";
        } else {
            return null;
        }
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.append("Attention: The volume may sound very loud. To change this, right click " +
                "the bot, and then set the volume using the scroll bar on the menu. " +
                "\n\nThe volume is kept at this level to provide smoother audio output for the bot.");
        File file = new File(getOSPath() + "volume.PNG");
        System.out.println(getOSPath() + "volume.PNG");
        msgChan.sendFile(file, messageBuilder.build()).queue();
    }

    @Override
    public String help() {
        return "Execute the command to learn about how the volume can be adjusted.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
