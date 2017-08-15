package tree.command.util.speech;


import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.speech.spi.v1.SpeechClient;
import com.google.cloud.speech.v1.*;
import com.google.common.util.concurrent.SettableFuture;
import com.google.longrunning.Operation;
import com.google.protobuf.ByteString;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.CombinedAudio;
import net.dv8tion.jda.core.audio.UserAudio;
import net.dv8tion.jda.core.entities.VoiceChannel;
//import com.google.protobuf.*;
import com.google.api.*;
import org.threeten.bp.Duration;

import java.util.List;

/**
 * Created by Valued Customer on 8/4/2017.
 */
public class AudioReceiveListener implements AudioReceiveHandler
{
    public static final double STARTING_MB = 0.5;
    public static final int CAP_MB = 16;
    public static final double PCM_MINS = 2;
    public double AFK_LIMIT = 2;
    public boolean canReceive = true;
    public double volume = 1.0;
    private VoiceChannel voiceChannel;

    public byte[] uncompVoiceData = new byte[(int) (3840 * 50 * 60) ]; //3840bytes/array * 50arrays/sec * 60sec = 1 mins
    public int uncompIndex = 0;

    public byte[] compVoiceData = new byte[(int) (1024 * 1024 * STARTING_MB)];    //start with 0.5 MB
    public int compIndex = 0;

    public boolean overwriting = false;

    private int afkTimer;

    public AudioReceiveListener(double volume, VoiceChannel voiceChannel) {
        this.volume = volume;
        this.voiceChannel = voiceChannel;
    }

    @Override
    public boolean canReceiveCombined()
    {
        return canReceive;
    }

    @Override
    public boolean canReceiveUser()
    {
        return false;
    }
/*
    //encode the passed array of PCM (uncompressed) audio to mp3 audio data
    public static byte[] encodePcmToMp3(byte[] pcm) {
        LameEncoder encoder =
                new LameEncoder(new AudioFormat(
                        48000.0f, 16,
                        2, true, true), 128,
                        MPEGMode.STEREO, Lame.QUALITY_HIGHEST, false);
        ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
        byte[] buffer = new byte[encoder.getPCMBufferSize()];

        int bytesToTransfer = Math.min(buffer.length, pcm.length);
        int bytesWritten;
        int currentPcmPosition = 0;
        while (0 < (bytesWritten = encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer))) {
            currentPcmPosition += bytesToTransfer;
            bytesToTransfer = Math.min(buffer.length, pcm.length - currentPcmPosition);

            mp3.write(buffer, 0, bytesWritten);
        }

        encoder.close();

        return mp3.toByteArray();
    }*/

    public byte[] getVoiceData() {
        return uncompVoiceData;
    }

    public static void streamRecognizeFile(byte[] data) throws Exception {
        ByteString audioBytes = ByteString.copyFrom(data);
        SpeechClient speech = SpeechClient.create();
        //SpeechClient speech = SpeechClient.create();

        // Builds the sync recognize request
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
                //.setSampleRateHertz(16000)
                .setLanguageCode("en-US")
                .build();
        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioBytes)
                .build();

        // Performs speech recognition on the audio file
       //speech.recog
        RecognizeResponse response = speech.recognize(config, audio);
        List<SpeechRecognitionResult> results = response.getResultsList();

