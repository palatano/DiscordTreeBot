package com.jda.util;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
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
    private Map<String, String> uniqueUsersSingleChannelMap;
    private Map<String, Map> uniqueUsersAllChannelsMap;
    private Yaml yaml;

    public DataUtil() {
        uniqueUsersSingleChannelMap = new HashMap<>();
        uniqueUsersAllChannelsMap = new HashMap<>();
        yaml = new Yaml();
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
            uniqueUsersSingleChannelMap.put(user, MessageUtil.timeStamp(msg));
        }
    }

    public void putUniqueUserMap(String chanName) {
        uniqueUsersAllChannelsMap.put(chanName, uniqueUsersSingleChannelMap);
    }

    private BufferedWriter getWriter(String path) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("File cannot be written to. Error.");
            return null;
        }
        return writer;
    }

    public void writeChannelDataYaml() {
        BufferedWriter writer =
                getWriter("C:\\Users\\Admin\\Documents\\GitHub\\single_output.yaml");
        if (writer == null) {
            return;
        }
        yaml.dump(uniqueUsersSingleChannelMap, writer);
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Can't be closed.");
            System.exit(0);
        }
    }

    private Map combineChannelData() {
        Map<String, String> channelDataMap = new HashMap<>();
        // Iterate over all maps, which represent data from a single channel.
        for (Map.Entry allEntry : uniqueUsersAllChannelsMap.entrySet()) {
            String currChannelName = (String) allEntry.getKey();
            Map<String, String> currChannelMap = (Map<String, String>) allEntry.getValue();
            for (Map.Entry singleEntry : currChannelMap.entrySet()) {
                String userName = (String) singleEntry.getKey();
                String timeStampOne = (String) singleEntry.getValue();
                // For each single channel, check if the time stamp is earlier than the one already stored.
                if (!channelDataMap.containsKey(userName)) {
                    channelDataMap.put(userName, timeStampOne);
                } else {
                    String timeStampTwo = currChannelMap.get(userName);
                    if (MessageUtil.compareTimeStamp(timeStampOne, timeStampTwo) == 1) {
                        currChannelMap.put(userName, timeStampOne);
                    }
                }
            }
        }
        return channelDataMap;
    }

    public void writeAllChannelsDataYaml() {
        BufferedWriter writer =
                getWriter("C:\\Users\\Admin\\Documents\\GitHub\\all_channel_output.yaml");
        if (writer == null) {
            return;
        }
        Map outputMap = combineChannelData();
        yaml.dump(outputMap, writer);
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Can't be closed.");
            System.exit(0);
        }
    }
}
