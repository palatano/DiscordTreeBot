package com.jda.main;

import com.jda.util.DataUtil;
import com.jda.util.DiscordReadUtil;
import com.jda.util.MessageUtil;
import com.jda.util.SerializableMessageHistory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * Created by Valued Customer on 7/20/2017.
 */
public class MainApplication extends ListenerAdapter {
    private DiscordReadUtil discUtil;
    private DataUtil dataUtil;
    private static final String[] channelIDList = {"249764885246902272", "247929469552033792",
    "269577202016845824", "247135478069854209", "247248468626636800",
    "248243893273886720", "247134894558281730"};
    private static final String[] rulesInfo = {"247109092567547905"};
    private static boolean followUpCommand = false;
    private List<Member> menuMemberList = new ArrayList<>();

    private static OkHttpClient.Builder setupBuilder(OkHttpClient.Builder builder) {
        builder = builder.connectTimeout(60000, TimeUnit.MILLISECONDS);
        builder = builder.readTimeout(60000, TimeUnit.MILLISECONDS);
        builder = builder.writeTimeout(60000, TimeUnit.MILLISECONDS);
        return builder;
    }

    public static void main(String[] args)
            throws LoginException, RateLimitedException, InterruptedException {
        /* Get the credentials file. */
        if (args[0] == null) {
            System.out.println("No filename supplied. Error.");
        }
        DataUtil dataUtil = new DataUtil();
        Map<String, Object> creds = dataUtil.retrieveCreds(args[0]);
        dataUtil.setCreds(creds);
        /* Create the bot and add the listeners for the bot. */
        String token = (String) creds.get("token");
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        JDA jda = new JDABuilder(AccountType.BOT)
                .setHttpClientBuilder(setupBuilder(builder))
                .setToken(token)
                .buildBlocking();
        jda.addEventListener(new MainApplication(dataUtil));
    }

    public MainApplication(DataUtil dataUtil) {
        this.dataUtil = dataUtil;
        discUtil = new DiscordReadUtil(dataUtil);
    }


