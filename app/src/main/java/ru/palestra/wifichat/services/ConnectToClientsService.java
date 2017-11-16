package ru.palestra.wifichat.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;

/**
 * Created by da.pavlov1 on 13.11.2017.
 */

// TODO: 15.11.2017 Объединить с сервисом сообщений, сделать правильное планирование и запуск задач
public class ConnectToClientsService extends Service {
    private static final String TAG = ConnectToClientsService.class.getSimpleName() + "_SERVICE";

//    private static final int MSG_START_DISCOVERING = 1001;
//    private static final int MSG_STOP_DISCOVERING = 1002;

//    private boolean isDiscovering;
//    private String myDeviceName;

//    public static final String SERVICE_ID = "palestra.wifichat";
//    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private Looper mServiceLooper;
    private Handler mServiceHandler;

//    private Timer connectionTimer;
//    private ConnectionServiceTimer connectionServiceTimer;

    @Override
    public void onCreate() {
        super.onCreate();
//        myDeviceName = App.sharedPreference().getInfoAboutMyDevice().getClientName();

//        startAdvertising();

//        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
//        thread.start();
//
//        mServiceLooper = thread.getLooper();
//        mServiceHandler = new SendMessageServiceHandler(mServiceLooper);
//
//        mServiceHandler.sendEmptyMessage(MSG_START_DISCOVERING);
    }

