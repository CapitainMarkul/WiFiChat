package ru.palestra.wifichat.model;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.threeten.bp.LocalDateTime;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;


/**
 * Created by Dmitry on 08.11.2017.
 */

@AutoValue
public abstract class Message implements Serializable, Comparator<Message> {
    public enum State{
        DELIVERED_MESSAGE, NEW_MESSAGE
    }

    @Nullable public abstract String getFrom();
    @Nullable public abstract String getTargetId();
    @Nullable public abstract String getTargetName();
    @Nullable public abstract String getText();
    public abstract String getUUID();
    public abstract LocalDateTime getTimeSend();
    @Nullable public abstract Message getDeliveredMessage();

    public static Message newMessage(String from, String targetId, String targetName, String text) {
        return new AutoValue_Message(from, targetId, targetName, text, UUID.randomUUID().toString(), LocalDateTime.now(),null);
    }

    public static Message broadcastMessage(String from, String targetName, String text) {
        return new AutoValue_Message(from, null, targetName, text, UUID.randomUUID().toString(), LocalDateTime.now(),null);
    }

    public static Message deliveredMessage(String targetId, Message message) {
        return new AutoValue_Message(null, targetId, null, null, UUID.randomUUID().toString(), LocalDateTime.now(), message);
    }

    // TODO: 10.11.2017 Доделай, чтобы МЕНЯ больше не искали
    public static Message stopSearchMe(String targetId, Message message) {
        return new AutoValue_Message(null, targetId, null, null, UUID.randomUUID().toString(), LocalDateTime.now(), message);
    }

    public State getState(){
        return getDeliveredMessage() != null ?
                State.DELIVERED_MESSAGE : State.NEW_MESSAGE;
    }

    @Override
    public int compare(Message message, Message t1) {
        return 0;
    }
}
