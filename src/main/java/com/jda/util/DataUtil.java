package com.jda.util;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.StringCharacterIterator;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Valued Customer on 7/21/2017.
 */
public class DataUtil {
    private Map<String, Object> creds;
    private Map<String, MemberData> uniqueUsersSingleChannelMap;
    private Map<String, Map> uniqueUsersAllChannelsMap;
    private Yaml yaml;
    private ExcelUtil excelUtil;
    private String currDate;
    private static final int NUMBER_NUG_PHOTOS = 12;

    public DataUtil() {
        uniqueUsersSingleChannelMap = new HashMap<>();
        uniqueUsersAllChannelsMap = new HashMap<>();
        yaml = new Yaml();
        excelUtil = new ExcelUtil();
    }

    public Map<String, Object> getCreds() {
        return creds;
    }

    public void setCreds(Map<String, Object> creds) {
        this.creds = creds;
    }

    public void resetMap() {
        uniqueUsersAllChannelsMap = new HashMap<>();
    }

    public void createUniqueUsersMap() {uniqueUsersSingleChannelMap = new HashMap<>();}

    public void setDate(Calendar cal) {
        currDate = (cal.get(Calendar.MONTH) + 1)  + "_" + cal.get(Calendar.DAY_OF_MONTH) +
                "_" + cal.get(Calendar.YEAR);
    }

