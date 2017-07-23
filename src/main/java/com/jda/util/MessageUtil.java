package com.jda.util;

import net.dv8tion.jda.core.entities.Message;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Created by Admin on 7/20/2017.
 */
public class MessageUtil {

    /**
     * Comparator method for time stamps.
     * @return 1 - timeStampOne earlier than timeStampTwo, 0 - equal,
     *         -1 - timeStampOne later than timeStampTwo.
     */
    public static int compareTimeStamp(String timeStampOne, String timeStampTwo) {
        int timeStampOneTime = parseTimeToSeconds(timeStampOne);
        int timeStampTwoTime = parseTimeToSeconds(timeStampTwo);
        return Integer.compare(timeStampOneTime, timeStampTwoTime);
    }

    /**
     * Get the time (EST).
     * @return formatted time string.
     */
    private static String getTime(Message message) {
        String zone;
        OffsetDateTime time = message.getCreationTime().minusHours(4);
        int hour = time.getHour();
        // Case 1. 24:00:00 or 00:00:00 -> 12 AM
        if (hour == 0) {
            hour += 12;
            zone = "AM";
        } else if (hour < 12) {
            // Case 2. 1:00:00 -> 1 AM
            zone = "AM";
        } else if (hour == 12) {
            // Case 3. 12:00:00 -> 12 PM
            zone = "PM";
        } else {
            // Case 4. 23:00:00 -> 11 PM
            hour %= 12;
            zone = "PM";
        }
        String minute = time.getMinute() < 10 ? "0" + time.getMinute() :
                Integer.toString(time.getMinute());
        String second = time.getSecond() < 10 ? "0" + time.getSecond() :
                Integer.toString(time.getSecond());
        return hour + ":" + minute + ":" + second
                + " " + zone;
    }

    public static int[] parseDate(String[] listStrings) {
        int month, day, year;
        try {
            month = Integer.parseInt(listStrings[0]);
            day = Integer.parseInt(listStrings[1]);
            year = Integer.parseInt(listStrings[2]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException();
        }
        return new int[]{month, day, year};
    }

    private static int parseTimeToSeconds(String timeStamp) {
        String[] timeStampValues = timeStamp.substring(16, 23).split(":");
        String zone = timeStamp.substring(25, 27);
        int hour = 0, minute = 0, second = 0;
        try {
            hour = Integer.parseInt(timeStampValues[0]);
            minute = Integer.parseInt(timeStampValues[1]);
            second = Integer.parseInt(timeStampValues[2]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (zone.equals("AM") && hour == 12) {
            return minute * 60 + second;
        } else if (zone.equals("PM") && hour != 12) {
            return (hour + 12) * 3600 + minute * 60 + second;
        } else {
            return hour * 3600 + minute * 60 + second;
        }
    }

    /**
     * Checks if the date after the "get" command is a date. Correct format is: MM/DD/YYYY.
     * @param text
     * @return
     */
    public static boolean getCheckIfValidDate(String text) {
        text = text.replace("!get ", "");
        String[] listStrings = text.split("/");
        if (listStrings.length == 3) {
            int[] dateValues = parseDate(listStrings);
            int month = dateValues[0];
            int day = dateValues[1];
            int year = dateValues[2];
            if (month < 1 || month > 12) {
                System.out.println("Invalid month. Must be between 1-12");
                return false;
            } else if (day < 1 || day > 30) {
                System.out.println("Invalid day. Must be between 1-30.");
                return false;
            } else if (year < 2010 || year > 2017) {
                System.out.println("Invalid year. Must be between 2010-2017.");
                return false;
            }
            return true;
        } else {
            System.out.println("Invalid date format.");
            return false;
        }
    }

    /**
     * Get the date.
     * @return formatted date string.
     */
    private static String getDate(Message msg) {
        OffsetDateTime date = msg.getCreationTime().minusHours(4);
        return date.getMonth() + " " + date.getDayOfMonth() + ", " + date.getYear();
    }

    /**
     * Get the time stamp of a message from an event.
     * @return formatted time stamp string.
     */
    public static String timeStamp(Message msg) {
        return "[" + getDate(msg) + "][" + MessageUtil.getTime(msg) + "]";
    }

    /**
     * Get the user and message from an event.
     * @param msg - the current message.
     * @return formatted user and message string.
     */
    public static String userMsg(Message msg) {
        String s = null;
        try {
            s = String.format("[Chan: %s] %s: %s",
                    msg.getTextChannel().getName(),
                    msg.getMember().getEffectiveName(),
                    msg.getContent());
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            System.out.println("User is banned or left. Skipping...");
        }
        return s;
    }
}
