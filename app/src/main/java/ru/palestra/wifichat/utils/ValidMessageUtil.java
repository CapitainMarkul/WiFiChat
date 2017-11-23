package ru.palestra.wifichat.utils;

import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.data.models.viewmodels.Message;

/**
 * Created by Dmitry on 23.11.2017.
 */

public class ValidMessageUtil {
    private ValidMessageUtil() {

    }

    public static List<Message> obtainValidMessage(List<Message> inputMessages){
        List<Message> validMessages = new ArrayList<>();

        Message[] oldMessages = new Message[inputMessages.size()];
        oldMessages = inputMessages.toArray(oldMessages);

        for (Message message : oldMessages) {
            //Удаляются сообщения старше 2 минут
            if (ChronoUnit.MINUTES.between(TimeUtils.longToLocalDateTime(message.getTimeSend()), TimeUtils.timeNowLocalDateTime()) > 2) {
                inputMessages.remove(message);
                continue;
            }

            validMessages.add(message);
        }
        return validMessages;
    }
}
