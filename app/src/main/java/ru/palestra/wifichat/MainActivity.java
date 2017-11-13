package ru.palestra.wifichat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Set;

import ru.palestra.wifichat.adapters.ClientsAdapter;
import ru.palestra.wifichat.adapters.MessagesAdapter;
import ru.palestra.wifichat.model.DeviceInfo;
import ru.palestra.wifichat.model.EndPoint;
import ru.palestra.wifichat.model.Message;
import ru.palestra.wifichat.services.ConnectToClientsService;
import ru.palestra.wifichat.services.SendLostMessageService;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.Logger;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private RecyclerView clientsRv;
    private RecyclerView messagesRecyclerView;

    private ClientsAdapter clientsAdapter;
    private MessagesAdapter messagesAdapter;

    private MainPresenter mainPresenter;

    private TextView footer;
    private Button sendMessage;
    private Button searchClients;
    private Button defaultOption;
    private Button startAdventuring;
    private EditText textMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainPresenter = new MainPresenter(this, statusRequestConnectionListener);
        setTitle(mainPresenter.getDeviceName());

        checkPermition();
//        createGoogleApiClient();

        setupClientsRecyclerView();
        setupMessagesRecyclerView();

        registerReceiver(sendLostMessagesReceiver, new IntentFilter(ConfigIntent.ACTION_SEND_LOST_MESSAGE));
//        registerReceiver(connectToClientsReceiver, new IntentFilter(ConfigIntent.ACTION_CONNECT_TO_CLIENTS));
        registerReceiver(connectionLifecycleReceiver, new IntentFilter(ConfigIntent.ACTION_CONNECTION_INITIATED));

        footer = findViewById(R.id.txt_peek);
        defaultOption = findViewById(R.id.btn_default_option);
        textMessage = findViewById(R.id.text_message);
        sendMessage = findViewById(R.id.btn_send_message);
        searchClients = findViewById(R.id.btn_start_search);
        startAdventuring = findViewById(R.id.btn_start_nearby);

        /** setDefaultOptions */
        defaultOption.setOnClickListener(view -> {
            mainPresenter.setDefaultOptions();
            updateFooterText();
        });

        /** SendMessage */
        sendMessage.setOnClickListener(view -> {
            //send Current Message
            mainPresenter.sendMessage(
                    textMessage.getText().toString());
        });

        /** Start advertising */
        startAdventuring.setOnClickListener((View view) -> {
//            if (startAdventuring.getText().toString().contains("Star")) {
//                startAdventuring.setText("Stop Advertising");
//                startAdvertising();
//            } else {
//                startAdventuring.setText("Start Advertising");
//                stopAdvertising();
//            }
        });

        /** Start discovering */
        searchClients.setOnClickListener(view -> {
//            if (searchClients.getText().toString().contains("Star")) {
//                startDiscovery();
//            } else {
//                stopDiscovery();
//            }
        });
    }

    private void startServices() {
        startService(new Intent(this, SendLostMessageService.class));
        startService(new Intent(this, ConnectToClientsService.class));
    }

    private void stopServices() {
        // FIXME: 13.11.2017 Падает, если мы вышли, но был запланирован AlarmMAnager. (GoogleApiClient not init)
        stopService(new Intent(this, SendLostMessageService.class));
        stopService(new Intent(this, ConnectToClientsService.class));
    }

    BroadcastReceiver sendLostMessagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                mainPresenter.sendLostMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    BroadcastReceiver connectionLifecycleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isDisconnect = intent.getBooleanExtra("isDisconnect", false);

            if(!isDisconnect) {
                String idEndPoint = intent.getStringExtra("idEndPoint");
                String nameEndPoint = intent.getStringExtra("nameEndPoint");

                // Automatically accept the connection on both sides.
                // FIXME: 13.11.2017 Проблема, с переносом в Сервис
                mainPresenter.acceptConnection(idEndPoint, payloadCallback,
                        status -> {
                            if (status.isSuccess()) {
                                Logger.debugLog("onConnectionInitiated: OK!!!");

                                DeviceInfo temp =
                                        mainPresenter.checkNewClient(idEndPoint, nameEndPoint);

                                if (temp.getState() != DeviceInfo.State.EMPTY) {
                                    mainPresenter.removePotentialClient(temp);
                                    mainPresenter.saveConnectedDevice(temp);

                                    clientsAdapter.setClient(temp);
                                    updateFooterText();
                                }
                            }
                        });
            } else {
                String idEndPoint = intent.getStringExtra("idEndPoint");

                DeviceInfo disconnectedDevice =
                        mainPresenter.searchDisconnectedDevice(idEndPoint);

                if (disconnectedDevice.getState() != DeviceInfo.State.EMPTY) {
                    Logger.debugLog("onDisconnected");

                    mainPresenter.disconnectedDevice(idEndPoint);
//                reconnectToClient(
//                        mainPresenter.searchDisconnectedDevice(endPoint));
                    updateFooterText();
                }
            }
        }
    };

    /**
     * GoogleApiClient.ConnectionCallbacks
     * ???????????
     */

    GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            debugLog("connectionCallbacks: onConnected");
        }

        @Override
        public void onConnectionSuspended(int i) {
            debugLog("connectionCallbacks: onConnectionSuspended " + i);
        }
    };

    /**
     * GoogleApiClient.OnConnectionFailedListener
     * Неудачи подключения
     */
    GoogleApiClient.OnConnectionFailedListener connectionFailedListener = connectionResult -> {

    };


    /**
     * ==========
     * 1 ЭТАП
     * ==========
     * Создание главного объекта доступа – GoogleApiClient.
     * Запуск клиента. Остановка клиента.
     */

