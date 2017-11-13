package ru.palestra.wifichat.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.model.Message;
import ru.palestra.wifichat.utils.ConfigIntent;

/**
 * Created by da.pavlov1 on 09.11.2017.
 */

public class SendLostMessageService extends IntentService {
    private static final String TAG = SendLostMessageService.class.getSimpleName() + "_SERVICE";

    public SendLostMessageService() {
        super(TAG);
    }

    @Override
    public void onDestroy() {
        runAlarm();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        sendBroadcast(
                new Intent(ConfigIntent.ACTION_SEND_LOST_MESSAGE));
    }

    private void runAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 20 * 1000,    // 20 sec
                PendingIntent.getService(this, 0,
                        new Intent(this, SendLostMessageService.class), 0));
    }
}
