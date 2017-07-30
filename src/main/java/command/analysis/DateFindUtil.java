package command.analysis;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.text.DateFormatSymbols;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Admin on 7/28/2017.
 */
public class DateFindUtil {
    private List<Member> menuMemberList = new ArrayList<>();

    private List<Member> findMembers(MessageReceivedEvent event, String memberString) {
        Guild guild = event.getGuild();
        List<Member> memberList = guild.getMembersByName(memberString, true);
        return memberList;
    }

    public boolean checkIfInt(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public void sendError(String error, MessageChannel msgChan) {
        System.out.println(error);
        msgChan.sendMessage(error).queue();
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

    public void getDateJoined(MessageReceivedEvent event) {
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
                memberString = parameters[0].replaceAll("<", "")
                        .replaceAll("@", "");
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
}
