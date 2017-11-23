package tree.command.voice;

import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import tree.command.util.MessageUtil;
import tree.command.util.speech.AudioEchoHandler;
import tree.commandutil.CommandManager;
import tree.commandutil.type.VoiceCommand;
import tree.db.DatabaseManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Valued Customer on 8/19/2017.
 */
public class EchoCommand implements VoiceCommand {
    private String commandName;
    private AudioEchoHandler handler;
    private Map<Guild, EchoData> guildEchoDataMap;
    private DatabaseManager db;

    class EchoData {
        private ScheduledExecutorService scheduler;

        EchoData() {
            scheduler = Executors.newScheduledThreadPool(1);
        }

        public ScheduledExecutorService getScheduler() {
            return scheduler;
        }
    }

    public EchoCommand(String commandName) {
        this.commandName = commandName;
        handler = new AudioEchoHandler();
        guildEchoDataMap = new HashMap<>();
        db = DatabaseManager.getInstance();
    }

    private void addEchoData(Guild guild, AudioManager audioManager) {
        EchoData echoData = new EchoData();
        guildEchoDataMap.put(guild, echoData);

        AudioReceiveHandler lastAh = audioManager.getReceiveHandler();
        AudioSendHandler lastSh = audioManager.getSendingHandler();
        audioManager.setReceivingHandler(handler);
        audioManager.setSendingHandler(handler);

        ScheduledExecutorService scheduler = echoData.getScheduler();
        scheduler.schedule(() -> {
            audioManager.setReceivingHandler(lastAh);
            audioManager.setSendingHandler(lastSh);
            guildEchoDataMap.remove(guild);
            scheduler.shutdownNow();
        }, 15, TimeUnit.SECONDS);
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (guildEchoDataMap.containsKey(guild)) {
            MessageUtil.sendError("Bot is already echoing in the guild.", msgChan);
            return;
        }

        if (!member.getVoiceState().inVoiceChannel()) {
            MessageUtil.sendError("You must be in a voice channel.", msgChan);
            return;
        }

        AudioManager audioManager = guild.getAudioManager();
        VoiceChannel voiceChan = member.getVoiceState().getChannel();
        if (!db.isAllowedVoiceChannel(guild, voiceChan)) {
            MessageUtil.sendError("I'm not allowed to join that channel!", msgChan);
            return;
        }
        audioManager.openAudioConnection(member.getVoiceState().getChannel());

        msgChan.sendMessage("Speak into the microphone to hear the echo.").queue();
        addEchoData(guild, audioManager);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Echoes the audio from the channel.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
