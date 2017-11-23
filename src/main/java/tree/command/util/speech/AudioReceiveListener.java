package tree.command.util.speech;


import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.speech.v1.*;
import com.google.common.util.concurrent.SettableFuture;
import com.google.longrunning.Operation;
import com.google.protobuf.ByteString;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.CombinedAudio;
import net.dv8tion.jda.core.audio.UserAudio;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
//import com.google.protobuf.*;
import com.google.api.*;
import org.threeten.bp.Duration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Valued Customer on 8/4/2017.
 */
public class AudioReceiveListener implements AudioReceiveHandler
{
    public double volume = 1.0;

    public static final int MAX_RECORD_TIME = 3840 * 50 * 15;
    public byte[] uncompVoiceData = new byte[MAX_RECORD_TIME]; //3840bytes/array * 50arrays/sec * 5sec = 5sec
    public int uncompIndex = 0;

    public byte[] uncompUserVoiceData = new byte[MAX_RECORD_TIME]; //3840bytes/array * 50arrays/sec * 5sec = 5sec
    public int uncompUserIndex = 0;
    public ConcurrentLinkedQueue<byte[]> queue;

    public AudioReceiveListener(double volume) {
        queue = new ConcurrentLinkedQueue<>();
        this.volume = volume;
    }

    public void reset() {
        queue.clear();
        uncompVoiceData = new byte[MAX_RECORD_TIME];
        uncompIndex = 0;
        uncompUserVoiceData = new byte[MAX_RECORD_TIME];
        uncompUserIndex = 0;
    }

    @Override
    public boolean canReceiveCombined()
    {
        return true;
    }

    @Override
    public boolean canReceiveUser()
    {
        return true;
    }

    public byte[] getVoiceData() {
        return uncompVoiceData;
    }


    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        byte[] data = combinedAudio.getAudioData(1.0);
        queue.add(data);
    }


    @Override
    public void handleUserAudio(UserAudio userAudio) {
            queue.add(userAudio.getAudioData(1.0));
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

}
