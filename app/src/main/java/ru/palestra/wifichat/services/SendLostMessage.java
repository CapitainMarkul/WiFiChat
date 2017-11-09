package ru.palestra.wifichat.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.model.Message;

/**
 * Created by da.pavlov1 on 09.11.2017.
 */

public class SendLostMessage extends IntentService {
    public SendLostMessage(String name) {
        super(name);
    }

    private List<Message> lostMessages = new ArrayList<>();

    @Override
    public void onDestroy() {
        runAlarm();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        lostMessages.clear();
        lostMessages.addAll(intent.getParcelableExtra("lostMessages"));

//        for (Message message : lostMessages) {
//            try {
//                if (message.getTargetId().equals(endpointId) ||
//                        message.getTargetName().equals(discoveredEndpointInfo.getEndpointName())) {
//                    //отправили получателю
//
//                    sendBroadcastMessage(message);
//                    lostMessages.remove(message);
//
//
//                } else {
//                    // TODO: 09.11.2017  //передали другим получателям
//                    sendBroadcastMessage(message);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void runAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 20 * 1000,    // 20 sec
                PendingIntent.getService(this, 0,
                        new Intent(this, SendLostMessage.class), 0));
    }
}
