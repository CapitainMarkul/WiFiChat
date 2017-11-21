package ru.palestra.wifichat.data.models.viewmodels;

import java.io.Serializable;
import java.util.UUID;

import ru.palestra.wifichat.App;
import ru.palestra.wifichat.utils.TimeUtils;


/**
 * Created by Dmitry on 08.11.2017.
 */

public class Message implements Serializable {
    public enum State {
        DELIVERED_MESSAGE, NEW_MESSAGE, EMPTY_MESSAGE, PING_PONG_MESSAGE, FOR_ME_MESSAGE
    }

    private String fromName;
    private String fromUUID;
    private String targetId;
    private String targetUUID;
    private String text;
    private String msgUUID;
    private Long timeSend;
    private boolean isDelivered;    //доставлено/не доставлено
    private boolean pingPongTypeMsg;
    private Message deliveredMsg;

    public Message() {

    }

    public static Message newMessage(String fromName, String fromUUID, String targetId, String targetUUID, String text) {
        return new Message()
                .setFromName(fromName)
                .setFromUUID(fromUUID)
                .setTargetId(targetId)
                .setTargetUUID(targetUUID)
                .setText(text)
                .setMsgUUID(UUID.randomUUID().toString())
                .setTimeSend(TimeUtils.timeNowLong())
                .setDelivered(false)
                .setPingPongTypeMsg(false);
    }

    public static Message reSendMessage(Message message) {
        return new Message()
                .setFromName(message.getFromName())
                .setFromUUID(message.getFromUUID())
                .setTargetId(message.getTargetId())
                .setTargetUUID(message.getTargetUUID())
                .setText(message.getText())
                .setMsgUUID(message.getMsgUUID())
                .setTimeSend(TimeUtils.timeNowLong())
                .setDelivered(false)
                .setPingPongTypeMsg(false);
    }

    public static Message broadcastMessage(String fromName, String fromUUID, String targetUUID, String text, String originalMessageUUID) {
        return new Message()
                .setFromName(fromName)
                .setFromUUID(fromUUID)
                .setTargetUUID(targetUUID)
                .setText(text)
                .setMsgUUID(originalMessageUUID)
                .setTimeSend(TimeUtils.timeNowLong())
                .setDelivered(false)
                .setPingPongTypeMsg(false);
    }

    //Сообщение о доставке
    public static Message deliveredMessage(String fromName, String fromUUID, Message message) {
        return new Message()
                .setFromName(fromName)
                .setFromUUID(fromUUID)
                .setMsgUUID(UUID.randomUUID().toString())
                .setTimeSend(message.getTimeSend())
                .setDelivered(false)
                .setDeliveredMsg(message)
                .setPingPongTypeMsg(false);
    }

    public static Message pingPongMessage(String fromName, String fromUUID, String targetId) {
        return new Message()
                .setFromName(fromName)
                .setFromUUID(fromUUID)
                .setTargetId(targetId)
                .setMsgUUID(UUID.randomUUID().toString())
                .setTimeSend(TimeUtils.timeNowLong())
                .setDelivered(false)
                .setPingPongTypeMsg(true);
    }

    public static Message empty() {
        return new Message();
    }

    public State getState() {
        //Все в угоду красоте в NearbyService
        return App.sharedPreference().getInfoAboutMyDevice().getUUID().equals(getTargetUUID()) ? State.FOR_ME_MESSAGE :
                isPingPongTypeMsg() ? State.PING_PONG_MESSAGE :
                        getMsgUUID() == null ? State.EMPTY_MESSAGE :
                                getDeliveredMsg() == null ? State.NEW_MESSAGE : State.DELIVERED_MESSAGE;
    }

