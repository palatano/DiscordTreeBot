package tree.command.util.music;

import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.audio.CombinedAudio;
import net.dv8tion.jda.core.audio.UserAudio;
import tree.command.util.speech.AudioReceiveListener;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Valued Customer on 8/19/2017.
 */
public class AudioEchoHandler implements AudioReceiveHandler, AudioSendHandler {
    private ConcurrentLinkedQueue<byte[]> queue;

    public AudioEchoHandler() {
        queue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean canProvide() {
        return !queue.isEmpty();
    }

    @Override
    public byte[] provide20MsAudio() {
        return queue.poll();
    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    @Override
    public boolean canReceiveUser() {
        return false;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        byte[] data = combinedAudio.getAudioData(1.0);
        queue.add(data);
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
        byte[] data = userAudio.getAudioData(1.0);
        queue.add(data);
    }
}