//    private void createGoogleApiClient() {
//        mainPresenter.initGoogleClient(new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(connectionCallbacks)
//                .addOnConnectionFailedListener(connectionFailedListener)
//                .addApi(Nearby.CONNECTIONS_API));
//    }
    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

//        mainPresenter.getGoogleApiClient().connect();

        startServices();
        debugLog("GoogleClient is Start");
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);

        stopServices();

//        if (mainPresenter.getGoogleApiClient() != null &&
//                mainPresenter.getGoogleApiClient().isConnected()) {
//            debugLog("GoogleClient is Stop");
            clientsAdapter.clearAll();
            footer.setText("PEEK");

//            mainPresenter.getGoogleApiClient().disconnect();
    }


    //foundNewPoint/LostPoint
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEndPointEvent(EndPoint endPoint) {
        if (endPoint.getState() == EndPoint.State.NEW_FOUND) {
            mainPresenter.foundNewEndPoint(
                    endPoint.getIdEndPoint(), endPoint.getNameEndPoint());
        } else {
            mainPresenter.lostEndPoint(endPoint.getIdEndPoint());
        }
    }


    /**
     * ==========
     * 2 ЭТАП
     * ==========
     * startAdvertising()
     * Запуск рекламации намерения стать точкой доступа
     */

//    private void startAdvertising() {
//        if (!mainPresenter.getGoogleApiClient().isConnected()) {
//            debugLog("Need run googleClient");
//            return;
//        }
//
//        debugLog("start Advertising");
//
//        mainPresenter.startAdvertising(connectionLifecycleCallback, statusAdvertising);
//    }
//
//    private ResultCallback<? super Connections.StartAdvertisingResult> statusAdvertising = result -> {
//        if (result.getStatus().isSuccess()) {
//            debugLog("stopAdvertising:onResult: SUCCESS");
//        } else {
//            debugLog("stopAdvertising:onResult: FAILURE " + result.getStatus());
//        }
//    };

    /**
     * stopAdvertising()
     * Прекращение намерения стать точкой доступа
     */

