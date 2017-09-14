package tree.command.analysis;

import net.dv8tion.jda.core.entities.*;
import tree.Config;
import tree.command.data.MenuSelectionInfo;
import tree.command.util.MessageUtil;
import tree.command.util.api.YoutubeMusicUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.AnalysisCommand;
import tree.commandutil.type.TextCommand;

import javax.xml.soap.Text;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * Created by Valued Customer on 8/30/2017.
 */
public class UnsetCommand implements AnalysisCommand {
    private String commandName;
    private Map<Guild, Map<Long, MenuSelectionInfo>> guildToUserMap;
    private Map<Guild, Map<Long, Config.SetType>> guildSetTypeMap;

    public UnsetCommand(String commandName) {
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

    private long getIdToRemove(Guild guild, MessageChannel msgChan, long userId, int choice) {
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
                long id = getIdToRemove(guild, msgChan, userId, choice);
                if (id == -1) {
                    return;
                }

                Config.SetType typeAfterMenu = guildSetTypeMap.get(guild).get(userId);
                Config.removeGuildInfo(typeAfterMenu, guild, id);

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


        switch (type) {
            case "text_channel":
                if (!Config.getGuildAllowedTextChannels().containsKey(guildId)) {
                    MessageUtil.sendError("No text channels to remove.", msgChan);
                    return;
                }

                Set<Long> textChannelSet = Config.getGuildAllowedTextChannels().get(guildId);
                List<TextChannel> textChannelList = guild.getTextChannelsByName(param, true);
                List<TextChannel> filteredTextChannelList = textChannelList.stream()
                        .filter(textChannel -> textChannelSet.contains(textChannel.getIdLong()))
                        .collect(Collectors.toList());

                // Check if there are multiple channels or not.
                TextChannel textChan = null;
                if (filteredTextChannelList.isEmpty()) {
                    MessageUtil.sendError("No channels found to set.", msgChan);
                    return;
                } else if (filteredTextChannelList.size() == 1) {
                    textChan = filteredTextChannelList.get(0);
                } else {
                    String menuString = createMenu(filteredTextChannelList);
                    Message menu = msgChan.sendMessage(menuString).complete();

                    List<Long> listOfIds = new ArrayList<>();
                    for (TextChannel textChannel : filteredTextChannelList) {
                        listOfIds.add(textChannel.getIdLong());
                    }

                    scheduleAction(menu, guild, msgChan,
                            member, Config.SetType.TEXT_CHANNEL, listOfIds);
                    return;
                }
                Config.removeGuildInfo(Config.SetType.TEXT_CHANNEL, guild, textChan.getIdLong());
                message.addReaction("\u2705").queue();
                break;

            case "voice_channel":
                List<VoiceChannel> voiceChannelList = guild.getVoiceChannelsByName(param, true);
                Set<Long> voiceChannelSet = Config.getGuildAllowedVoiceChannels().get(guildId);
                List<VoiceChannel> filteredVoiceChannelList = voiceChannelList.stream()
                        .filter(voiceChannel -> voiceChannelSet.contains(voiceChannel.getIdLong()))
                        .collect(Collectors.toList());

                // Check if there are multiple channels.
                VoiceChannel voiceChan = null;
                if (filteredVoiceChannelList.isEmpty()) {
                    MessageUtil.sendError("No channels found to set.", msgChan);
                    return;
                } else if (filteredVoiceChannelList.size() == 1) {
                    voiceChan = filteredVoiceChannelList.get(0);
                } else {
                    String menuString = createMenu(filteredVoiceChannelList);
                    Message menu = msgChan.sendMessage(menuString).complete();
                    List<Long> listOfIds = new ArrayList<>();

                    for (VoiceChannel voiceChannel : filteredVoiceChannelList) {
                        listOfIds.add(voiceChannel.getIdLong());
                    }

                    scheduleAction(menu, guild, msgChan,
                            member, Config.SetType.VOICE_CHANNEL, listOfIds);
                    return;
                }

                Config.removeGuildInfo(Config.SetType.VOICE_CHANNEL, guild, voiceChan.getIdLong());
                message.addReaction("\u2705").queue();

                break;

            case "admin":
                List<Member> memberList = new ArrayList<>(createMemberMap(guild, param).values());
                Set<Long> adminSet = Config.getGuildAdmins().get(guildId);
                List<Member> filteredMemberList = memberList.stream()
                        .filter(m -> adminSet.contains(m.getUser().getIdLong()))
                        .collect(Collectors.toList());

                // Check if there are multiple users.
                Member mem = null;
                if (filteredMemberList.isEmpty()) {
                    MessageUtil.sendError("No admins found to set.", msgChan);
                    return;
                } else if (filteredMemberList.size() == 1) {
                    mem = filteredMemberList.get(0);
                } else {
                    String menuString = createMenu(filteredMemberList);
                    Message menu = msgChan.sendMessage(menuString).complete();

                    List<Long> listOfIds = new ArrayList<>();
                    for (Member memberForId : filteredMemberList) {
                        listOfIds.add(memberForId.getUser().getIdLong());
                    }

                    scheduleAction(menu, guild, msgChan,
                            member, Config.SetType.ADMIN, listOfIds);
                    return;
                }

                Config.removeGuildInfo(Config.SetType.ADMIN, guild, mem.getUser().getIdLong());
                message.addReaction("\u2705").queue();
                break;

            case "music_role":
                List<Role> roleList = guild.getRolesByName(param, true);
                Set<Long> roleSet = Config.getGuildAdmins().get(guildId);
                List<Role> filteredRoleList = roleList.stream()
                        .filter(role -> roleSet.contains(role.getIdLong()))
                        .collect(Collectors.toList());

                // Check if there are multiple users.
                Role role = null;
                if (roleList.isEmpty()) {
                    MessageUtil.sendError("No roles found to set.", msgChan);
                    return;
                } else if (roleList.size() == 1) {
                    role = roleList.get(0);
                } else {
                    String menuString = createMenu(filteredRoleList);
                    Message menu = msgChan.sendMessage(menuString).complete();
                    List<Long> listOfIds = new ArrayList<>();

                    for (Role roleForId : roleList) {
                        listOfIds.add(roleForId.getIdLong());
                    }

                    scheduleAction(menu, guild, msgChan,
                            member, Config.SetType.ADMIN, listOfIds);
                    return;
                }

                Config.removeGuildInfo(Config.SetType.MUSIC_ROLE, guild, role.getIdLong());
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
                " Allows the admins to unset the desired category with a name.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

}