    /**
     * Get the current message based on the !curr event.
     * @param event - event of a received message.
     */
    private void getCurrMessage(MessageReceivedEvent event) {
        String publicMessage = String.format(MessageUtil.timeStamp(event.getMessage()) +
                       MessageUtil.userMsg(event.getMessage()));
        String privateMessage = String.format("[PM] %s: %s\n", event.getAuthor().getName(),
                event.getMessage().getContent());
        if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.printf(privateMessage);
        }
        else {
            System.out.printf(publicMessage);
        }
    }

    /**
     * Confirm that the user is palat, so the commands won't be abused.
     * @param event - event of a received message.
     * @return boolean depending if user is palat.
     */
    private boolean isMainUser(MessageReceivedEvent event) {
        String name = event.getAuthor().getId();
        return name.equals("192372494202568706");
    }

    /*
    Two types: Members and Users.
    Case 1: !dateJoined -> Error missing argument GOOD
    Case 2: !dateJoined palat -> OK
    Case 3: !dateJoined palat palat -> Error,  extra argument. GOOD
    Case 4: !dateJoined "palat" -> OK
    Case 5: !dateJoined "palat name" -> OK (for valid user)
    Case 6: !dateJoined "palat -> Error missing quotation GOOD
    Case 7: !dateJoined palat" -> Error missing quotation GOOD
    Case 8: !dateJoined "palat" asdf|"palat"|"" -> Error extra argument. GOOD
    Case 9: !dateJoined palat (x2) -> Menu.
         */

    private void sendError(String error, MessageChannel msgChan) {
        System.out.println(error);
        msgChan.sendMessage(error).queue();
    }

    private List<Member> findMembers(MessageReceivedEvent event, String memberString) {
        Guild guild = event.getGuild();
        List<Member> memberList = guild.getMembersByName(memberString, true);
        return memberList;
    }

    private boolean checkIfInt(String s) {
        try {
            int i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private int isWrappedCommand(String memberString, MessageChannel msgChan) {
        for (int stringIndex = 0; stringIndex < memberString.length(); stringIndex++) {
            char c = memberString.charAt(stringIndex);
            if (c == '<') {
                while(++stringIndex < memberString.length()) {
                    char c2 = memberString.charAt(stringIndex);
                    if (c2 == '>') {
                        return 1;
                    }
                }
                sendError("Wrap the name to search with <>. For example, !dateJoined <palat>", msgChan);
                return 0;
            } else if (c == '>') {
                sendError("Wrap the name to search with <>. For example, !dateJoined <palat>", msgChan);
                return 0;
            }
        }
        return 2;
    }

    private void menuFollowUpCommand(String memberString, MessageChannel msgChan) {
        if (checkIfInt(memberString)) {
            int optionChosen = Integer.parseInt(memberString);
            // Make sure the int is selected as one of the commands.
            if (optionChosen < 1 || optionChosen > menuMemberList.size()) {
                sendError("The number chosen is not on the list. Please search the name again.", msgChan);
                menuMemberList = new ArrayList<>();
                return;
            }
            Member member = menuMemberList.get(optionChosen - 1);
            OffsetDateTime joinDate = member.getJoinDate();
            String monthString = new DateFormatSymbols().getMonths()[joinDate.getMonth().getValue() - 1];
            Message message = new MessageBuilder().append("Your join date is " + monthString
                    + " " + joinDate.getDayOfMonth() + ", " + joinDate.getYear() + ".").build();
            msgChan.sendMessage(message).queue();
            menuMemberList = new ArrayList<>();
        } else {
            sendError("Command is not a number. Please search the name again.", msgChan);
            menuMemberList = new ArrayList<>();
        }
        return;
    }

    private void getMember(List<Member> memberList, String memberString, MessageChannel msgChan) {
        if (memberList.isEmpty()) {
            // No matches found.
            sendError("No users found with name: " + memberString + ". Make sure the name entered is " +
                    "the account name and not a nickname.", msgChan);
            return;
        } else if (memberList.size() == 1) {
            // Only one match found.
            OffsetDateTime joinDate = memberList.get(0).getJoinDate();
            String monthString = new DateFormatSymbols().getMonths()[joinDate.getMonth().getValue() - 1];
            Message message = new MessageBuilder().append("Your join date is " + monthString
                    + " " + joinDate.getDayOfMonth() + ", " + joinDate.getYear() + ".").build();
            msgChan.sendMessage(message).queue();
            return;
        } else {
            // More than one match found. Menu needed.
            int currIndex = 1;
            String menuSelection = "Multiple users found. Please select the option with the correct user," +
                    " typing !dateJoined n, where n is your option:\n";
            for (Member currMember : memberList) {
                User user = currMember.getUser();
                menuSelection += Integer.toString(currIndex++) + ": " + user.getName() +
                        "#" + user.getDiscriminator() + "\n";
            }
            msgChan.sendMessage(menuSelection).queue();
            menuMemberList = memberList;
            return;
        }
    }

    private void getDateJoined(MessageReceivedEvent event) {
        MessageChannel msgChan = event.getTextChannel();
        String msgContent = event.getMessage().getContent().trim();
        String[] memberStringCommand = msgContent.split(" ");

        // Check if valid number of arguments:
        if (memberStringCommand.length == 1) {
            sendError("No parameter (name) entered after !dateJoined command.", msgChan);
            return;
        }

        // Split the string with the first whitespace.
        for (int stringIndex = 0; stringIndex < msgContent.length(); stringIndex++) {
            char c = msgContent.charAt(stringIndex);
            if (c == ' ') {
                memberStringCommand = new String[] {msgContent.substring(0, stringIndex),
                        msgContent.substring(stringIndex + 1, msgContent.length())};
                break;
            }
        }
        if (!menuMemberList.isEmpty()) {
            menuFollowUpCommand(memberStringCommand[1], event.getTextChannel());
            return;
        }

        String[] parameters;
        String memberString = null;
        List<Member> memberList = null;
        int isWrapped = isWrappedCommand(memberStringCommand[1], msgChan);
        switch (isWrapped) {
            // There was an error wrapping the name with <>.
            case 0:
                return;
            // The name is wrapped with <>.
            case 1:
                parameters = memberStringCommand[1].split(">");
                if (parameters.length > 1) {
                    sendError("Only one parameter (name) at a time. If you have a name with spaces," +
                            " surround the name with <>. e.g. !dateJoined <palat>", msgChan);
                    return;
                }
                memberString = parameters[0].replaceAll("<", "").replaceAll("@", "");
                memberList = findMembers(event, memberString);
                getMember(memberList, memberString, event.getTextChannel());
                return;
            // The name is not wrapped with <>.
            case 2:
                parameters = memberStringCommand[1].split(" ");
                if (parameters.length > 1) {
                    sendError("Only one parameter (name) at a time. If you have a name with spaces," +
                            " surround the name with <>. e.g. !dateJoined <palat>", msgChan);
                    return;
                }
                memberString = parameters[0].replaceAll("<", "").
                        replaceAll("@", "");
                memberList = findMembers(event, memberString);
                getMember(memberList, memberString, event.getTextChannel());

                break;
        }
    }

    @Override
    /**
     * The main message listener for testing purposes.
     * @param{event} - event of a received message.
     */
    public void onMessageReceived(MessageReceivedEvent event) {
        /* Allow only the admin to access the following commands. */
        String msgContent = event.getMessage().getContent();
        if (!isMainUser(event)) {
            if (msgContent.startsWith("!dateJoined")) {
                getDateJoined(event);
            }
            return;
        }
        if (msgContent.equals("!test")) {
            /* Test if reaction word is created. */
            event.getMessage().addReaction("\uD83D\uDE02").queue();
        } else if (msgContent.startsWith("!writeSingle") &&
                MessageUtil.getCheckIfValidDate(msgContent)) {
            /* Get Message History from channel. */
            discUtil.getDailyHistory(event.getTextChannel(), msgContent,
                    MessageUtil.checkIfWrite(msgContent));
        } else if (msgContent.equals("!curr")) {
            /* Get current message from channel. */
            getCurrMessage(event);
        } else if (msgContent.startsWith("!writeAll") &&
                MessageUtil.getCheckIfValidDate(msgContent)) {
            dataUtil.resetMap();
            for (String channelID : rulesInfo) {
                // Get the channel and message history to iterate over.
                TextChannel channel = event.getGuild().getTextChannelById(channelID);
                discUtil.getDailyHistory(channel, "!get " +
                        msgContent.replace("!writeData ", ""), false);
                // Set the message history to null to reset for the next operation.
            }
            dataUtil.writeAllChannelDataExcel(event.getTextChannel());
        } else if (msgContent.equals("!nug")) {
            MessageChannel msgChan = event.getTextChannel();
            dataUtil.writeRandomNugPhoto(event.getTextChannel());
        } else if (msgContent.startsWith("!dateJoined")) {
            getDateJoined(event);
            return;
        }
    }
}
