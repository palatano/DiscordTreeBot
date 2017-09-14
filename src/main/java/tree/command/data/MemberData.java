package tree.command.data;


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

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getContent() {
        return content;
    }


    public String getNickName() {
        return nickName;
    }


}
