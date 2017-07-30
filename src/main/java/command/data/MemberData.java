package command.data;


import net.dv8tion.jda.core.entities.Message;

import java.lang.reflect.Member;

/**
 * Created by Admin on 7/23/2017.
 */
public class MemberData {
    String timeStamp;
    String content;
    String userName;
    String nickName;

    public MemberData(String timeStamp, String content, String userName, String nickName) {
        this.timeStamp = timeStamp;
        this.content = content;
        this.userName = userName;
        this.nickName = nickName;
    }

}