    @Override
    public void onDestroy() {
//        stopConnectionTimer();
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

//    private final class ConnectionServiceHandler extends Handler {
//
//        ConnectionServiceHandler(Looper looper) {
//            super(looper);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case MSG_START_DISCOVERING:
//                    startDiscovery();
//                    break;
//                case MSG_STOP_DISCOVERING:
//                    stopDiscovery();
//                    break;
//                default:
//                    break;
//            }
//
//            isDiscovering = !isDiscovering;
//            stopConnectionTimer();
//            initConnectionTimer();
//        }
//    }
//
//    private void initConnectionTimer() {
//        if (connectionTimer == null) {
//            connectionTimer = new Timer();
//
//            if (connectionServiceTimer != null) connectionServiceTimer.cancel();
//            connectionServiceTimer = new ConnectionServiceTimer();
//
//            connectionTimer.schedule(
//                    connectionServiceTimer,
//                    TimeUnit.SECONDS.toMillis(isDiscovering ? 3 : 10));
//        }
//    }
//
//    private void stopConnectionTimer() {
//        if (connectionTimer != null) {
//            connectionTimer.cancel();
//            connectionTimer = null;
//        }
//    }
//
//    private final class ConnectionServiceTimer extends TimerTask {
//
//        @Override
//        public void run() {
//            if (mServiceHandler != null) {
//                Logger.debugLog("Timer connectToClientService");
//                mServiceHandler.sendEmptyMessage(
//                        isDiscovering ? MSG_STOP_DISCOVERING : MSG_START_DISCOVERING);
//            }
//        }
//    }

//    /**
//     * startAdvertising()
//     * Запуск намерения стать точкой доступа
//     */
//    private void startAdvertising() {
//        if (!isConnected()) return;
//        Logger.debugLog("start Advertising");
//
//        Nearby.Connections.startAdvertising(
//                App.googleApiClient(),
//                myDeviceName,
//                SERVICE_ID,
//                connectionLifecycleCallback,
//                new AdvertisingOptions(STRATEGY))
//                .setResultCallback(statusAdvertising);
//    }
//
//    /**
//     * stopAdvertising()
//     * Прекращение намерения стать точкой доступа
//     */
//    private void stopAdvertising() {
//        if (!isConnected()) return;
//        Logger.debugLog("stop Advertising");
//        Nearby.Connections.stopAdvertising(App.googleApiClient());
//    }
//
//    private ResultCallback<? super Connections.StartAdvertisingResult> statusAdvertising = result -> {
//        if (result.getStatus().isSuccess()) {
//            Logger.debugLog("startAdvertising: SUCCESS");
//        } else {
//            Logger.errorLog("startAdvertising: FAILURE " + result.getStatus());
//        }
//    };
//
//
//    // TODO: 14.11.2017 Возможно этот CAllBAck необходимо очистить
//    private ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
//        @Override
//        public void onConnectionInitiated(String endPoint, ConnectionInfo connectionInfo) {
//            Logger.debugLog("onConnectionInitiated: START!");
//            sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
//                    .putExtra(ConfigIntent.CONNECTION_TARGET_ID, endPoint)
//                    .putExtra(ConfigIntent.CONNECTION_TARGET_NAME, connectionInfo.getEndpointName())
//                    .putExtra(ConfigIntent.CONNECTION_FOOTER_TEXT, "Кто то пришел")
//                    .putExtra(ConfigIntent.CONNECTION_TARGET_IS_DISCONNECT, false));
//        }
//
//        @Override
//        public void onConnectionResult(String s, ConnectionResolution result) {
//            switch (result.getStatus().getStatusCode()) {
//                case ConnectionsStatusCodes.STATUS_OK:
//                    Logger.debugLog("Connect: OK");
//                    break;
//                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
//                    Logger.errorLog("Connect: FAIL" + result.getStatus());
//                    break;
//            }
//        }
//
//        @Override
//        public void onDisconnected(String endPoint) {
//            sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
//                    .putExtra(ConfigIntent.CONNECTION_TARGET_ID, endPoint)
//                    .putExtra(ConfigIntent.CONNECTION_FOOTER_TEXT, "Кто то пришел")
//                    .putExtra(ConfigIntent.CONNECTION_TARGET_IS_DISCONNECT, true));
//        }
//    };


//    /**
//     * startDiscovery()
//     * Запуск поиска точек для соединения
//     */
//    private void startDiscovery() {
//        if (!isConnected()) return;
//        Logger.debugLog("Start discovery");
//
//        Nearby.Connections.startDiscovery(
//                App.googleApiClient(),
//                SERVICE_ID,
//                endpointDiscoveryCallback,
//                new DiscoveryOptions(STRATEGY))
//                .setResultCallback(statusDiscovery);
//    }
//
//    /**
//     * stopDiscovery()
//     * Прекращение поиска точек для соединения
//     */
//    private void stopDiscovery() {
//        if (!isConnected()) return;
//        Logger.debugLog("Stop discovery");
//
//        Nearby.Connections.stopDiscovery(App.googleApiClient());
//    }
//
//    /**
//     * EndpointDiscoveryCallback()
//     * Оповещает о найденных точках доступа
//     */
//    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
//        @Override
//        public void onEndpointFound(
//                String idEndPoint, DiscoveredEndpointInfo discoveredEndpointInfo) {
//            Logger.debugLog("Found new endpoint: " + idEndPoint);
//
//            App.sharedPreference().saveWasConnectedClient(
//                    ClientSql.otherDevice(discoveredEndpointInfo.getEndpointName(), idEndPoint, null));
//
//            sendBroadcast(new Intent(ConfigIntent.ACTION_SEARCH_CLIENT)
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_ID, idEndPoint)
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_NAME, discoveredEndpointInfo.getEndpointName())
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_IS_LOST, false));
//        }
//
//        @Override
//        public void onEndpointLost(String idEndPoint) {
//            Logger.debugLog("Lost endpoint: " + idEndPoint);
//
//            App.sharedPreference().removeWasConnectedClient(
//                    ClientSql.otherDevice(null, idEndPoint, null));
//
//            sendBroadcast(new Intent(ConfigIntent.ACTION_SEARCH_CLIENT)
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_ID, idEndPoint)
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_IS_LOST, true));
//        }
//    };
//
//    private ResultCallback<? super Status> statusDiscovery = (ResultCallback<Status>) status -> {
//        if (status.isSuccess()) {
//            Logger.debugLog("startDiscovery: SUCCESS");
//        } else {
//            Logger.errorLog("startDiscovery: FAILURE" + status.getStatus());
//
//            if (status.getStatus().toString().contains("STATUS_ALREADY_DISCOVERING")) {
//                isDiscovering = true;
//            }
//        }
//    };
//
//    private boolean isConnected() {
//        if (!App.googleApiClient().isConnected()) {
//            Logger.errorLog("Need run googleClient");
//            return false;
//        }
//        return true;
//    }
}
