package ru.palestra.wifichat.model;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.threeten.bp.Clock;
import org.threeten.bp.LocalDateTime;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;


/**
 * Created by Dmitry on 08.11.2017.
 */

@AutoValue
public abstract class Message implements Serializable, Comparator<Message> {
    public enum State {
        DELIVERED_MESSAGE, NEW_MESSAGE, EMPTY_MESSAGE
    }

    @Nullable
    public abstract String getFrom();
    @Nullable
    public abstract String getTargetId();
    @Nullable
    public abstract String getTargetName();
    @Nullable
    public abstract String getText();
    public abstract String getUUID();
    public abstract LocalDateTime getTimeSend();

    @Nullable
    public abstract Message getDeliveredMessage();

    public static Message newMessage(String from, String targetId, String targetName, String text) {
        return new AutoValue_Message(from, targetId, targetName, text, UUID.randomUUID().toString(), LocalDateTime.now(Clock.systemDefaultZone()), null);
    }

    public static Message broadcastMessage(String from, String targetName, String text, String UUID) {
        return new AutoValue_Message(from, null, targetName, text, UUID, LocalDateTime.now(Clock.systemDefaultZone()), null);
    }

    public static Message deliveredMessage(Message message) {
        return new AutoValue_Message(null, null, null, null, UUID.randomUUID().toString(), LocalDateTime.now(Clock.systemDefaultZone()), message);
    }

    public static Message empty() {
        return new AutoValue_Message(null, null, null, null, null, null, null);
    }

    public State getState() {
        return getUUID() == null ?
                State.EMPTY_MESSAGE :
                getDeliveredMessage() == null ?
                        State.NEW_MESSAGE : State.DELIVERED_MESSAGE;
    }

    @Override
    public int compare(Message message, Message t1) {
        return 0;
    }
}
