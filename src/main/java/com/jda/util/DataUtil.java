package com.jda.util;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.StringCharacterIterator;
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

    public void createMap() {
        uniqueUsersSingleChannelMap = new HashMap<>();
    }

    public void setDate(int[] dateValues) {
        currDate = dateValues[0] + "_" + dateValues[1] + "_" + dateValues[2];
    }
    /**
     * Get the credentials data structure for logging the bot into the server.
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

    public void writeChannelDataYaml(String channelName, int[] dateValues) {
        String date = dateValues[0] + "_" + dateValues[1] + "_" + dateValues[2];
        File fileDesktop = createFormattedFile(channelName, date, false);
        File fileDiscord = createFormattedFile(channelName, date, true);
        BufferedWriter writerToDesktop = getWriter(fileDesktop);
        if (writerToDesktop == null) {
            return;
        }
        closeDataWriter(writerToDesktop);
        yaml.dump(uniqueUsersSingleChannelMap, writerToDesktop);
        String dataForDiscord = yaml.dump(uniqueUsersSingleChannelMap);
        try {
            FileUtils.writeStringToFile(fileDiscord, dataForDiscord);
        } catch (IOException e) {
            System.out.println("File cannot be written with YAML dump to a string. Error.");
            return;
        }
    }

    private File createFormattedFile(String channelName, String date, boolean writeToDiscord) {
        String filename = channelName + "_output_" + date + ".xlsx";
        if (writeToDiscord) {
            return new File(filename);
        }
        return new File(creds.get("filepath") + filename);
    }

    public void writeChannelDataExcel(String channelName, int[] dateValues,
                                     boolean writeToDiscord, MessageChannel msgChan) {
        String date = dateValues[0] + "_" + dateValues[1] + "_" + dateValues[2];
        File fileDesktop = createFormattedFile(channelName, date, false);
        File fileDiscord = createFormattedFile(channelName, date, true);

        excelUtil.writeToExcel(uniqueUsersSingleChannelMap, fileDesktop, date, channelName);
        if (writeToDiscord) {
            excelUtil.writeToExcel(uniqueUsersSingleChannelMap, fileDiscord, date, channelName);
            Message message = new MessageBuilder().append("The data for #" + channelName + " on " + date +
                    " is shown below: ").build();
            msgChan.sendFile(fileDiscord, fileDesktop.getName(), message).queue();
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
        File fileDiscord = new File("all_channel_data_" + currDate + ".xlsx");
        Map outputMap = combineChannelData();
        excelUtil.writeToExcel(outputMap, fileDesktop, currDate, "all channels");
        excelUtil.writeToExcel(outputMap, fileDiscord, currDate, "all channels");
        String channelList = "";
        for (Object key : uniqueUsersAllChannelsMap.keySet()) {
            String channelName = (String) key;
            channelList += "#" + channelName + ", ";
        }
        Message message = new MessageBuilder().append("The data for {" +
                channelList.substring(0, channelList.length() - 2) + "} on " + currDate +
                " is shown below: ").build();
        msgChan.sendFile(fileDiscord, fileDesktop.getName(), message).queue();
    }


}
