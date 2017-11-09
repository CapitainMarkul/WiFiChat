package ru.palestra.wifichat.model;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.io.Serializable;

/**
 * Created by Dmitry on 08.11.2017.
 */

@Parcel
public class Message implements Serializable {
    private final String from;
    private final String targetId;
    private final String targetName;
    private final String text;

    public static Message newMessage(String from, String targetId, String targetName, String text) {
        return new Message(from, targetId, targetName, text);
    }

//    public static Message uuidMessage(String from, String targetId, String text) {
//        return new Message(from, targetId, text);
//    }

    @ParcelConstructor
    private Message(String from, String targetId, String targetName, String text) {
        this.from = from;
        this.targetId = targetId;
        this.targetName = targetName;
        this.text = text;
    }

    public String getFrom() {
        return from;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getText() {
        return text;
    }
}
