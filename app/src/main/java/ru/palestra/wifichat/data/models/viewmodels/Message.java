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
        DELIVERED_MESSAGE, NEW_MESSAGE, EMPTY_MESSAGE
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

    public abstract String getUUID();

    public abstract Long getTimeSend();

    @Nullable
    public abstract Message getDeliveredMessage();

    // TODO: 16.11.2017 PingMessage (С указанием Нашего UUID)

    public static Message newMessage(String fromName, String fromUUID, String targetId, String targetUUID, String text) {
        return new AutoValue_Message(fromName, fromUUID, targetId, targetUUID, text, UUID.randomUUID().toString(), TimeUtils.timeNowLong(), null);
    }

    public static Message broadcastMessage(String fromName, String fromUUID, String targetUUID, String text, String UUID) {
        return new AutoValue_Message(fromName, fromUUID, null, targetUUID, text, UUID, TimeUtils.timeNowLong(), null);
    }

    public static Message deliveredMessage(String fromName, String fromUUID, Message message) {
        return new AutoValue_Message(fromName, fromUUID, null, null, null, UUID.randomUUID().toString(), TimeUtils.timeNowLong(), message);
    }

    public static Message empty() {
        return new AutoValue_Message(null, null, null, null, null, null, null, null);
    }

    public State getState() {
        return getUUID() == null ?
                State.EMPTY_MESSAGE :
                getDeliveredMessage() == null ?
                        State.NEW_MESSAGE : State.DELIVERED_MESSAGE;
    }
}
