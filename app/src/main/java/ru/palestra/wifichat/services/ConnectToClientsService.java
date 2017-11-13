package ru.palestra.wifichat.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.Nullable;

import ru.palestra.wifichat.utils.ConfigIntent;

/**
 * Created by da.pavlov1 on 13.11.2017.
 */

public class ConnectToClientsService extends IntentService {
    private static final String TAG = ConnectToClientsService.class.getSimpleName() + "_SERVICE";

    public ConnectToClientsService() {
        super(TAG);
    }

    private boolean isDiscovering;

    @Override
    public void onDestroy() {
        runAlarm();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        isDiscovering = intent.getBooleanExtra("disc", false);

        sendBroadcast(
                new Intent(ConfigIntent.ACTION_CONNECT_TO_CLIENTS)
                        .putExtra("isDiscovering", isDiscovering));
    }

    private void runAlarm() {
        isDiscovering = !isDiscovering;

        Intent intent = new Intent(this, ConnectToClientsService.class);
        intent.putExtra("disc", isDiscovering);


        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (isDiscovering ? 3000 : 10000),
                PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
    }
}
