package ru.palestra.wifichat;

import android.Manifest;
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
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import ru.palestra.wifichat.adapters.ClientsAdapter;
import ru.palestra.wifichat.adapters.MessagesAdapter;
import ru.palestra.wifichat.model.DeviceInfo;
import ru.palestra.wifichat.model.Message;

import static ru.palestra.wifichat.MainPresenter.STRATEGY;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private RecyclerView potentialClientsRv;
    private RecyclerView messagesRecyclerView;

    private ClientsAdapter potentialClientsAdapter;
    private MessagesAdapter messagesAdapter;

//    private DeviceInfo myDevice;

    private MainPresenter mainPresenter;

    private TextView footer;
    private Button sendMessage;
    private Button searchClients;
    private Button sendBroadcast;
    private Button startAdventuring;
    private EditText textMessage;

//    private String targetId = "";
//    private String targetName = "";

//    private GoogleApiClient googleApiClient;
//
//    private List<Message> lostMessages = new ArrayList<>(); //Недоставленные сообщения
//    private List<Message> deliveredLostMessages = new ArrayList<>(); //Доставленные "Недоставленные" сообщения
//
//    private Set<DeviceInfo> potentialClients = new HashSet<>();
//    private Set<DeviceInfo> connectedClients = new HashSet<>();
//
//    public static final String SERVICE_ID = "palestra.wifichat";
//    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private Timer timerSendLostMessage;
    private Timer timerTryConnect;

    private boolean isDiscovering;
//    private Map<Message, Set<DeviceInfo>> broadCastBanList = new HashMap<>();
//    private boolean sendingLostMessageComplete = true;
//
//    private SharedPrefServiceImpl sharedPrefService = new SharedPrefServiceImpl(this);

//    public interface FoundNewDevice {
//        void foundNewDevice(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo);
//    }
//
//    private FoundNewDevice foundNewDevice = (endpointId, discoveredEndpointInfo) -> {
//
//    };
//
//    private void tryConnectToDevice() {
//
//    }

//    private void sendLostMessage() throws IOException {
//        debugLog(String.format("Timer! Lost: %s, Deliver: %s", lostMessages.size(), deliveredLostMessages.size()));
//
//        //Защита, если мы еще не успели всем передать сообщение, а уже сработало событие
//        if (sendingLostMessageComplete) {   //Lock.class ??
//            sendingLostMessageComplete = false;
//            broadCastBanList.clear();
//
//            if (!lostMessages.isEmpty()) {
////                Intent intent = new Intent(getApplicationContext(), SendLostMessage.class);
////                intent.putExtra("lostMessages", Parcels.wrap(lostMessages));
////                startService(intent);
//
//                Message[] messages = new Message[lostMessages.size()];
//                messages = lostMessages.toArray(messages);
//
//                for (Message message : messages) {
//                    for (DeviceInfo client : connectedClients) {
//                        //Проверяем, пытались ли мы уже отправлять сообщение данному клиенту
//                        if (broadCastBanList.get(message) == null ||
//                                !broadCastBanList.get(message).contains(client)) {
//                            //Проверяем, это тот клиент который нам нужен
//                            if (message.getTargetId().equals(client.getClientNearbyKey()) ||
//                                    message.getTargetName().equals(client.getClientName())) {
//
//                                //Доставили получателю
//                                deliverTargetMessage(message);
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//
//            sendingLostMessageComplete = true;
//        }
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainPresenter = new MainPresenter(this);

        checkPermition();
        createGoogleApiClient();

