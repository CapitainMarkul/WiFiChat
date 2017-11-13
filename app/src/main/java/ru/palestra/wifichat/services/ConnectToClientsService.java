package ru.palestra.wifichat.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
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

import ru.palestra.wifichat.App;
import ru.palestra.wifichat.model.EndPoint;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.Logger;

/**
 * Created by da.pavlov1 on 13.11.2017.
 */

public class ConnectToClientsService extends IntentService {
    private static final String TAG = ConnectToClientsService.class.getSimpleName() + "_SERVICE";

    public ConnectToClientsService() {
        super(TAG);
    }

    private boolean isDiscovering;

    public static final String SERVICE_ID = "palestra.wifichat";
    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;


    private String myDeviceName;    // TODO: 13.11.2017  Передавать через Intent
// TODO: 13.11.2017 Service должен уметь запускать и останавливать Discovery

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
            Logger.debugLog("stopAdvertising: SUCCESS");
        } else {
            Logger.errorLog("stopAdvertising: FAILURE " + result.getStatus());
        }
    };

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

//            DeviceInfo temp = DeviceInfo.otherDevice(
//                    discoveredEndpointInfo.getEndpointName(), endPointId, null);

            EventBus.getDefault().post(
                    EndPoint.newFound(endPointId, discoveredEndpointInfo.getEndpointName()));
//            mainPresenter.foundNewEndPoint(
//                    EndPoint.newFound(endPointId));
        }

        @Override
        public void onEndpointLost(String endPointId) {
            Logger.debugLog("Lost endpoint: " + endPointId);
            EventBus.getDefault().post(
                    EndPoint.lost(endPointId));
//            mainPresenter.lostEndPoint(endPointId);
        }
    };

    private ResultCallback<? super Status> statusDiscovery = (ResultCallback<Status>) status -> {
        if (status.isSuccess()) {
            Logger.debugLog("startDiscovery: SUCCESS");
        } else {
            Logger.errorLog("startDiscovery: FAILURE" + status.getStatus());
        }
    };

    private boolean isConnected() {
        if (!App.googleApiClient().isConnected()) {
            Logger.errorLog("Need run googleClient");
            return false;
        }
        return true;
    }


    /**
     *
     *
     *
     *
     * */


    @Override
    public void onCreate() {
        super.onCreate();

        startAdvertising();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        isDiscovering = intent.getBooleanExtra("disc", false);

        if (isDiscovering) {
            stopDiscovery();
        } else {
            startDiscovery();   //todo Необходимо чтобы он работал какое-то время
        }
    }

    @Override
    public void onDestroy() {
        runAlarm();
        super.onDestroy();
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
