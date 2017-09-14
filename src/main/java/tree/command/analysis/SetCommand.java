package tree.command.analysis;

import com.google.cloud.language.v1.PartOfSpeech;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import tree.Config;
import tree.command.data.MenuSelectionInfo;
import tree.command.util.MessageUtil;
import tree.command.util.api.YoutubeMusicUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;

import javax.xml.soap.Text;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by Valued Customer on 8/29/2017.
 */
public class SetCommand implements AnalysisCommand {
    private String commandName;
    private Map<Guild, Map<Long, MenuSelectionInfo>> guildToUserMap;
    private Map<Guild, Map<Long, Config.SetType>> guildSetTypeMap;

    public SetCommand(String commandName) {
        this.commandName = commandName;
        guildToUserMap = new HashMap<>();
        guildSetTypeMap = new HashMap<>();
    }

    private Runnable createRunnable(Guild guild, MessageChannel msgChan, Member member) {
        return () -> {
            deleteSelectionEntry(guild, member.getUser().getIdLong());
        };
    }

    private void addSelectionEntry(Guild guild, long userId,
                                   MenuSelectionInfo msInfo, Config.SetType type) {
        if (!guildToUserMap.containsKey(guild)) {
            guildToUserMap.put(guild, new HashMap<>());
        }

        if (!guildSetTypeMap.containsKey(guild)) {
            guildSetTypeMap.put(guild, new HashMap<>());
        }

        Map<Long, MenuSelectionInfo> userSelectionMap =
                guildToUserMap.get(guild);
        userSelectionMap.put(userId, msInfo);
        guildSetTypeMap.get(guild).put(userId, type);
    }

    private void deleteSelectionEntry(Guild guild, long userId) {
        if (!guildToUserMap.containsKey(guild) || !guildSetTypeMap.containsKey(guild)) {
                System.out.println("Bot is attempting to remove a user and its selection from" +
                        " a non-existent guild.");
                return;
            }
            Map<Long, MenuSelectionInfo> userSelectionMap = guildToUserMap.get(guild);
            if (!userSelectionMap.containsKey(userId)) {
                System.out.println("Bot is attempting to remove a non-existent user.");
                return;
            }
            MenuSelectionInfo msInfo = userSelectionMap.get(userId);
            ScheduledFuture<?> task = msInfo.getTask();
            long menuId = msInfo.getMenu().getIdLong();
            MessageChannel msgChan = msInfo.getChannel();
            if (menuId != 0) {
                msgChan.deleteMessageById(menuId).queue();
            }
            userSelectionMap.remove(userId);
        if (!task.isCancelled() || !task.isDone()) {
            task.cancel(true);
        }
    }

    private String combineArgument(String[] args) {
        String out = "";
        for (int i = 2; i < args.length; i++) {
            out += args[i] + " ";
        }
        return out.trim();
    }

    private boolean isAdmin(long guildId, Member member) {
        long userId = member.getUser().getIdLong();
        if (Config.isOwner(userId) || member.isOwner()) {
            return true;
        }
        if (Config.guildAdmins.containsKey(guildId) &&
                Config.guildAdmins.get(guildId)
                        .contains(member.getUser().getIdLong())) {
            return true;
        }
        return false;
    }
    private Map<Long, Member> createMemberMap(Guild guild, String search) {
        List<Member> memberListEffective = guild.getMembersByEffectiveName(search, true);
        List<Member> memberListUser = guild.getMembersByName(search, true);
        Map<Long, Member> combinedMap = new HashMap<>();
        for (Member memEff : memberListEffective) {
            combinedMap.put(memEff.getUser().getIdLong(), memEff);
        }
        for (Member memUser : memberListUser) {
            combinedMap.put(memUser.getUser().getIdLong(), memUser);
        }
        return combinedMap;
    }