//        myDevice = sharedPrefService.getInfoAboutMyDevice();

        setupClientsRecyclerView();
        setupMessagesRecyclerView();

        setTitle(mainPresenter.getDeviceName());

        footer = findViewById(R.id.txt_peek);
        sendBroadcast = findViewById(R.id.btn_broadcast);
        textMessage = findViewById(R.id.text_message);
        sendMessage = findViewById(R.id.btn_send_message);
        searchClients = findViewById(R.id.btn_start_search);
        startAdventuring = findViewById(R.id.btn_start_nearby);

        /** ChangeBroadcast */
        sendBroadcast.setOnClickListener(view -> {
            mainPresenter.setDefaultOptions();
//            updateFooterText();
        });

        /** SendMessage */
        sendMessage.setOnClickListener(view -> {
            //send Current Message
            mainPresenter.sendMessage(
                    textMessage.getText().toString());
        });

        /** Start advertising */
        startAdventuring.setOnClickListener((View view) -> {
            if (startAdventuring.getText().toString().contains("Star")) {
                startAdventuring.setText("Stop Advertising");
                startAdventuring();
            } else {
                startAdventuring.setText("Start Advertising");
                stopAdventuring();
            }
        });

        /** Start discovering */
        searchClients.setOnClickListener(view -> {
            if (searchClients.getText().toString().contains("Star")) {
                startDiscovery();
            } else {
                stopDiscovery();
            }
        });
    }

    private void runTimerTask() {
        timerSendLostMessage = new Timer();
        SendLostMessagesTask sendLostMessages = new SendLostMessagesTask();
        timerSendLostMessage.schedule(sendLostMessages, 5000, 20000);

        timerTryConnect = new Timer();
        TryConnectToClients tryConnectToClients = new TryConnectToClients();
        timerTryConnect.schedule(tryConnectToClients, 2500, 4000);
    }

    class SendLostMessagesTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(() -> {
                try {
                    mainPresenter.sendLostMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    class TryConnectToClients extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(() -> {
                if (isDiscovering) {
                    stopDiscovery();
                    mainPresenter.tryConnectDevices();
                } else {
                    startDiscovery();
                }
                isDiscovering = !isDiscovering;
            });
        }
    }

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

    private void createGoogleApiClient() {
        mainPresenter.initGoogleClient(new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(connectionFailedListener)
                .addApi(Nearby.CONNECTIONS_API));
    }

    @Override
    protected void onStart() {
        super.onStart();
        createGoogleApiClient();    // FIXME: 12.11.2017 Без этого не хочет работать
        mainPresenter.getGoogleApiClient().connect();

        runTimerTask();
        debugLog("GoogleClient is Start");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mainPresenter.getGoogleApiClient() != null &&
                mainPresenter.getGoogleApiClient().isConnected()) {
            debugLog("GoogleClient is Stop");
            potentialClientsAdapter.clearAll();
            footer.setText("PEEK");

            mainPresenter.getGoogleApiClient().disconnect();
        }
        timerSendLostMessage.cancel();
        timerTryConnect.cancel();

    }


    /**
     * ==========
     * 2 ЭТАП
     * ==========
     * startAdventuring()
     * Запуск рекламации намерения стать точкой доступа
     */

    private void startAdventuring() {
        if (!mainPresenter.getGoogleApiClient().isConnected()) {
            debugLog("Need run googleClient");
            return;
        }

        debugLog("start Advertising");

        Nearby.Connections.startAdvertising(
                mainPresenter.getGoogleApiClient(),
                mainPresenter.getDeviceName(),
                mainPresenter.getServiceId(),
                connectionLifecycleCallback,
                new AdvertisingOptions(STRATEGY))
                .setResultCallback(result -> {
                    if (result.getStatus().isSuccess()) {
                        debugLog("startAdvertising:onResult: SUCCESS");
                    } else {
                        debugLog("startAdvertising:onResult: FAILURE " + result.getStatus());
                    }
                });
    }

    /**
     * stopSearchingClients()
     * Прекращение намерения стать точкой доступа
     */

    private void stopAdventuring() {
        debugLog("stopSearchingClients");

        Nearby.Connections.stopAdvertising(
                mainPresenter.getGoogleApiClient());

//        potentialClientsAdapter.clearAll();
//        connectedClients.clear();
//        updateFooterText();
    }

    /**
     * ==========
     * 3 ЭТАП
     * ==========
     * startDiscovery()
     * Запуск поиска точек для соединения
     * -
     * Результат поиска обрабатывается в endpointDiscoveryCallback
     */

    private void startDiscovery() {
        searchClients.setText("Stop discovering");

        if (!mainPresenter.getGoogleApiClient().isConnected()) {
            debugLog("Need run googleClient");
            return;
        }

        debugLog("start discovering");

        Nearby.Connections.startDiscovery(
                mainPresenter.getGoogleApiClient(),
                mainPresenter.getServiceId(),
                endpointDiscoveryCallback,
                new DiscoveryOptions(STRATEGY))
                .setResultCallback(
                        status -> {
                            if (status.isSuccess()) {
                                debugLog("startDiscovery:onResult: SUCCESS");
                            } else {
                                debugLog("startDiscovery:onResult: FAILURE" + status.getStatus());
                            }
                        });
    }

    public void stopDiscovery() {
        Nearby.Connections.stopDiscovery(
                mainPresenter.getGoogleApiClient());

        searchClients.setText("Start discovering");

        debugLog("stopDiscovery: SUCCESS");
    }

    /**
     * EndpointDiscoveryCallback()
     * Оповещает о найденных точках доступа
     */

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(
                String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
            debugLog("Found new endpoint: " + endpointId);

            DeviceInfo temp = DeviceInfo.otherDevice(
                    discoveredEndpointInfo.getEndpointName(), endpointId, null);

            mainPresenter.foundNewEndPoint(temp);
        }

        @Override
        public void onEndpointLost(String endPointId) {
            debugLog("Lost endpoint: " + endPointId);
            mainPresenter.lostEndPoint(endPointId);
        }
    };

    /**
     * ==========
     * 4 ЭТАП
     * ==========
     * requestConnection()
     * Присоединение к точке обмена данными
     */

    public interface ItemClick {
        void onItemClick(DeviceInfo client, boolean needRequestConnect);
    }

    private ItemClick itemClickListener = (client, needRequestConnect) -> {
        if (needRequestConnect) {
            stopDiscovery();
            requestConnection(client, null);
        } else {
            debugLog(String.format("Current target: %s - %s",
                    client.getClientName(), client.getClientNearbyKey()));

            mainPresenter.updateTargetDevice(
                    client.getClientNearbyKey(), client.getClientName());
        }
    };

    /**
     * requestConnection
     * Запрос на соединение с клиентом
     */

    public void requestConnection(DeviceInfo client, @Nullable Message lostMessage) {
        Nearby.Connections.requestConnection(
                mainPresenter.getGoogleApiClient(),
                mainPresenter.getDeviceName(),
                client.getClientNearbyKey(),
                connectionLifecycleCallback)
                .setResultCallback(
                        status -> {
                            if (status.isSuccess()) {
                                debugLog("We successfully requested a connection");

                                updateFooterText();

//                                mainPresenter.removePotentialClient(client);
//                                mainPresenter.addConnectedClient(client);

                                if (lostMessage != null) {
                                    try {
                                        mainPresenter.deliverTargetMessage(lostMessage);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                debugLog("Nearby Connections failed" + status.getStatus());

                                if (status.getStatus().equals("STATUS_ENDPOINT_UNKNOWN")) {
                                    mainPresenter.removePotentialClient(client);
                                    potentialClientsAdapter.removeClient(client);

//                                    Nearby.Connections.disconnectFromEndpoint(
//                                            mainPresenter.getGoogleApiClient(), client.getClientNearbyKey());
                                }
                            }
                        }
                );
    }


    /**
     * ==========
     * 5 ЭТАП
     * ==========
     * deliverTargetMessage || sendBroadcastMessage
     * Отправка сообщения
     */

    public void removePotentialClient(DeviceInfo device) {
        potentialClientsAdapter.removeClient(device);
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

    /**
     * ConnectionLifecycleCallback
     * Оповещения о состоянии подключения
     */

    private ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endPoint, ConnectionInfo connectionInfo) {
            debugLog("onConnectionInitiated: START!");

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Automatically accept the connection on both sides.
            Nearby.Connections.acceptConnection(
                    mainPresenter.getGoogleApiClient(),
                    endPoint,
                    payloadCallback)
                    .setResultCallback(status -> {
                        if (status.isSuccess()) {
                            debugLog("onConnectionInitiated: OK!!!");

                            DeviceInfo temp =
                                    mainPresenter.checkNewClient(endPoint, connectionInfo);

                            if (temp.getState() != DeviceInfo.State.EMPTY) {
                                mainPresenter.removePotentialClient(temp);
                                mainPresenter.addConnectedClient(temp);

                                potentialClientsAdapter.setClient(temp);
                                mainPresenter.connectedNewDevice(temp);
                                updateFooterText();
                            }
                        }
                    });
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    debugLog("Connect: OK");
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    debugLog("Connect: FAIL" + result.getStatus());
                    break;
            }
        }

        @Override
        public void onDisconnected(String endPoint) {
            DeviceInfo disconnectedDevice = mainPresenter.searchDisconnectedDevice(endPoint);

            if (disconnectedDevice.getState() != DeviceInfo.State.EMPTY) {
                debugLog("onDisconnected");
                Nearby.Connections.disconnectFromEndpoint(
                        mainPresenter.getGoogleApiClient(), endPoint);
                mainPresenter.disconnectedDevice(endPoint);
//                reconnectToClient(
//                        mainPresenter.searchDisconnectedDevice(endPoint));
                updateFooterText();
            }
        }
    };

    public void updatePotentialClient(DeviceInfo client) {
        potentialClientsAdapter.setClient(client);
    }

    public void updatePotentialClient(Set<DeviceInfo> clients) {
        potentialClientsAdapter.setAllClients(clients);
    }

    private void updateFooterText() {
        footer.setText(
                mainPresenter.createFooterText());
    }

    private void reconnectToClient(DeviceInfo disconnectedDevice) {
        requestConnection(disconnectedDevice, null);
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

//    private DeviceInfo checkNewClient(String endPoint, ConnectionInfo connectionInfo) {
//        DeviceInfo tempClient = DeviceInfo.otherDevice(connectionInfo.getEndpointName(), endPoint, null);
//        if (!connectedClients.contains(tempClient)) {
//            connectedClients.add(tempClient);
//            return tempClient;
//        }
//        return DeviceInfo.empty();
//    }

    public void debugLog(String textLog) {
        Log.d(TAG, textLog);
        Toast.makeText(getApplicationContext(),
                textLog, Toast.LENGTH_SHORT).show();
    }

    private void setupClientsRecyclerView() {
        potentialClientsRv = findViewById(R.id.rv_potential_client);
        potentialClientsRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        potentialClientsAdapter = new ClientsAdapter();
        potentialClientsAdapter.setListener(itemClickListener);
        potentialClientsRv.setAdapter(potentialClientsAdapter);
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