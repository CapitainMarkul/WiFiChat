package ru.palestra.wifichat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ru.palestra.wifichat.model.Message;

/**
 * Created by da.pavlov1 on 09.11.2017.
 */

public class MessageConverter {
    private MessageConverter() {
        //no instance
    }

    public static byte[] toBytes(Message message) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOS = new ObjectOutputStream(outputStream);
        objectOS.writeObject(message);
        return outputStream.toByteArray();
    }

    public static Message getMessage(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        ObjectInputStream objectIS = new ObjectInputStream(inputStream);
        return (Message) objectIS.readObject();
    }
}
