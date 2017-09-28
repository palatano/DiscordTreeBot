package tree.command.voice;

import com.google.cloud.speech.v1.*;
import com.google.common.base.Stopwatch;
import com.google.protobuf.ByteString;
import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.MaryAudioUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tree.Config;
import tree.command.data.GoogleResults;
import tree.command.data.MessageWrapper;
import tree.command.util.MessageUtil;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.speech.AudioReceiveListener;
import tree.commandutil.CommandManager;
import tree.commandutil.type.VoiceCommand;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static sun.net.www.protocol.http.HttpURLConnection.userAgent;

public class VoiceSearchCommand implements VoiceCommand {
    private String commandName;
//    private StreamSpeechRecognizer recognizer;
    private AudioReceiveListener handler;
    private ScheduledExecutorService scheduler;
    private static MaryInterface marytts;
    private static SpeechClient speech;
    private static RecognitionConfig config;
    private Map<Guild, Message> guildMessageMap;

    public VoiceSearchCommand(String commandName) {
        this.commandName = commandName;
        handler = new AudioReceiveListener(1.0);
        guildMessageMap = new HashMap<>();

        scheduler = Executors.newScheduledThreadPool(3);
        try {
            marytts = new LocalMaryInterface();
        } catch (MaryConfigurationException e) {
            e.printStackTrace();
        }

        // Builds the sync recognize request
        config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(44100)
                .setLanguageCode("en-US")
                .build();

        try {
            speech = SpeechClient.create();
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
        return "``" + CommandManager.botToken + commandName + "``: get results from google.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    private byte[] writeToFile() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        File in = new File ("in.raw");

        for (byte[] data : handler.userQueue) {
            baos.write(data);
        }
        FileUtils.writeByteArrayToFile(in, baos.toByteArray());
        return baos.toByteArray();
    }

    private void search(Guild guild, MessageChannel msgChan, Member member) {
        if (!guild.getAudioManager().isConnected()) {
            MessageUtil.sendError("Bot is not connected to a voice channel.", msgChan);
            return;
        }
        AudioManager audioManager = guild.getAudioManager();

        AudioReceiveHandler lastAh = audioManager.getReceiveHandler();
        net.dv8tion.jda.core.audio.AudioSendHandler lastSh = audioManager.getSendingHandler();
        handler.reset();
        audioManager.setReceivingHandler(handler);

        Message message = msgChan.sendMessage("Please say your question, up to 5 seconds.").complete();
        guildMessageMap.put(guild, message);

        Runnable runnable = () -> {
            try {
                audioManager.setReceivingHandler(lastAh);
                writeToFile();
                getResults(guild, msgChan, member);
            } catch (IOException e) {
                e.printStackTrace();
            }

        };

        scheduler.schedule(runnable,5, TimeUnit.SECONDS);
    }


    private boolean checkIfStillConnected(Guild guild, Member member)  {
        AudioManager audioManager = guild.getAudioManager();
        VoiceState vs = member.getVoiceState();
        return audioManager.isConnected() &&
                vs.getAudioChannel().equals(audioManager.getConnectedChannel());

    }

    private void getResults(Guild guild, MessageChannel msgChan, Member member) throws IOException {
        String in = "in.raw";
        try {
            streamingRecognizeFile(guild, msgChan, member, in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void streamingRecognizeFile(Guild guild, MessageChannel msgChan, Member member, String rawFile) throws Exception, IOException {
        Stopwatch watch = Stopwatch.createUnstarted();
        watch.start();
        String outFileName = "final.wav";
        File file = new File(outFileName);
        file.delete();

        System.out.println("Starting command: " + watch.toString());
        String command = "ffmpeg -f s32be -ar 48000 -ac 1 -i " + rawFile + " -ar 44100 " + outFileName;

        Runtime.getRuntime().exec(command);
        System.out.println("Command executed: " + watch.toString());


        // The path to the audio file to transcribe

        Message message = guildMessageMap.get(guild);
        message = message.editMessage("Received your input. Transcribing...").complete();
        guildMessageMap.put(guild, message);
        while (!file.exists()) {
            Thread.sleep(50);
        }

        String fileName = "final.wav";
        // Reads the audio file into memory

        Path path = Paths.get(fileName);
        byte[] data = Files.readAllBytes(path);
        ByteString audioBytes = ByteString.copyFrom(data);
        System.out.println("Initialization complete: " + watch.toString());

        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioBytes)
                .build();

        // Performs speech recognition on the audio file
        // Instantiates a client]

        System.out.println("Recognizing: " + watch.toString());
        RecognizeResponse response = null;
        try {
            response = speech.recognize(config, audio);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Recognition complete: " + watch.toString());
        List<SpeechRecognitionResult> results = response.getResultsList();

        String speechToText = "";
        for (SpeechRecognitionResult result: results) {
            List<SpeechRecognitionAlternative> alternatives = result.getAlternativesList();
            for (SpeechRecognitionAlternative alternative: alternatives) {
                System.out.printf("Transcription: %s%n", alternative.getTranscript());
                speechToText += alternative.getTranscript() + " ";
            }
        }
        System.out.println("Finished printing: " + watch.toString());
        speech.close();

        getAudioOutputResult(guild, msgChan, member, speechToText);
        speech = SpeechClient.create();
    }

    private void getAudioOutputResult(Guild guild, MessageChannel msgChan, Member member, String search) {
        try {
            MessageEmbed embed = getSearchResult(search, msgChan, member);
            if (embed != null) {
                Message message = guildMessageMap.get(guild);
                message = message.editMessage(embed).complete();
                int descIndex = embed.getDescription().indexOf("\n");
                String desc = embed.getDescription();
                String output = desc.substring(descIndex + 1, desc.length());
                AudioInputStream audioOut = marytts.generateAudio(output);

                String fileName = "out.wav";
                MaryAudioUtils.writeWavFile(MaryAudioUtils.getSamplesAsDoubleArray(audioOut),
                        fileName,
                        audioOut.getFormat());

                // Now, get lavaplayer to play it.

                String filePath = Config.getFilePath() + "discord-dau\\" + fileName;
                AudioPlayerAdapter player = AudioPlayerAdapter.audioPlayerAdapter;
                if (checkIfStillConnected(guild, member)) {
                    player.loadLocalAudio(filePath, member);
                }
                Clip clip = AudioSystem.getClip();
                clip.open(audioOut);
                clip.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private  MessageEmbed getSearchResult(String search, MessageChannel msgChan, Member member) {
        Message message = guildMessageMap.get(member.getGuild());
        message = message.editMessage("You are searching for: *" + search.trim() + "*").complete();
        guildMessageMap.put(member.getGuild(), message);
        URI url = null;
        try {
            url = new URIBuilder("http://www.google.com/search").addParameter("q", search).build();
            Elements links = Jsoup.connect(url.toString())
                    .userAgent("Gnar")
                    .get()
                    .select(".g");
            if (links.isEmpty()) {
                MessageUtil.sendError("No search results found.", msgChan);
                return null;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor("Google Results", "https://www.google.com/", "https://www.google.com/favicon.ico");

            for (Element link : links) {
                Elements list = link.select(".r>a");
                if (list.isEmpty()) {
                    continue;
                }
                Element entry = list.first();
                String title = entry.text();
                String resultUrl = entry.absUrl("href").replace(")", "\\)");
                String description = null;
                Elements st = link.select(".st");
                if (!st.isEmpty()) {
                    description = st.first().text();
                    if (description.isEmpty()) {
                        continue;
                    }
                }
                embed.setDescription("**[" + title + "](" + resultUrl + ")**\n" + description);
                return embed.build();
            }

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}