    private String createMenu(List<?> list) {
        String out = "Please select from the menu below," +
                " using ``;set #``:\n\n";
        int index = 1;
        Object type = list.get(0);

        if (type instanceof TextChannel) {
            List<TextChannel> textChannelList = (List<TextChannel>) list;
            for (TextChannel textChannel : textChannelList) {
                out += index++ + ") " + textChannel.getName() + "\n";
            }
            return out;
        } else if (type instanceof VoiceChannel) {
            List<VoiceChannel> voiceChannelList = (List<VoiceChannel>) list;
            for (VoiceChannel voiceChannel : voiceChannelList) {
                out += index++ + ") " + voiceChannel.getName() + "\n";
            }
            return out;
        } else if (type instanceof Member) {
            List<Member> memberList = (List<Member>) list;
            for (Member mem : memberList) {
                out += index++ + ") " + mem.getUser().getName() + "#" + mem.getUser().getDiscriminator() + "\n";
            }
            return out;
        } else if (type instanceof Role) {
            List<Role> roleList = (List<Role>) list;
            for (Role role : roleList) {
                out += index++ + ") " + role.getName() + "\n";
            }
            return out;
        }
        return null;
    }

    private void scheduleAction(Message menu, Guild guild,
                                MessageChannel msgChan, Member member,
                                Config.SetType type, List<Long> list) {
        // Send the message, store the menu, and update a variable such that
        // the user should return a response.
        YoutubeMusicUtil ytUtil = YoutubeMusicUtil.getInstance();
        ScheduledFuture<?> task = ytUtil.getMenuUtil().createMenuTask(createRunnable(guild, msgChan, member),
                null, 15);
        MenuSelectionInfo msInfo = new MenuSelectionInfo(menu, msgChan,
                list, task);
        addSelectionEntry(guild, member.getUser().getIdLong(), msInfo, type);
    }

