package tree.command.analysis;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.command.music.AddCommand;
import tree.command.util.MenuUtil;
import tree.command.util.MessageUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;
import tree.commandutil.type.Command;
import tree.util.LoggerUtil;

import java.awt.*;
import java.text.DateFormatSymbols;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static tree.command.util.MessageUtil.sendError;

/**
 * Created by Admin on 7/28/2017.
 */
public class InfoCommand implements AnalysisCommand {
    private ArrayList<Member> menuMemberList = new ArrayList<>();
    private static Logger logger = LoggerFactory.getLogger(InfoCommand.class);
    private String commandName;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> menuSelectionTask;
    private MenuUtil menuUtil;

    public InfoCommand(String commandName) {
        this.commandName = commandName;
        scheduler = Executors
                .newScheduledThreadPool(1);
        menuUtil = MenuUtil.getInstance();
    }

    private Runnable createRunnable(Guild guild, MessageChannel msgChan, Member member) {
        return () -> {
            reset(msgChan);
            menuUtil.deleteMenu(msgChan, commandName);
        };
    }

    public void reset(MessageChannel msgChan) {
        menuMemberList = new ArrayList<>();
        if (menuSelectionTask.isCancelled() || menuSelectionTask.isDone()) {
            menuSelectionTask.cancel(true);
        }
    }

    public boolean waitingForChoice() {
        return !menuMemberList.isEmpty();
    }

    private void menuFollowUpCommand(String memberString, MessageChannel msgChan) {
        if (MessageUtil.checkIfInt(memberString)) {
            int optionChosen = Integer.parseInt(memberString);
            // Make sure the int is selected as one of the commands.
            if (optionChosen < 1 || optionChosen > menuMemberList.size()) {
                sendError("The number chosen is not on the list. Please search the name again.", msgChan);
                reset(msgChan);
                menuUtil.deleteMenu(msgChan, commandName);
                return;
            }
            Member member = menuMemberList.get(optionChosen - 1);
            MessageEmbed embed = getInfo(member);
            msgChan.sendMessage(embed).queue();
        } else {
            sendError("Command is not a number. Please search the name again.", msgChan);
        }
        reset(msgChan);
        menuUtil.deleteMenu(msgChan, commandName);
    }

    private MessageEmbed getInfo(Member member) {
        EmbedBuilder embed = new EmbedBuilder();
        String avatarURL = member.getUser().getEffectiveAvatarUrl();
        embed.setAuthor(member.getUser().getName() + "#" + member.getUser().getDiscriminator(),
                avatarURL, avatarURL);
        embed.setThumbnail(avatarURL);
        embed.setColor(Color.green);

        // Nickname.
        String nickNameString = member.getNickname() == null ? "" :
                CommandManager.bulletToken + " **Nickname:** " + member.getNickname() + "\n";

        // Roles.
        int counter = 0;
        String roleString = CommandManager.bulletToken + " **Roles:** ";
        for (Role role : member.getRoles()) {
            roleString += "``" + role.getName() + "`` ";
        }
        roleString += "\n";

        // Guild join date.
        OffsetDateTime guildJoinDate = member.getJoinDate();
        String guildMonthString = new DateFormatSymbols().getMonths()[guildJoinDate.getMonth().getValue() - 1];
        String guildJoinDateString = CommandManager.bulletToken + " **Guild Join Date:** " + guildMonthString
                + " " + guildJoinDate.getDayOfMonth() + ", " + guildJoinDate.getYear() + "\n";

        // Discord join date.
        OffsetDateTime discordJoinDate = member.getUser().getCreationTime();
        String discordMonthString = new DateFormatSymbols().getMonths()[discordJoinDate.getMonth().getValue() - 1];
        String discordJoinDateString = CommandManager.bulletToken + " **Discord Join Date:** " + discordMonthString
                + " " + discordJoinDate.getDayOfMonth() + ", " + discordJoinDate.getYear() + "\n";
        embed.setDescription(nickNameString + roleString + guildJoinDateString + discordJoinDateString);
        return embed.build();

    }