    /**
     * Get the credentials data structure for logging the bot into the server.
     *
     * @param file - The filename argument.
     * @return The credentials data structure.
     */
    public static Map<String, Object> retrieveCreds(String file) {
        Map<String, Object> creds = null;
        try {
            File credsFile = new File(file);
            Yaml yaml = new Yaml();
            String credsFileString = FileUtils.readFileToString(credsFile);
            creds = (Map<String, Object>) yaml.load(credsFileString);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        return creds;
    }

    public void putUniqueUser(Message msg, MessageChannel msgChan) {
        String user = msg.getAuthor().getName();
        if (!uniqueUsersSingleChannelMap.containsKey(user)) {
            MemberData memData = new MemberData(MessageUtil.timeStamp(msg), msg.getContent(),
                    msg.getAuthor().getName(), msg.getMember().getNickname());
            uniqueUsersSingleChannelMap.put(user, memData);
        }
    }

    public void putUniqueUserMap(String chanName) {
        uniqueUsersAllChannelsMap.put(chanName, uniqueUsersSingleChannelMap);
    }

    private BufferedWriter getWriter(File file) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("File cannot be written to. Error.");
            return null;
        }
        return writer;
    }

    public void writeChannelDataYaml(String channelName, int[] dateValues,
                                     boolean writeToDiscord, MessageChannel msgChan) {
        String date = dateValues[0] + "_" + dateValues[1] + "_" + dateValues[2];
        String filename = channelName + "_output_" + date + ".yaml";
        File fileDesktop = new File(creds.get("filePath") + filename);
        ;
        BufferedWriter writerToDesktop = getWriter(fileDesktop);
        if (writerToDesktop == null) {
            return;
        }
        closeDataWriter(writerToDesktop);
        yaml.dump(uniqueUsersSingleChannelMap, writerToDesktop);
        if (writeToDiscord) {
            fileDesktop.renameTo(new File(channelName + "_output_" + date + ".yaml"));
            Message message = new MessageBuilder().append("The data for #" + channelName + " on " + date +
                    " is shown below: ").build();
            msgChan.sendFile(fileDesktop, fileDesktop.getName(), message).queue();
        }
    }

    public void writeChannelDataExcel(String channelName, Calendar cal,
                                      boolean writeToDiscord, MessageChannel msgChan) {
        String filename = channelName + "_output_" + currDate + ".xlsx";
        File fileDesktop = new File(creds.get("filePath") + filename);

        excelUtil.writeToExcel(uniqueUsersSingleChannelMap, fileDesktop, currDate, channelName);
        if (writeToDiscord) {
            fileDesktop.renameTo(new File(channelName + "_" + currDate + ".xlsx"));
            Message message = new MessageBuilder().append("The data for #" + channelName + " on " + currDate +
                    " is shown below: ").build();
            msgChan.sendFile(fileDesktop, fileDesktop.getName(), message).queue();
        }
    }

    private Map combineChannelData() {
        Map<String, MemberData> channelDataMap = new HashMap<>();
        // Iterate over all maps, which represent data from a single channel.
        for (Map.Entry allEntry : uniqueUsersAllChannelsMap.entrySet()) {
            String currChannelName = (String) allEntry.getKey();
            Map<String, MemberData> currChannelMap = (Map<String, MemberData>) allEntry.getValue();
            for (Map.Entry singleEntry : currChannelMap.entrySet()) {
                String userName = (String) singleEntry.getKey();
                MemberData memData = (MemberData) singleEntry.getValue();
                // For each single channel, check if the time stamp is earlier than the one already stored.
                if (!channelDataMap.containsKey(userName)) {
                    channelDataMap.put(userName, memData);
                } else {
                    MemberData memDataTwo = currChannelMap.get(userName);
                    if (MessageUtil.compareTimeStamp(memData.timeStamp, memDataTwo.timeStamp) == 1) {
                        currChannelMap.put(userName, memDataTwo);
                    }
                }
            }
        }
        return channelDataMap;
    }

    private void closeDataWriter(BufferedWriter writer) {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Can't be closed.");
            System.exit(0);
        }
    }

    public void writeAllChannelsDataYaml() {
        File file = new File(creds.get("filepath") + "all_channel_output.yaml");
        BufferedWriter writer =
                getWriter(file);
        if (writer == null) {
            return;
        }
        Map outputMap = combineChannelData();
        yaml.dump(outputMap, writer);
        closeDataWriter(writer);
    }

    public void writeAllChannelDataExcel(MessageChannel msgChan) {
        File fileDesktop = new File(creds.get("filePath") + "all_channel_data_" + currDate + ".xlsx");
        Map outputMap = combineChannelData();
        excelUtil.writeToExcel(outputMap, fileDesktop, currDate, "all channels");
        String channelList = "";
        String name = "";
        for (Object key : uniqueUsersAllChannelsMap.keySet()) {
            String channelName = (String) key;
            channelList += "#" + channelName + ", ";
            name = fileDesktop.getAbsoluteFile() + "";
        }
        File fileDiscord = new File(name);
        Message message = new MessageBuilder().append("The data for {" +
               channelList.substring(0, channelList.length() - 2) + "} on " + currDate +
                    " is shown below: ").build();
        msgChan.sendFile(fileDiscord, fileDiscord.getName(), message).queue();
    }

    private boolean tryFileExtension(String filePath, String filename, String ext) {
//        try {
            File nugFile = new File(filePath + filename + ext);
            return nugFile.exists();
//        } catch (FileNotFoundException fnfe) {
//            System.out.println(filename + " with extension " + ext + " not found");
//            return false;
//        }
//        return true;
    }

    private boolean convertAndSendImage(File outputNugFile, MessageChannel msgChan) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(FileUtils.readFileToByteArray(outputNugFile));
            BufferedImage image = ImageIO.read(bais);
//            BufferedImage newBufferedImage = new BufferedImage(image.getWidth(),
//                    image.getHeight(), BufferedImage.TYPE_INT_RGB);
//            newBufferedImage.createGraphics().drawImage(image,
//                    0, 0, Color.WHITE, null);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            File nugToDiscord = new File("nug.png");
            OutputStream outputStream = new FileOutputStream (nugToDiscord);
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

    public void writeRandomNugPhoto(MessageChannel msgChan) {
        int rand = (int) Math.ceil(Math.random() * NUMBER_NUG_PHOTOS);
        String nugFilePath = creds.get("filePath") + "\\discord-dau\\etc\\nug\\";
        String nugFileName = "nug" + rand;
        String[] fileExtensions = new String[]{".png", ".jpg"};
        File outputNugFile = null;

        for (String ext : fileExtensions) {
            if (tryFileExtension(nugFilePath, nugFileName, ext)) {
                outputNugFile = new File(nugFilePath + nugFileName + ext);
                convertAndSendImage(outputNugFile, msgChan);
                return;
            }
        }
        System.out.println("No files found with extensions" + fileExtensions.toString());
    }
}