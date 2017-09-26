package tree.command.util;

import tree.Config;
import tree.command.util.ExcelUtil;
import tree.command.data.MemberData;
import tree.command.util.MessageUtil;
import javafx.util.Pair;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by Valued Customer on 7/21/2017.
 */
public class DataUtil {
    private Map<String, MemberData> uniqueUsersSingleChannelMap;
    private Map<String, Map> uniqueUsersAllChannelsMap;
    private Yaml yaml;
    private ExcelUtil excelUtil;
    private String currDate;

    public DataUtil() {
        uniqueUsersSingleChannelMap = new HashMap<>();
        uniqueUsersAllChannelsMap = new HashMap<>();
        yaml = new Yaml();
    }

    public void resetMap() {
        uniqueUsersAllChannelsMap = new HashMap<>();
    }

    public void createUniqueUsersMap() {uniqueUsersSingleChannelMap = new HashMap<>();}

    public void setDate(Calendar cal) {
        currDate = (cal.get(Calendar.MONTH) + 1)  + "_" + cal.get(Calendar.DAY_OF_MONTH) +
                "_" + cal.get(Calendar.YEAR);
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
        File fileDesktop = new File(Config.getFilePath() + filename);
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
        File fileDesktop = new File(Config.getFilePath() + filename);

        ExcelUtil.writeToExcel(uniqueUsersSingleChannelMap, fileDesktop, currDate, channelName);
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
                    if (MessageUtil.compareTimeStamp(memData.getTimeStamp(), memDataTwo.getTimeStamp()) == 1) {
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
        File file = new File(Config.getFilePath() + "all_channel_output.yaml");
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
        File fileDesktop = new File(Config.getFilePath() + "all_channel_data_" + currDate + ".xlsx");
        Map outputMap = combineChannelData();
        ExcelUtil.writeToExcel(outputMap, fileDesktop, currDate, "all channels");
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

    public void writeGuildDataToFile(String fileName, String toWrite) {
        String filePath = Config.getFilePath();
        File guildDataFile = new File(filePath + fileName + ".txt");
        try {
            FileUtils.writeStringToFile(guildDataFile, toWrite, "UTF-8");
        } catch (IOException e) {
            System.out.println("Cannot write to file");
            e.printStackTrace();
        }
    }



}