    private int getMember(Map<Long, Member> memberMap, Guild guild, Member member, MessageChannel msgChan) {
        if (memberMap.isEmpty()) {
            // No matches found.
            return -1;
        } else if (memberMap.size() == 1) {
            // Only one match found.
            Member currMember = memberMap.values().iterator().next();
            MessageEmbed embed = getInfo(currMember);
            msgChan.sendMessage(embed).queue();
            return 0;
        } else {
            // More than one match found. Menu needed.
            int currIndex = 1;
            String menuSelection = "Multiple users found. Type ``" +
                    CommandManager.botToken +
                    commandName +
                    " n`` to select, where ``n`` is your choice. \n\n";

            int counter = 0;
            for (Map.Entry<Long, Member> entry : memberMap.entrySet()) {
                Member currMember = entry.getValue();
                User user = currMember.getUser();
                String nickName = currMember.getNickname() == null ? " None" : currMember.getNickname();
                menuSelection += Integer.toString(currIndex++) + ") **" + user.getName() +
                        "#" + user.getDiscriminator() + "** (Nickname: " + nickName + ")\n";
                if (++counter >= 5) {
                    menuSelection += "\n plus " + (memberMap.size() - counter) + "more ..."; //TODO: If need more, type info more
                    break;
                }
            }
            for (Map.Entry<Long, Member> entry : memberMap.entrySet()) {
                menuMemberList.add(entry.getValue());
            }
            menuSelectionTask = menuUtil.createMenuTask(createRunnable(guild, msgChan, member), menuSelectionTask, 15);
            menuUtil.setUserId(commandName, msgChan, member);
            msgChan.sendMessage(menuSelection).queue(m -> menuUtil.setMenuId(commandName, msgChan, m));
            return 0;
        }
    }


    public void getDateJoined(Guild guild, MessageChannel msgChan, Message message, Member member, String search) {

        if (!menuMemberList.isEmpty()) {
            long userId = member.getUser().getIdLong();
            long lastUserId = menuUtil.getUserId(commandName, msgChan);
            if (userId != lastUserId) {
                message.addReaction("\u274E").queue();
            } else {
                menuFollowUpCommand(search, msgChan);
            }
            return;
        }

        search = search.replaceAll("@", "");
        List<Member> memberListEffective = guild.getMembersByEffectiveName(search, true);
        List<Member> memberListUser = guild.getMembersByName(search, true);
        Map<Long, Member> combinedMap = new HashMap<>();
        for (Member memEff : memberListEffective) {
            combinedMap.put(memEff.getUser().getIdLong(), memEff);
        }
        for (Member memUser : memberListUser) {
            combinedMap.put(memUser.getUser().getIdLong(), memUser);
        }

        if (getMember(combinedMap, guild, member, msgChan) == -1) {
            MessageUtil.sendError("No users found with name: " + search, msgChan);
        }
    }

    public String combine(String[] args) {
        String search = "";
        for (int i = 1; i < args.length; i++) {
            search += args[i] + " ";
        }
        return search.trim();
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length <= 1) {
            LoggerUtil.logMessage(logger, message, "No argument entered.");
            MessageUtil.sendError("No argument entered.", msgChan);
            return;
        }

        String search = combine(args);
        getDateJoined(guild, msgChan, message, member, search);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName +
                " [name] ``: Returns your join date to the server. If you have left/been kicked \n" +
                "the join date will be the day you rejoined.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Deprecated
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
                sendError("Wrap the name to search with <>. For example, " +
                        CommandManager.botToken +
                        getCommandName() +
                        " <palat>", msgChan);
                return 0;
            } else if (c == '>') {
                sendError("Wrap the name to search with <>. For example, " +
                        CommandManager.botToken +
                        getCommandName() +
                        " <palat>", msgChan);
                return 0;
            }
        }
        return 2;
    }
}
