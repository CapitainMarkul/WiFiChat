package ru.palestra.wifichat.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Strategy;

import org.greenrobot.eventbus.EventBus;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import ru.palestra.wifichat.App;
import ru.palestra.wifichat.model.DeviceInfo;
import ru.palestra.wifichat.model.EndPoint;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.Logger;

/**
 * Created by da.pavlov1 on 13.11.2017.
 */

public class ConnectToClientsService extends Service {
    private static final String TAG = ConnectToClientsService.class.getSimpleName() + "_SERVICE";

    private static final int MSG_START_DISCOVERING = 1001;
    private static final int MSG_STOP_DISCOVERING = 1002;

    private boolean isDiscovering;
    private String myDeviceName;

    public static final String SERVICE_ID = "palestra.wifichat";
    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private Looper mServiceLooper;
    private Handler mServiceHandler;

    private Timer mTimer;
    private ServiceTimer mServiceTimer;

    @Override
    public void onCreate() {
        super.onCreate();

        myDeviceName = App.sharedPreference().getInfoAboutMyDevice().getClientName();

        startAdvertising();

        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mServiceHandler.sendEmptyMessage(MSG_START_DISCOVERING);
    }

    @Override
    public void onDestroy() {
        stopTimer();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runAlarm() {

        Intent intent = new Intent(this, ConnectToClientsService.class);
        intent.putExtra("disc", isDiscovering);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (isDiscovering ? 3000 : 10000),
                PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
    }

    private final class ServiceHandler extends Handler {

        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_DISCOVERING:
                    startDiscovery();
                    break;
                case MSG_STOP_DISCOVERING:
                    stopDiscovery();
                    break;
                default:
                    break;
            }

            isDiscovering = !isDiscovering;
            stopTimer();
            initTimer();
        }
    }

    private void initTimer() {
        if (mTimer == null) {
            mTimer = new Timer();

            if (mServiceTimer != null) mServiceTimer.cancel();
            mServiceTimer = new ServiceTimer();

            mTimer.schedule(
                    mServiceTimer,
                    TimeUnit.SECONDS.toMillis(isDiscovering ? 3 : 10));
        }
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private final class ServiceTimer extends TimerTask {

        @Override
        public void run() {
            if (mServiceHandler != null) {
                Logger.debugLog("Timer connectToClientService");
                mServiceHandler.sendEmptyMessage(
                        isDiscovering ? MSG_STOP_DISCOVERING : MSG_START_DISCOVERING);
            }
        }
    }

    /**
     * startAdvertising()
     * Запуск намерения стать точкой доступа
     */
    private void startAdvertising() {
        if (!isConnected()) return;
        Logger.debugLog("start Advertising");

        Nearby.Connections.startAdvertising(
                App.googleApiClient(),
                myDeviceName,
                SERVICE_ID,
                connectionLifecycleCallback,
                new AdvertisingOptions(STRATEGY))
                .setResultCallback(statusAdvertising);
    }

    /**
     * stopAdvertising()
     * Прекращение намерения стать точкой доступа
     */
    private void stopAdvertising() {
        if (!isConnected()) return;
        Logger.debugLog("stop Advertising");
        Nearby.Connections.stopAdvertising(App.googleApiClient());
    }

    private ResultCallback<? super Connections.StartAdvertisingResult> statusAdvertising = result -> {
        if (result.getStatus().isSuccess()) {
            Logger.debugLog("startAdvertising: SUCCESS");
        } else {
            Logger.errorLog("startAdvertising: FAILURE " + result.getStatus());
        }
    };


    // TODO: 14.11.2017 Возможно этот CAllBAck необходимо очистить
    private ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endPoint, ConnectionInfo connectionInfo) {
            Logger.debugLog("onConnectionInitiated: START!");
            sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
                    .putExtra("idEndPoint", endPoint)
                    .putExtra("nameEndPoint", connectionInfo.getEndpointName())
                    .putExtra("isDisconnect", false));
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Logger.debugLog("Connect: OK");
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Logger.errorLog("Connect: FAIL" + result.getStatus());
                    break;
            }
        }

        @Override
        public void onDisconnected(String endPoint) {
            sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
                    .putExtra("idEndPoint", endPoint)
                    .putExtra("isDisconnect", true));
        }
    };


    /**
     * startDiscovery()
     * Запуск поиска точек для соединения
     */
    private void startDiscovery() {
        if (!isConnected()) return;
        Logger.debugLog("Start discovery");

        Nearby.Connections.startDiscovery(
                App.googleApiClient(),
                SERVICE_ID,
                endpointDiscoveryCallback,
                new DiscoveryOptions(STRATEGY))
                .setResultCallback(statusDiscovery);
    }

    /**
     * stopDiscovery()
     * Прекращение поиска точек для соединения
     */
    private void stopDiscovery() {
        if (!isConnected()) return;
        Logger.debugLog("Stop discovery");

        Nearby.Connections.stopDiscovery(App.googleApiClient());
    }

    /**
     * EndpointDiscoveryCallback()
     * Оповещает о найденных точках доступа
     */
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(
                String endPointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
            Logger.debugLog("Found new endpoint: " + endPointId);

            App.sharedPreference().savePotentialClient(
                    DeviceInfo.otherDevice(discoveredEndpointInfo.getEndpointName(), endPointId, null));


            Logger.debugLog("==========\nPotentialClients:\n");
            for (DeviceInfo client : App.sharedPreference().getAllPotentialClient()) {
                Logger.debugLog(String.format("Client: %s.%s", client.getClientName(), client.getClientNearbyKey()));
            }
        }

        @Override
        public void onEndpointLost(String endPointId) {
            Logger.debugLog("Lost endpoint: " + endPointId);

            App.sharedPreference().removePotentialClient(
                    DeviceInfo.otherDevice(null, endPointId, null));

            Logger.debugLog("==========\nPotentialClients:\n");
            for (DeviceInfo client : App.sharedPreference().getAllPotentialClient()) {
                Logger.debugLog(String.format("Client: %s.%s", client.getClientName(), client.getClientNearbyKey()));
            }
        }
    };

    private ResultCallback<? super Status> statusDiscovery = (ResultCallback<Status>) status -> {
        if (status.isSuccess()) {
            Logger.debugLog("startDiscovery: SUCCESS");
        } else {
            Logger.errorLog("startDiscovery: FAILURE" + status.getStatus());

            if (status.getStatus().toString().contains("STATUS_ALREADY_DISCOVERING")) {
                isDiscovering = true;
            }
        }
    };

    private boolean isConnected() {
        if (!App.googleApiClient().isConnected()) {
            Logger.errorLog("Need run googleClient");
            return false;
        }
        return true;
    }
}
