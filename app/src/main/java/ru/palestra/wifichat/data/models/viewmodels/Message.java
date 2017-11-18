package ru.palestra.wifichat.data.models.viewmodels;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.Serializable;
import java.util.UUID;

import ru.palestra.wifichat.utils.TimeUtils;


/**
 * Created by Dmitry on 08.11.2017.
 */

@AutoValue
public abstract class Message implements Serializable {
    public enum State {
        DELIVERED_MESSAGE, NEW_MESSAGE, EMPTY_MESSAGE, PING_PONG_MESSAGE
    }

    @Nullable
    public abstract String getFromName();
    @Nullable
    public abstract String getFromUUID();
    @Nullable
    public abstract String getTargetId();
    @Nullable
    public abstract String getTargetUUID();
    @Nullable
    public abstract String getText();
    public abstract String getMsgUUID();
    public abstract Long getTimeSend();
    public abstract boolean isDelivered();  //доставлено/не доставлено
    @Nullable
    public abstract Message getDeliveredMsg(); //чтение доставленного сообщения
    public abstract boolean isPingPongTypeMsg();

    static Builder builder() {
        return new AutoValue_Message.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder setFromName(String name);
        abstract Builder setFromUUID(String UUID);
        abstract Builder setTargetId(String id);
        abstract Builder setTargetUUID(String UUID);
        abstract Builder setText(String text);
        abstract Builder setMsgUUID(String UUID);
        abstract Builder setTimeSend(Long time);
        abstract Builder setDelivered(boolean isDelivered);
        abstract Builder setDeliveredMsg(Message message);
        abstract Builder setPingPongTypeMsg(boolean isPingPong);
        abstract Message build();
    }

    public static Message newMessage(String fromName, String fromUUID, String targetId, String targetUUID, String text) {
        return Message.builder()
                .setFromName(fromName)
                .setFromUUID(fromUUID)
                .setTargetId(targetId)
                .setTargetUUID(targetUUID)
                .setText(text)
                .setMsgUUID(UUID.randomUUID().toString())
                .setTimeSend(TimeUtils.timeNowLong())
                .setDelivered(false)
                .setPingPongTypeMsg(false)
                .build();
    }

    public static Message broadcastMessage(String fromName, String fromUUID, String targetUUID, String text, String originalMessageUUID) {
        return Message.builder()
                .setFromName(fromName)
                .setFromUUID(fromUUID)
                .setTargetUUID(targetUUID)
                .setText(text)
                .setMsgUUID(originalMessageUUID)
                .setTimeSend(TimeUtils.timeNowLong())
                .setDelivered(false)
                .setPingPongTypeMsg(false)
                .build();
    }

    public static Message deliveredMessage(String fromName, String fromUUID, Message message) {
        return Message.builder()
                .setFromName(fromName)
                .setFromUUID(fromUUID)
                .setMsgUUID(UUID.randomUUID().toString())
                .setTimeSend(TimeUtils.timeNowLong())
                .setDelivered(false)
                .setDeliveredMsg(message)
                .setPingPongTypeMsg(false)
                .build();
    }

    //todo Нет targetUUID, необходимо учитывать. Здесь мы только "Знакомимся"
    public static Message pingPongMessage(String fromName, String fromUUID, String targetId) {
        return Message.builder()
                .setFromName(fromName)
                .setFromUUID(fromUUID)
                .setTargetId(targetId)
                .setMsgUUID(UUID.randomUUID().toString())
                .setTimeSend(TimeUtils.timeNowLong())
                .setDelivered(false)
                .setPingPongTypeMsg(true)
                .build();
    }

    public static Message empty() {
        return Message.builder().build();
    }

    public State getState() {
        return isPingPongTypeMsg() ? State.PING_PONG_MESSAGE :
                getMsgUUID() == null ? State.EMPTY_MESSAGE :
                        getDeliveredMsg() == null ? State.NEW_MESSAGE : State.DELIVERED_MESSAGE;
    }
}