        for (SpeechRecognitionResult result: results) {
            List<SpeechRecognitionAlternative> alternatives = result.getAlternativesList();
            for (SpeechRecognitionAlternative alternative: alternatives) {
                System.out.printf("Transcription: %s%n", alternative.getTranscript());
            }
        }
        speech.close();
    }

        // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
        /*SpeechClient speech = SpeechClient.create();

        // Configure request with local raw PCM audio
        RecognitionConfig recConfig = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setLanguageCode("en-US")
                .setSampleRateHertz(16000)
                .build();
        StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder()
                .setConfig(recConfig)
                .build();

        class ResponseApiStreamingObserver<T> implements ApiStreamObserver<T> {
            private final SettableFuture<List<T>> future = SettableFuture.create();
            private final List<T> messages = new java.util.ArrayList<T>();

            @Override
            public void onNext(T message) {
                messages.add(message);
            }

            @Override
            public void onError(Throwable t) {
                future.setException(t);
            }

            @Override
            public void onCompleted() {
                future.set(messages);
            }

            // Returns the SettableFuture object to get received messages / exceptions.
            public SettableFuture<List<T>> future() {
                return future;
            }
        }

        ResponseApiStreamingObserver<StreamingRecognizeResponse> responseObserver =
                new ResponseApiStreamingObserver<StreamingRecognizeResponse>();

        StreamingCallable<StreamingRecognizeRequest,StreamingRecognizeResponse> callable =
                speech.streamingRecognizeCallable();

        ApiStreamObserver<StreamingRecognizeRequest> requestObserver =
                callable.bidiStreamingCall(responseObserver);

        // The first request must **only** contain the audio configuration:
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(config)
                .build());

        // Subsequent requests must **only** contain the audio data.
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data))
                .build());

        // Mark transmission as completed after sending the data.
        requestObserver.onCompleted();

        List<StreamingRecognizeResponse> responses = responseObserver.future().get();

        for (StreamingRecognizeResponse response: responses) {
            for (StreamingRecognitionResult result: response.getResultsList()) {
                for (SpeechRecognitionAlternative alternative : result.getAlternativesList()) {
                    System.out.println(alternative.getTranscript());
                }
            }
        }
        speech.close();
    }*/


    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio)
    {

        uncompIndex = 0;
        for (byte b : combinedAudio.getAudioData(volume)) {
            uncompVoiceData[uncompIndex++] = b;
        }

        try {
            streamRecognizeFile(uncompVoiceData);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        //String data = new String(uncompVoiceData);
//        File out = new File("out");
//        try {
//            FileUtils.writeByteArrayToFile(out, uncompVoiceData);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Mixer.Info[] infoArray = AudioSystem.getMixerInfo();
        // for(Mixer.Info info : infoArray) {
        //    System.out.println("info: " + info.toString());
        // }

        /*
        AudioFileFormat.Type[] typeArray = AudioSystem.getAudioFileTypes();
        for(AudioFileFormat.Type type : typeArray) {
            System.out.println("type: " + type.toString());
        }

        Microphone mic = new Microphone(FLACFileWriter.FLAC);
        File file = new File ("testfile2.flac");	//Name your file whatever you want
        try {
            mic.captureAudioToFile (file);
        } catch (Exception ex) {
            //Microphone not available or some other error.
            System.out.println ("ERROR: Microphone is not availible.");
            ex.printStackTrace ();
        }

    /* User records the voice here. Microphone starts a separate thread so do whatever you want
     * in the mean time. Show a recording icon or whatever.

        try {
            System.out.println ("Recording...");
            Thread.sleep (5000);	//In our case, we'll just wait 5 seconds.
            mic.close ();
        } catch (InterruptedException ex) {
            ex.printStackTrace ();
        }

        mic.close ();		//Ends recording and frees the resources
        System.out.println ("Recording stopped.");*/
/*
        Recognizer recognizer = new Recognizer (Recognizer.Languages.ENGLISH_US, System.getProperty("google-api-key"));
        //Although auto-detect is available, it is recommended you select your region for added accuracy.
        try {
            int maxNumOfResponses = 4;
            //System.out.println("Sample rate is: " + (int) mic.getAudioFormat().getSampleRate());
            recognizer.setApiKey("AIzaSyA1HuuVVp8dDAsJBbP-5UK3oP-jxCRdLwk");
            GoogleResponse response = recognizer.getRecognizedDataForWave(out, maxNumOfResponses);
            System.out.println("Google Response: " + response.getResponse ());
            System.out.println("Google is " + Double.parseDouble (response.getConfidence ()) * 100 + "% confident in" + " the reply");
            System.out.println("Other Possible responses are: ");
            for (String s:response.getOtherPossibleResponses ()) {
                System.out.println ("\t" + s);
            }
        }
        catch (Exception ex) {
            // TODO Handle how to respond if Google cannot be contacted
            System.out.println ("ERROR: Google cannot be contacted");
            ex.printStackTrace ();
        }*/

        /*
        if (combinedAudio.getUsers().size() == 0) afkTimer++;
        else afkTimer = 0;

        if (afkTimer >= 50 * 60 * AFK_LIMIT) {   //20ms * 50 * 60 seconds * 2 mins = 2 mins
            System.out.format("AFK detected, leaving '%s' voice channel in %s\n", voiceChannel.getName(), voiceChannel.getGuild().getName());
            TextChannel defaultTC = voiceChannel.getGuild().getTextChannelById(serverSettings.get(voiceChannel.getGuild().getId()).defaultTextChannel);
            DiscordEcho.sendMessage(defaultTC, "No audio for 2 minutes, leaving from AFK detection...");

            voiceChannel.getGuild().getAudioManager().closeAudioConnection();
            DiscordEcho.killAudioHandlers(voiceChannel.getGuild());
            return;
        }

        if (uncompIndex == uncompVoiceData.length / 2 || uncompIndex == uncompVoiceData.length) {
            new Thread(() -> {

                if (uncompIndex < uncompVoiceData.length / 2)  //first half
                    addCompVoiceData(DiscordEcho.encodePcmToMp3(Arrays.copyOfRange(uncompVoiceData, 0, uncompVoiceData.length / 2)));
                else
                    addCompVoiceData(DiscordEcho.encodePcmToMp3(Arrays.copyOfRange(uncompVoiceData, uncompVoiceData.length / 2, uncompVoiceData.length )));

            }).start();

            if (uncompIndex == uncompVoiceData.length)
                uncompIndex = 0;
        }

        for (byte b : combinedAudio.getAudioData(volume)) {
            uncompVoiceData[uncompIndex++] = b;
        }*/
    }
/*
    public byte[] getVoiceData() {
        canReceive = false;

        //flush remaining audio
        byte[] remaining = new byte[uncompIndex];

        int start = uncompIndex < uncompVoiceData.length / 2 ? 0 : uncompVoiceData.length / 2;

        for (int i = 0; i < uncompIndex - start; i++) {
            remaining[i] = uncompVoiceData[start + i];
        }

        addCompVoiceData(DiscordEcho.encodePcmToMp3(remaining));

        byte[] orderedVoiceData;
        if (overwriting) {
            orderedVoiceData = new byte[compVoiceData.length];
        } else {
            orderedVoiceData = new byte[compIndex + 1];
            compIndex = 0;
        }

        for (int i=0; i < orderedVoiceData.length; i++) {
            if (compIndex + i < orderedVoiceData.length)
                orderedVoiceData[i] = compVoiceData[compIndex + i];
            else
                orderedVoiceData[i] = compVoiceData[compIndex + i - orderedVoiceData.length];
        }

        wipeMemory();
        canReceive = true;

        return orderedVoiceData;
    }


    public void addCompVoiceData(byte[] compressed) {
        for (byte b : compressed) {
            if (compIndex >= compVoiceData.length && compVoiceData.length != 1024 * 1024 * CAP_MB) {    //cap at 16MB

                byte[] temp = new byte[compVoiceData.length * 2];
                for (int i=0; i < compVoiceData.length; i++)
                    temp[i] = compVoiceData[i];

                compVoiceData = temp;

            } else if (compIndex >= compVoiceData.length && compVoiceData.length == 1024 * 1024 * CAP_MB) {
                compIndex = 0;

                if (!overwriting) {
                    overwriting = true;
                    System.out.format("Hit compressed storage cap in %s on %s", voiceChannel.getName(), voiceChannel.getGuild().getName());
                }
            }


            compVoiceData[compIndex++] = b;
        }
    }


    public void wipeMemory() {
        System.out.format("Wiped recording data in %s on %s", voiceChannel.getName(), voiceChannel.getGuild().getName());
        uncompIndex = 0;
        compIndex = 0;

        compVoiceData = new byte[1024 * 1024 / 2];
        System.gc();
    }


    public byte[] getUncompVoice(int time) {
        canReceive = false;

        if (time > PCM_MINS * 60 * 2) {     //2 mins 60 * 2;
            time = (int)(PCM_MINS * 60 * 2);
        }
        int requestSize = 3840 * 50 * time;
        byte[] voiceData = new byte[requestSize];

        for (int i = 0; i < voiceData.length; i++) {
            if (uncompIndex + i < voiceData.length)
                voiceData[i] = uncompVoiceData[uncompIndex + i];
            else
                voiceData[i] = uncompVoiceData[uncompIndex + i - voiceData.length];
        }

        wipeMemory();
        canReceive = true;
        return voiceData;
    }*/

    @Override
    public void handleUserAudio(UserAudio userAudio) {}
}
