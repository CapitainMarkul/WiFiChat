package ru.palestra.wifichat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ru.palestra.wifichat.data.models.viewmodels.Message;

/**
 * Created by da.pavlov1 on 09.11.2017.
 */

public class MessageConverter {
    private MessageConverter() {
        //no instance
    }

    public static byte[] toBytes(Message message) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOS;
        try {
            objectOS = new ObjectOutputStream(outputStream);
            objectOS.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public static Message getMessage(byte[] data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        ObjectInputStream objectIS;
        try {
            objectIS = new ObjectInputStream(inputStream);

            return (Message) objectIS.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();

            return Message.empty();
        }
    }
}
