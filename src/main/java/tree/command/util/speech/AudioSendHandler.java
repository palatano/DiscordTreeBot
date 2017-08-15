package tree.command.util.speech;

import java.util.Arrays;

/**
 * Created by Valued Customer on 8/5/2017.
 */
public class AudioSendHandler implements net.dv8tion.jda.core.audio.AudioSendHandler {
    private byte[][] voiceData;
    private boolean canProvide;
    private int index;

        public AudioSendHandler(byte[] data) {
            canProvide = true;
            voiceData = new byte[data.length / 3840][3840];
            for (int i=0; i < voiceData.length; i++) {
                voiceData[i] = Arrays.copyOfRange(data, i * 3840, i * 3840 + 3840);
            }
        }

        @Override
        public boolean canProvide() {
            return canProvide;
        }

        @Override
        public byte[] provide20MsAudio() {
            if (index == voiceData.length - 1)
                canProvide = false;
            return voiceData[index++];
        }

        @Override
        public boolean isOpus() {
            return false;
        }
}
