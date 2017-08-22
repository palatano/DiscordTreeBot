package tree.command.voice;

//import com.darkprograms.speech.microphone.Microphone;
//import com.darkprograms.speech.recognizer.GoogleResponse;
//import com.darkprograms.speech.recognizer.Recognizer;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.CombinedAudio;
import net.dv8tion.jda.core.audio.UserAudio;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.managers.impl.AudioManagerImpl;
import tree.command.util.MessageUtil;
import tree.command.util.speech.AudioReceiveListener;
import tree.command.util.speech.AudioSendHandler;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;
import tree.commandutil.type.VoiceCommand;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class VoiceSearchCommand implements VoiceCommand {
    private String commandName;
    private StreamSpeechRecognizer recognizer;
    private AudioReceiveListener handler;


    public VoiceSearchCommand(String commandName) {
        this.commandName = commandName;
        handler = new AudioReceiveListener(1.0);

        Configuration configuration = new Configuration();
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        try {
            recognizer = new StreamSpeechRecognizer(configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        search(guild, msgChan, member);

    }

    @Override
    public String help() {
        return "Type " +
                CommandManager.botToken +
                getCommandName() +
                " to say something and get results from google.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    private void search(Guild guild, MessageChannel msgChan, Member member) {
        if (!guild.getAudioManager().isConnected()) {
            MessageUtil.sendError("Bot is not connected to a voice channel.", msgChan);
            return;
        }
        AudioManager audioManager = guild.getAudioManager();
        AudioReceiveHandler lastAh = audioManager.getReceiveHandler();
//        net.dv8tion.jda.core.audio.AudioSendHandler lastSh = audioManager.getSendingHandler();
        audioManager.setReceivingHandler(handler);
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        handler.reset();

        while (handler.uncompUserIndex < AudioReceiveListener.MAX_RECORD_TIME &&
                handler.uncompIndex < AudioReceiveListener.MAX_RECORD_TIME) {

        }
        try {
            if (handler.uncompIndex >= AudioReceiveListener.MAX_RECORD_TIME) {
                getResults(handler.uncompVoiceData);
            } else if (handler.uncompUserIndex >= AudioReceiveListener.MAX_RECORD_TIME) {
                getResults(handler.uncompUserVoiceData);
            } else {
                System.out.println("Shouldn't reach here.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        audioManager.setReceivingHandler(lastAh);
    }


    private void getResults(byte[] array) throws IOException {
        InputStream stream = new ByteArrayInputStream(array);

        recognizer.startRecognition(stream);
        SpeechResult result;
        while ((result = recognizer.getResult()) != null) {
            System.out.format("Hypothesis: %s\n", result.getHypothesis());
        }
        recognizer.stopRecognition();
    }

}