    @Override
    public String toString() {
        return "Message{"
                + "fromName=" + fromName + ", "
                + "fromUUID=" + fromUUID + ", "
                + "targetId=" + targetId + ", "
                + "targetUUID=" + targetUUID + ", "
                + "text=" + text + ", "
                + "msgUUID=" + msgUUID + ", "
                + "timeSend=" + timeSend + ", "
                + "delivered=" + isDelivered + ", "
                + "deliveredMsg=" + deliveredMsg + ", "
                + "pingPongTypeMsg=" + pingPongTypeMsg
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Message) {
            Message that = (Message) o;
            return ((this.fromName == null) ? (that.getFromName() == null) : this.fromName.equals(that.getFromName()))
                    && ((this.fromUUID == null) ? (that.getFromUUID() == null) : this.fromUUID.equals(that.getFromUUID()))
                    && ((this.targetId == null) ? (that.getTargetId() == null) : this.targetId.equals(that.getTargetId()))
                    && ((this.targetUUID == null) ? (that.getTargetUUID() == null) : this.targetUUID.equals(that.getTargetUUID()))
                    && ((this.text == null) ? (that.getText() == null) : this.text.equals(that.getText()))
                    && (this.msgUUID.equals(that.getMsgUUID()))
//                    && (this.timeSend.equals(that.getTimeSend())) Не нужно т.к. мы можем делать "Перепосылку" сообщения (у такого сообщения должно быть переписано время, но сообщение остается тем же)
                    && (this.isDelivered == that.isDelivered())
                    && ((this.deliveredMsg == null) ? (that.getDeliveredMsg() == null) : this.deliveredMsg.equals(that.getDeliveredMsg()))
                    && (this.pingPongTypeMsg == that.isPingPongTypeMsg());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (fromName == null) ? 0 : this.fromName.hashCode();
        h *= 1000003;
        h ^= (fromUUID == null) ? 0 : this.fromUUID.hashCode();
        h *= 1000003;
        h ^= (targetId == null) ? 0 : this.targetId.hashCode();
        h *= 1000003;
        h ^= (targetUUID == null) ? 0 : this.targetUUID.hashCode();
        h *= 1000003;
        h ^= (text == null) ? 0 : this.text.hashCode();
        h *= 1000003;
        h ^= this.msgUUID.hashCode();
        h *= 1000003;
//        h ^= this.timeSend.hashCode();
//        h *= 1000003;
        h ^= this.isDelivered ? 1231 : 1237;
        h *= 1000003;
        h ^= (deliveredMsg == null) ? 0 : this.deliveredMsg.hashCode();
        h *= 1000003;
        h ^= this.pingPongTypeMsg ? 1231 : 1237;
        return h;
    }

    public String getFromName() {
        return fromName;
    }

    public Message setFromName(String fromName) {
        this.fromName = fromName;
        return this;
    }

    public String getFromUUID() {
        return fromUUID;
    }

    public Message setFromUUID(String fromUUID) {
        this.fromUUID = fromUUID;
        return this;
    }

    public String getTargetId() {
        return targetId;
    }

    public Message setTargetId(String targetId) {
        this.targetId = targetId;
        return this;
    }

    public String getTargetUUID() {
        return targetUUID;
    }

    public Message setTargetUUID(String targetUUID) {
        this.targetUUID = targetUUID;
        return this;
    }

    public String getText() {
        return text;
    }

    public Message setText(String text) {
        this.text = text;
        return this;
    }

    public String getMsgUUID() {
        return msgUUID;
    }

    public Message setMsgUUID(String msgUUID) {
        this.msgUUID = msgUUID;
        return this;
    }

    public Long getTimeSend() {
        return timeSend;
    }

    public Message setTimeSend(Long timeSend) {
        this.timeSend = timeSend;
        return this;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public Message setDelivered(boolean delivered) {
        isDelivered = delivered;
        return this;
    }

    public boolean isPingPongTypeMsg() {
        return pingPongTypeMsg;
    }

    public Message setPingPongTypeMsg(boolean pingPongTypeMsg) {
        this.pingPongTypeMsg = pingPongTypeMsg;
        return this;
    }

    public Message getDeliveredMsg() {
        return deliveredMsg;
    }

    public Message setDeliveredMsg(Message deliveredMsg) {
        this.deliveredMsg = deliveredMsg;
        return this;
    }
}
