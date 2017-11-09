package ru.palestra.wifichat.model;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

/**
 * Created by Dmitry on 08.11.2017.
 */

@Parcel
public class Message {
    private final String from;
    private final String target;
    private final String text;

    public static Message newMessage(String from, String target, String text) {
        return new Message(from, target, text);
    }

//    public static Message uuidMessage(String from, String target, String text) {
//        return new Message(from, target, text);
//    }

    @ParcelConstructor
    private Message(String from, String target, String text) {
        this.from = from;
        this.target = target;
        this.text = text;
    }

    public String getFrom() {
        return from;
    }

    public String getTarget() {
        return target;
    }

    public String getText() {
        return text;
    }
}