//    private void stopAdvertising() {
//        debugLog("stopSearchingClients");
//
//        mainPresenter.stopAdvertising();
//
////        clientsAdapter.clearAll();
////        connectedClients.clear();
////        updateFooterText();
//    }

    /**
     * ==========
     * 3 ЭТАП
     * ==========
     * startDiscovery()
     * Запуск поиска точек для соединения
     * -
     * Результат поиска обрабатывается в endpointDiscoveryCallback
     */

//    private void startDiscovery() {
//        searchClients.setText("Stop discovering");
//
//        if (!mainPresenter.getGoogleApiClient().isConnected()) {
//            debugLog("Need run googleClient");
//            return;
//        }
//
//        debugLog("start discovering");
//
//        mainPresenter.startDiscovery(endpointDiscoveryCallback, resultDiscovery);
//    }

//    /**
//     * EndpointDiscoveryCallback()
//     * Оповещает о найденных точках доступа
//     */
//
//    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
//        @Override
//        public void onEndpointFound(
//                String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
//            debugLog("Found new endpoint: " + endpointId);
//
//            DeviceInfo temp = DeviceInfo.otherDevice(
//                    discoveredEndpointInfo.getEndpointName(), endpointId, null);
//
//            mainPresenter.foundNewEndPoint(temp);
//        }
//
//        @Override
//        public void onEndpointLost(String endPointId) {
//            debugLog("Lost endpoint: " + endPointId);
//            mainPresenter.lostEndPoint(endPointId);
//        }
//    };
//
//    private ResultCallback<? super Status> resultDiscovery = (ResultCallback<Status>) status -> {
//        if (status.isSuccess()) {
//            debugLog("startDiscovery:onResult: SUCCESS");
//        } else {
//            debugLog("startDiscovery:onResult: FAILURE" + status.getStatus());
//        }
//    };

//    public void stopDiscovery() {
//        mainPresenter.stopDiscovery();
//
//        searchClients.setText("Start discovering");
//
//        debugLog("stopDiscovery: SUCCESS");
//    }

    /**
     * ==========
     * 4 ЭТАП
     * ==========
     * requestConnection()
     * Присоединение к точке обмена данными
     * Запрос на соединение с клиентом
     */

    /**
     * ConnectionLifecycleCallback
     * Оповещения о состоянии подключения
     */

