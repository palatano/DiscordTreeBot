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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.commons.io.FileUtils;
import tree.command.util.MessageUtil;
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

public class VoiceSearchCommand implements VoiceCommand {
    private String commandName;
//    private StreamSpeechRecognizer recognizer;
    private AudioReceiveListener handler;
    private ScheduledExecutorService scheduler;
    private static MaryInterface marytts;


    public VoiceSearchCommand(String commandName) {
        this.commandName = commandName;
        handler = new AudioReceiveListener(1.0);

        scheduler = Executors.newScheduledThreadPool(3);
//        Configuration configuration = new Configuration();
//        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
//        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
//        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        try {
            marytts = new LocalMaryInterface();
        } catch (MaryConfigurationException e) {
            e.printStackTrace();
        }



//        try {
//            recognizer = new StreamSpeechRecognizer(configuration);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }



    }


    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        search(guild, msgChan,member);
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
        Runnable runnable = () -> {
            audioManager.setReceivingHandler(lastAh);
            try {
                writeToFile();
                getResults(msgChan, handler.uncompVoiceData);
            } catch (IOException e) {
                e.printStackTrace();
            }

        };

        scheduler.schedule(runnable,5, TimeUnit.SECONDS);
    }

    public boolean checkIfAnythingThere(byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return true;
            }
        }
        return false;
    }

    private void getResults(MessageChannel msgChan, byte[] pcm) throws IOException {
//        if (checkIfAnythingThere(pcm)) {
//            System.out.println("We got something in combined!.");
//        }
//        ByteArrayInputStream stream = new ByteArrayInputStream(pcm);
//        File rawFile = new File("in.raw");
//        FileUtils.writeByteArrayToFile(rawFile, pcm);
        pcm = writeToFile();
        String in = "in.raw";
        try {
            byte[] wavData = createWav(pcm);
            streamingRecognizeFile(msgChan, in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] get32BitPcm(byte[] data) {
        byte[] resultData = new byte[2 * data.length];
        int iter = 0;
        for (double sample : data) {
            short maxSample = (short)((sample * Integer.MAX_VALUE));
            resultData[iter++] = (byte)(maxSample & 0x00ff);
            resultData[iter++] = (byte)((maxSample & 0xff00) >>> 16);
        }
        return resultData;
    }

    public byte[] createWav(byte[] pcm) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] header = new byte[44];
        byte[] data = pcm;

        long totalDataLen = data.length + 36;
        int srate = 48000;
        int channel = 1;
        int format = 16;
        long bitrate = srate * channel * format;

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = (byte) format;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channel;
        header[23] = 0;
        header[24] = (byte) (srate & 0xff);
        header[25] = (byte) ((srate >> 8) & 0xff);
        header[26] = (byte) ((srate >> 16) & 0xff);
        header[27] = (byte) ((srate >> 24) & 0xff);
        header[28] = (byte) ((bitrate / 8) & 0xff);
        header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
        header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
        header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
        header[32] = (byte) ((channel * format) / 8);
        header[33] = 0;
        header[34] = 16;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (data.length  & 0xff);
        header[41] = (byte) ((data.length >> 8) & 0xff);
        header[42] = (byte) ((data.length >> 16) & 0xff);
        header[43] = (byte) ((data.length >> 24) & 0xff);

        os.write(header, 0, 44);
        try {
            os.write(data);
            byte[] wavData = os.toByteArray();
            os.close();
            return wavData;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void streamingRecognizeFile(MessageChannel msgChan, String rawFile) throws Exception, IOException {
        Stopwatch watch = Stopwatch.createUnstarted();
        watch.start();
        String outFileName = "final.wav";
        File file = new File(outFileName);
        file.delete();
//        FileUtils.writeByteArrayToFile(file, wavData);



        System.out.println("Starting command: " + watch.toString());
        String command = "ffmpeg -f s32be -ar 48000 -ac 1 -i " + rawFile + " -ar 44100 " + outFileName;
//        Runtime.getRuntime().exec(command);

//        ProcessBuilder pb = new ProcessBuilder();
//        pb.directory(new File("C:\\Users\\Valued Customer\\Documents\\GitHub\\discord-dau\\"));
//        pb.command(command);
//        pb.start();

        Runtime.getRuntime().exec(command);
        System.out.println("Command executed: " + watch.toString());

        // The path to the audio file to transcribe

//                Thread.sleep(10000);
        while (!file.exists()) {
            System.out.println("waiting...");
        }

        String fileName = "final.wav";
        // Reads the audio file into memory



        Path path = Paths.get(fileName);
        byte[] data = Files.readAllBytes(path);
        ByteString audioBytes = ByteString.copyFrom(data);
        System.out.println("Initialization complete: " + watch.toString());

        // Builds the sync recognize request
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(44100)
                .setLanguageCode("en-US")
                .build();

        SpeechClient speech = null;
        try {
            speech = SpeechClient.create();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream audioOut = marytts.generateAudio(speechToText);
            msgChan.sendMessage("The received messaged was: " + speechToText).queue();
            clip.open(audioOut);
            clip.start();
            watch.stop();
        } catch (Exception e) {

        }

    }
}