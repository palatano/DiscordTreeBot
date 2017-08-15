package tree.command.picture;

import javafx.util.Pair;
import net.dv8tion.jda.core.MessageBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import tree.Config;
import tree.command.util.MessageUtil;
import tree.commandutil.type.PictureCommand;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Admin on 7/31/2017.
 */
public class NuggetCommand implements PictureCommand {
    String commandName;
    private static final int NUMBER_NUG_PHOTOS = 21;
    private static final String[] fileExtensions = {".jpg", ".gif", ".png"};
    private static final int SPAM_NUG_COUNT = 5;
    private PriorityQueue<Pair<Integer, Integer>> photoIDsPosted;
    private int numNugCount = 0;
    private boolean nugPicAllowed = true;
    private StopWatch nuggetStopWatch;
    private Timer timer;

    private void timerInitialize() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                nugPicAllowed = true;
                numNugCount = 0;
            }
        }, 0, 30000);
    }

    private String getOSNuggetPath() {
        String osName = Config.getOsName();
        if (osName.indexOf("win") >= 0) {
            return "discord-dau\\etc\\nug\\";
        } else if (osName.indexOf("nux") >= 0){
            return "discord-dau/etc/nug/";
        } else {
            return null;
        }
    }

    public NuggetCommand(String commandName) {
        this.commandName = commandName;
        photoIDsPosted = new PriorityQueue<>(Comparator.comparing(Pair::getValue));
        nuggetStopWatch = new StopWatch();
        nuggetStopWatch.start();
        timer = new Timer();
        timerInitialize();
    }

    @Override
    public String help() {
        return "Sends a random nugget picture to chat.";
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member, String[] args) {
        if (!nugPicAllowed) {
            if (numNugCount >= SPAM_NUG_COUNT) {
                guild.getJDA()
                        .getUserById(Config.CONFIG.getAdminID())
                        .openPrivateChannel()
                        .queue(chan ->
                                chan.sendMessage("You got some spam bro. Seems that " +
                                        member.getUser().getName() + " is spamming at " +
                                        MessageUtil.timeStamp(message) + ".")
                                        .queue());
            }
            msgChan.sendMessage("Too many nugget command requests. Calm down bro.")
                    .queue(msg -> msg.addReaction(
                            msg.getJDA().getEmoteById(286674438009782272L)).queue());
            return;
        }
        writeRandomNugPhoto(msgChan);
    }

    public String getCommandName() {
        return commandName;
    }

    private boolean tryFileExtension(String filePath, String filename, String ext) {
        File nugFile = new File(filePath + filename + ext);
        return nugFile.exists();
    }

    private int getRandomID() {
        int rand = (int) Math.ceil(Math.random() * NUMBER_NUG_PHOTOS);
        for (Pair p : photoIDsPosted) {
            int ID = (int) p.getKey();
            if (rand == ID) {
                return getRandomID();
            }
        }
        return rand;
    }

    private void incrementNuggetCount() {
        if (++numNugCount >= 5) {
            nugPicAllowed = false;
        }
    }


    public void writeRandomNugPhoto(MessageChannel msgChan) {
        incrementNuggetCount();
        int rand = getRandomID();
        String nugFilePath = Config.CONFIG.getFilePath() + getOSNuggetPath();
        String nugFileName = "nug" + rand;
        File outputNugFile = null;

        for (String ext : fileExtensions) {
            if (tryFileExtension(nugFilePath, nugFileName, ext)) {
                // To prevent the bot from posting the same photo five
                // consecutive times, keep track of photo IDs.
                if (photoIDsPosted.size() >= 5) {
                    Pair p = photoIDsPosted.poll();
                    System.out.println("ID = " + p.getKey() + "time = " + p.getValue());
                }
                photoIDsPosted.offer(new Pair(rand, (int) nuggetStopWatch.getTime()));
                outputNugFile = new File(nugFilePath + nugFileName + ext).getAbsoluteFile();
                convertAndSendImage(outputNugFile, msgChan);
                return;
            }
        }
        System.out.println("No files found with extensions" + fileExtensions.toString());
    }

    private boolean convertAndSendImage(File outputNugFile, MessageChannel msgChan) {
        int fileNameLength = outputNugFile.getName().length();
        if (outputNugFile.getName()
                .substring(fileNameLength - 4, fileNameLength)
                .equals(".gif")) {
            Message msg = new MessageBuilder().append(" ").build();
            msgChan.sendFile(outputNugFile, msg).queue();
            return true;
        }
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(FileUtils.readFileToByteArray(outputNugFile));
            BufferedImage image = ImageIO.read(bais);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            File nugToDiscord = new File("nug.png");
            OutputStream outputStream = new FileOutputStream(nugToDiscord);
            baos.writeTo(outputStream);
            baos.close();

            Message msg = new MessageBuilder().append(" ").build();
            msgChan.sendFile(nugToDiscord, msg).queue();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("File could not be created.");
            return false;
        }
        return true;
    }
}
