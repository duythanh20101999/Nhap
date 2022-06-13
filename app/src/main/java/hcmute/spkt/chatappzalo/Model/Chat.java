package hcmute.spkt.chatappzalo.Model;

public class Chat {
    //id người gửi, id người nhận, nội dung tin nhắn
    String sender, receiver, message, type;

    //kiểm tra tin nhắn đã đọc hay chưa
    boolean isseen;

    public Chat(String sender, String receiver, String message, boolean isseen, String type) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.isseen = isseen;
        this.type = type;
    }

    public Chat() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isIsseen() {
        return isseen;
    }

    public void setIsseen(boolean isseen) {
        this.isseen = isseen;
    }
}