//    private ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
//        @Override
//        public void onConnectionInitiated(String endPoint, ConnectionInfo connectionInfo) {
//            debugLog("onConnectionInitiated: START!");
//
////            try {
////                Thread.sleep(200);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
//
//            // Automatically accept the connection on both sides.
//            mainPresenter.acceptConnection(endPoint, payloadCallback, status -> {
//                if (status.isSuccess()) {
//                    debugLog("onConnectionInitiated: OK!!!");
//
//                    DeviceInfo temp =
//                            mainPresenter.checkNewClient(endPoint, connectionInfo);
//
//                    if (temp.getState() != DeviceInfo.State.EMPTY) {
//                        mainPresenter.removePotentialClient(temp);
//                        mainPresenter.saveConnectedDevice(temp);
//
//                        clientsAdapter.setClient(temp);
//                        updateFooterText();
//                    }
//                }
//            });
//        }
//
//        @Override
//        public void onConnectionResult(String s, ConnectionResolution result) {
//            switch (result.getStatus().getStatusCode()) {
//                case ConnectionsStatusCodes.STATUS_OK:
//                    debugLog("Connect: OK");
//                    break;
//                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
//                    debugLog("Connect: FAIL" + result.getStatus());
//                    break;
//            }
//        }
//
//        @Override
//        public void onDisconnected(String endPoint) {
//            DeviceInfo disconnectedDevice = mainPresenter.searchDisconnectedDevice(endPoint);
//
//            if (disconnectedDevice.getState() != DeviceInfo.State.EMPTY) {
//                debugLog("onDisconnected");
//
//                mainPresenter.disconnectedDevice(endPoint);
////                reconnectToClient(
////                        mainPresenter.searchDisconnectedDevice(endPoint));
//                updateFooterText();
//            }
//        }
//    };

    public interface ItemClick {
        void onItemClick(DeviceInfo client, boolean needRequestConnect);
    }

    interface StatusRequestConnectionListener {
        void onConnected(DeviceInfo client, Status status);
    }

    private StatusRequestConnectionListener statusRequestConnectionListener = (client, status) -> {
        if (status.isSuccess()) {
            debugLog("We successfully requested a connection");

            updateFooterText();
        } else {
            debugLog("Nearby Connections failed" + status.getStatus());
// TODO: 13.11.2017 Здесь будет нерабочее. Оставить только выбор таргета
            if (status.getStatus().toString().contains("STATUS_ENDPOINT_UNKNOWN")) {
                mainPresenter.removePotentialClient(client);
                removePotentialClient(client);
            } else if (status.getStatus().toString().contains("STATUS_ALREADY_CONNECTED_TO_ENDPOINT")) {
                mainPresenter.removePotentialClient(client);
            }
        }
    };

    private ItemClick itemClickListener = (client, needRequestConnect) -> {
        if (needRequestConnect) {
//            stopDiscovery();
//            mainPresenter.requestConnection(client, null,
//                    connectionLifecycleCallback, statusRequestConnectionListener);
        } else {
            debugLog(String.format("Current target: %s - %s",
                    client.getClientName(), client.getClientNearbyKey()));

            mainPresenter.updateTargetDevice(
                    client.getClientNearbyKey(), client.getClientName());
        }
    };

    public void removePotentialClient(DeviceInfo device) {
        clientsAdapter.removeClient(device);
    }

    public void showMessageForMe(Message message) {
        messagesAdapter.setMessages(message);
        textMessage.setText("");

        scrollToBottom();
    }

    public void showBroadcastMessage(Message message) {
        messagesAdapter.setMessages(message);
        textMessage.setText("");

        scrollToBottom();
    }

    public void showMyMessage(Message message) {
        messagesAdapter.setMessages(message);
        textMessage.setText("");

        scrollToBottom();
    }
    /**
     * ==========
     * 5 ЭТАП
     * ==========
     * deliverTargetMessage || sendBroadcastMessage
     * Отправка сообщения
     */
    /**
     * ==========
     * 6 ЭТАП
     * ==========
     * onPayloadReceived()
     * Принятие и обработка сообщений
     */

    /**
     * PayloadCallback
     * Прием сообщений от других клиентов
     */

    private PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endPointId, Payload payload) {
            mainPresenter.receivedRequest(endPointId, payload);
        }

        @Override
        public void onPayloadTransferUpdate(String endPointId, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };


    public void updatePotentialClient(DeviceInfo client) {
        clientsAdapter.setClient(client);
    }

    public void updatePotentialClient(Set<DeviceInfo> clients) {
        clientsAdapter.setAllClients(clients);
    }

    private void updateFooterText() {
        footer.setText(
                mainPresenter.createFooterText());
    }

    /**
     * OTHER
     */

    /**
     * Проверка разрешений приложения (Для android 6.0 и выше)
     */
    private void checkPermition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestPermission();
        }
    }

    public void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                }, 0);  // TODO: 07.11.2017 RequestCode ?
    }

    public void debugLog(String textLog) {
        Log.d(TAG, textLog);
//        Toast.makeText(getApplicationContext(),
//                textLog, Toast.LENGTH_SHORT).show();
    }

    private void setupClientsRecyclerView() {
        clientsRv = findViewById(R.id.rv_potential_client);
        clientsRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        clientsAdapter = new ClientsAdapter();
        clientsAdapter.setListener(itemClickListener);
        clientsRv.setAdapter(clientsAdapter);
    }

    private void setupMessagesRecyclerView() {
        messagesRecyclerView = findViewById(R.id.massages_list);

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(true);

        messagesRecyclerView.setLayoutManager(linearLayoutManager);

        messagesAdapter = new MessagesAdapter();
        messagesAdapter.setCurrentDevice(
                mainPresenter.getDeviceName());
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void scrollToBottom() {
        messagesRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1);
    }
}