    private long getIdToSet(Guild guild, MessageChannel msgChan, long userId, int choice) {
        List<Long> list = null;
        list = (List<Long>) guildToUserMap.get(guild).get(userId).getSongsToChoose();
        if (choice < 1 || choice >= list.size() + 1) {
            MessageUtil.sendError("Enter a valid number from the list.", msgChan);
            return -1;
        }
        long id = list.get(choice - 1);
        return id;
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        // Before using the set command, check if the guild exists and the user is an admin.
        if (!Config.isAdmin(guild, member)) {
            message.addReaction("\u274E").queue();
            return;
        }

        long guildId = guild.getIdLong();
        long userId = member.getUser().getIdLong();
        String type = args[1];

        // If the ;;set is followed by an integer, a menu should be open.
        // Otherwise, throw an error.
        if (MessageUtil.checkIfInt(type)) {
            if (args.length != 2) {
                MessageUtil.sendError("Too many arguments.", msgChan);
                return;
            }

            Map<Long, MenuSelectionInfo> userSelectionMap =
                    guildToUserMap.get(guild);
            if (userSelectionMap != null && userSelectionMap.containsKey(userId)) {
                int choice = Integer.parseInt(type);
                long id = getIdToSet(guild, msgChan, userId, choice);
                if (id == -1) {
                    return;
                }

                Config.SetType typeAfterMenu = guildSetTypeMap.get(guild).get(userId);
                Config.setGuildInfo(typeAfterMenu, guild, id);

                ScheduledFuture<?> task = guildToUserMap.get(guild).get(userId).getTask();
                if (!task.isCancelled() || !task.isDone()) {
                    task.cancel(true);
                }
                deleteSelectionEntry(guild, userId);
                message.addReaction("\u2705").queue();
            } else {
                message.addReaction("\u274E").queue();
            }
            return;
        }

        String param = combineArgument(args).replaceFirst("#", "")
                .replaceFirst("@", "");

        // Create menu.
        // Save id.
        // Create runnable to delete menu. Cancel if it doesnt exist.
        // If menu runs out before confirmation, delete and cancel.
        // If option is selected out of the list (MAX: 5 results), delete and cancel.

        switch (type) {
            case "text_channel":
                List<TextChannel> textChannelList = guild.getTextChannelsByName(param, true);

                // Check if there are multiple channels or not.
                TextChannel textChan = null;
                if (textChannelList.isEmpty()) {
                    MessageUtil.sendError("No channels found to set.", msgChan);
                    return;
                } else if (textChannelList.size() == 1) {
                    textChan = textChannelList.get(0);
                } else {
                    String menuString = createMenu(textChannelList);
                    Message menu = msgChan.sendMessage(menuString).complete();

                    List<Long> listOfIds = new ArrayList<>();
                    for (TextChannel textChannel : textChannelList) {
                        listOfIds.add(textChannel.getIdLong());
                    }

                    scheduleAction(menu, guild, msgChan,
                            member, Config.SetType.TEXT_CHANNEL, listOfIds);
                    return;
                }
                Config.setGuildInfo(Config.SetType.TEXT_CHANNEL, guild, textChan.getIdLong());
                message.addReaction("\u2705").queue();
                break;

            case "voice_channel":
                List<VoiceChannel> voiceChannelList = guild.getVoiceChannelsByName(param, true);

                // Check if there are multiple channels.
                VoiceChannel voiceChan = null;
                if (voiceChannelList.isEmpty()) {
                    MessageUtil.sendError("No channels found to set.", msgChan);
                    return;
                } else if (voiceChannelList.size() == 1) {
                    voiceChan = voiceChannelList.get(0);
                } else {
                    String menuString = createMenu(voiceChannelList);
                    Message menu = msgChan.sendMessage(menuString).complete();
                    List<Long> listOfIds = new ArrayList<>();

                    for (VoiceChannel voiceChannel : voiceChannelList) {
                        listOfIds.add(voiceChannel.getIdLong());
                    }

                    scheduleAction(menu, guild, msgChan,
                            member, Config.SetType.VOICE_CHANNEL, listOfIds);
                    return;
                }

                Config.setGuildInfo(Config.SetType.VOICE_CHANNEL, guild, voiceChan.getIdLong());
                message.addReaction("\u2705").queue();

                break;

            case "admin":
                List<Member> memberList = new ArrayList<>(createMemberMap(guild, param).values());

                // Check if there are multiple users.
                Member mem = null;
                if (memberList.isEmpty()) {
                    MessageUtil.sendError("No admins found to set.", msgChan);
                    return;
                } else if (memberList.size() == 1) {
                    mem = memberList.get(0);
                } else {
                    String menuString = createMenu(memberList);
                    Message menu = msgChan.sendMessage(menuString).complete();

                    List<Long> listOfIds = new ArrayList<>();
                    for (Member memberForId : memberList) {
                        listOfIds.add(memberForId.getUser().getIdLong());
                    }

                    scheduleAction(menu, guild, msgChan,
                            member, Config.SetType.ADMIN, listOfIds);
                    return;
                }

                Config.setGuildInfo(Config.SetType.ADMIN, guild, mem.getUser().getIdLong());
                message.addReaction("\u2705").queue();
                break;

            case "music_role":
                List<Role> roleList = guild.getRolesByName(param, true);

                // Check if there are multiple users.
                Role role = null;
                if (roleList.isEmpty()) {
                    MessageUtil.sendError("No roles found to set.", msgChan);
                    return;
                } else if (roleList.size() == 1) {
                    role = roleList.get(0);
                } else {
                    String menuString = createMenu(roleList);
                    Message menu = msgChan.sendMessage(menuString).complete();

                    List<Long> listOfIds = new ArrayList<>();

                    for (Role roleForId : roleList) {
                        listOfIds.add(roleForId.getIdLong());
                    }

                    scheduleAction(menu, guild, msgChan,
                            member, Config.SetType.ADMIN, listOfIds);
                    return;
                }

                Config.setGuildInfo(Config.SetType.MUSIC_ROLE, guild, role.getIdLong());
                message.addReaction("\u2705").queue();

                break;
            default:
                MessageUtil.sendError("Invalid set parameters." +
                        " Valid parameters to be set are ``text_channel``," +
                        "``voice_channel``," +
                        "``admin``, or" +
                        "``music_role``.", msgChan);
                break;
        }
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + " [text_channel | voice_channel | admin | music_role] [name]``:" +
                " Allows the admins to set the desired category with a name.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }
}
