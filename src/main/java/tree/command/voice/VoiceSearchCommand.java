package tree.command.voice;

//import com.darkprograms.speech.microphone.Microphone;
//import com.darkprograms.speech.recognizer.GoogleResponse;
//import com.darkprograms.speech.recognizer.Recognizer;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.CombinedAudio;
import net.dv8tion.jda.core.audio.UserAudio;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.managers.impl.AudioManagerImpl;
import tree.command.util.speech.AudioReceiveListener;
import tree.command.util.speech.AudioSendHandler;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;
import tree.commandutil.type.VoiceCommand;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioSystem;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Jarvis Speech API Tutorial
 * @author Aaron Gokaslan (Skylion)
 *
 */
public class VoiceSearchCommand implements VoiceCommand {
    private String commandName;
    private List<Byte[]> audioDataSample;
    private CombinedAudio combinedAudio;

    public VoiceSearchCommand(String commandName) {
        this.commandName = commandName;
        audioDataSample = new ArrayList<>();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        search(guild, msgChan);

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



    private void search(Guild guild, MessageChannel msgChan) {
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setReceivingHandler(new AudioReceiveListener(1.0, guild.getVoiceChannelById(314495018079617026L)));
        AudioReceiveListener ah = (AudioReceiveListener) guild.getAudioManager().getReceiveHandler();
        audioManager.openAudioConnection(guild.getVoiceChannelById(314495018079617026L));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        combinedAudio = new CombinedAudio(guild.getVoiceChannelById(314495018079617026L)
                .getJDA().getUsers(), new short[(int) 3840 * 50 * 60 / 3000]);
        ah.handleCombinedAudio(combinedAudio);
        audioManager.closeAudioConnection();


        //audioManager.setSendingHandler(new AudioSendHandler(ah.getVoiceData()));



//        // Mixer.Info[] infoArray = AudioSystem.getMixerInfo();
//        // for(Mixer.Info info : infoArray) {
//        //    System.out.println("info: " + info.toString());
//        // }
//        Type[] typeArray = AudioSystem.getAudioFileTypes();
//        for(Type type : typeArray) {
//            System.out.println("type: " + type.toString());
//        }
//
//        Microphone mic = new Microphone(FLACFileWriter.FLAC);
//        File file = new File ("testfile2.flac");	//Name your file whatever you want
//        try {
//            mic.captureAudioToFile (file);
//        } catch (Exception ex) {
//            //Microphone not available or some other error.
//            System.out.println ("ERROR: Microphone is not availible.");
//            ex.printStackTrace ();
//        }
//
//    /* User records the voice here. Microphone starts a separate thread so do whatever you want
//     * in the mean time. Show a recording icon or whatever.
//     */
//        try {
//            System.out.println ("Recording...");
//            Thread.sleep (5000);	//In our case, we'll just wait 5 seconds.
//            mic.close ();
//        } catch (InterruptedException ex) {
//            ex.printStackTrace ();
//        }
//
//        mic.close ();		//Ends recording and frees the resources
//        System.out.println ("Recording stopped.");
//
//        Recognizer recognizer = new Recognizer (Recognizer.Languages.ENGLISH_US, System.getProperty("google-api-key"));
//        //Although auto-detect is available, it is recommended you select your region for added accuracy.
//        try {
//            int maxNumOfResponses = 4;
//            System.out.println("Sample rate is: " + (int) mic.getAudioFormat().getSampleRate());
//            GoogleResponse response = recognizer.getRecognizedDataForFlac (file, maxNumOfResponses, (int) mic.getAudioFormat().getSampleRate ());
//            System.out.println ("Google Response: " + response.getResponse ());
//            System.out.println ("Google is " + Double.parseDouble (response.getConfidence ()) * 100 + "% confident in" + " the reply");
//            System.out.println ("Other Possible responses are: ");
//            for (String s:response.getOtherPossibleResponses ()) {
//                System.out.println ("\t" + s);
//            }
//        }
//        catch (Exception ex) {
//            // TODO Handle how to respond if Google cannot be contacted
//            System.out.println ("ERROR: Google cannot be contacted");
//            ex.printStackTrace ();
//        }

        //SynthesiserV2 synth = new SynthesiserV2();

        //file.deleteOnExit ();	//Deletes the file as it is no longer necessary.
    }
}