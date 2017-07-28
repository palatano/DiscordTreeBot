package com.jda.util;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;

import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Admin on 7/20/2017.
 */
public class MessageUtil {

    public static boolean isInvalidFutureDate(Calendar cal, OffsetDateTime msgDate) {
        int commandYear = cal.get(Calendar.YEAR);
        int msgYear = msgDate.getYear();
        int commandDay = cal.get(Calendar.DAY_OF_YEAR);
        int msgDay = msgDate.getDayOfYear();
        return cal.get(Calendar.DAY_OF_YEAR) > msgDate.getDayOfYear() && // ... 302 > 203 TRUE, 302 > 365 FALSE
                cal.get(Calendar.YEAR) > msgDate.getYear(); // 2016 > 2017 FALSE, 2016 > 2017 FALSE,
        /*
        Case 1: 8/1/2017 -> True. Error.
        Case 2: 7/23/2017 -> False. Continue.
        Case 3: 11/12/2016 -> False. Continue.
        Case 4: 11/11/2016 -> No more history. Error.
         */
    }

    public static boolean msgDateAfterCal(Calendar cal, OffsetDateTime msgDate) {
        return cal.get(Calendar.DAY_OF_YEAR) < msgDate.getDayOfYear() || // 302 < 203 FALSE -> 302 < 364 TRUE -> 302 FALSE
                cal.get(Calendar.YEAR) < msgDate.getYear(); // 2016 < 2017 TRUE -> 2016 FALSE
        /*
        Case 2: 07/23/2017 -> False. Continue.
        Case 3: 11/12/2016 -> True. Stop.
        Case 4: 11/11/2016 -> False. Should not reach.
         */
    }

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

    public static Calendar parseDate(String[] listStrings) {
        int month, day, year;
        int dayStringLength = listStrings[0].length();
        listStrings[0] = listStrings[0].substring(dayStringLength - 2, dayStringLength);
        listStrings[2] = listStrings[2].substring(0, 4);
        try {
            month = Integer.parseInt(listStrings[0]);
            day = Integer.parseInt(listStrings[1]);
            year = Integer.parseInt(listStrings[2]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException();
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        cal.set(year, month - 1, day);
        return cal;
    }

    private static int parseTimeToSeconds(String timeStamp) {
        String[] timeStampValues = timeStamp.substring(16, 23).split(":");
        String zone = timeStamp.substring(24, 26);
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
        String[] splitCommandValues = text.trim().split(" ");
        String[] listStrings = splitCommandValues[1].split("/");
        if (listStrings.length == 3) {
            Calendar cal = parseDate(listStrings);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int year = cal.get(Calendar.YEAR);
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

    public static boolean checkIfWrite(String content) {
        content = content.replaceFirst(" ", "");
        String[] contentParts = content.split(" ");
        if (contentParts.length != 2 || !contentParts[1].equals("--write")) {
            System.out.println("Write flag not entered correctly. Will not write to file.");
            return false;
        } else if (contentParts[1].trim().equals("")) {
            return false;
        }
        return true;
    }
    /**
     * Get the time stamp of a message from an event.
     * @return formatted time stamp string.
     */
    public static String timeStamp(Message msg) {
        return "[" + getDate(msg) + "][" + MessageUtil.getTime(msg) + " EST]";
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
            System.out.println("User is banned or left. Skipping...");
        }
        return s;
    }
}
