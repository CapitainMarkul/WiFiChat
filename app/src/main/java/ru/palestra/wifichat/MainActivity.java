package ru.palestra.wifichat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.nearby.connection.Strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import ru.palestra.wifichat.adapters.ClientsAdapter;
import ru.palestra.wifichat.adapters.MessagesAdapter;
import ru.palestra.wifichat.model.DeviceInfo;
import ru.palestra.wifichat.model.Message;
import ru.palestra.wifichat.services.SharedPrefServiceImpl;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private DeviceInfo myDevice;

    private RecyclerView clientsRecyclerView;
    private ClientsAdapter clientsAdapter;

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;

    private TextView footer;
    private Button sendMessage;
    private Button searchClients;
    private Button sendBroadcast;
    private EditText textMessage;

    private boolean isDiscovering = false;

    private String targetId = "";
    private String targetName = "";

    private GoogleApiClient googleApiClient;
    // client's name that's visible to other devices when connecting
    private List<Message> lostMessages = new ArrayList<>(); //Недоставленные сообщения
    private Set<DeviceInfo> clients = new HashSet<>();

    public static final String SERVICE_ID = "palestra.wifichat";
    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private Timer timer;

    private Map<Message, List<DeviceInfo>> broadCastBanList = new HashMap<>();
    private boolean sendingLostMessageComplete = true;

    private SharedPrefServiceImpl sharedPrefService = new SharedPrefServiceImpl(this);

    public interface FoundNewDevice {
        void foundNewDevice(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo);
    }

    private FoundNewDevice foundNewDevice = (endpointId, discoveredEndpointInfo) -> {
        // TODO: 09.11.2017  После нахождения нового устройства проверяем, ему ли предназначалось сообщение
//
//        debugLog(String.format("___Count Lost Message %s", lostMessages.size()));
//
//        try {
////            sendLostMessage();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    };

    private void sendLostMessage() throws IOException {

        //Защита, если мы есще не успели всем передать сообщение, а уже сработало событие
        if (sendingLostMessageComplete) {   //Lock.class ??
            sendingLostMessageComplete = false;
            broadCastBanList.clear();

            if (!lostMessages.isEmpty()) {
//                Intent intent = new Intent(getApplicationContext(), SendLostMessage.class);
//                intent.putExtra("lostMessages", Parcels.wrap(lostMessages));
//                startService(intent);
                boolean isFoundTarget = false;

                Message[] messages = new Message[lostMessages.size()];
                messages = lostMessages.toArray(messages);

                for (Message message : messages) {
                    List<DeviceInfo> banList = new ArrayList<>();

                    for (DeviceInfo client : clients) {
                        if (message.getTargetId().equals(client.getClientNearbyKey()) ||
                                message.getTargetName().equals(client.getClientName())) {

                            //Доставили получателю
                            sendBroadcastMessage(message, true);
                            lostMessages.remove(message);
                            isFoundTarget = true;
                            debugLog("Send Lost Message on target");
                        }
                        banList.add(client);
                    }
                    broadCastBanList.put(message, banList);


                    if (!isFoundTarget) {
                        //Получатель не найден
                        sendBroadcastMessage(message, true);
                        debugLog("Send Lost Message on all");
                    }
                }
            }

            sendingLostMessageComplete = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupClientsRecyclerView();
        setupMessagesRecyclerView();

        myDevice = sharedPrefService.getInfoAboutMyDevice();
        setTitle(myDevice.getClientName());

        footer = findViewById(R.id.txt_peek);
        sendBroadcast = findViewById(R.id.btn_broadcast);
        textMessage = findViewById(R.id.text_message);
        sendMessage = findViewById(R.id.btn_send_message);
        searchClients = findViewById(R.id.btn_start_search);
        Button button = findViewById(R.id.btn_start_nearby);

        /** ChangeBroadcast */
        sendBroadcast.setOnClickListener(view -> {
            targetId = "";
            targetName = "";

            debugLog("Broadcast: On");

//            Nearby.Connections.stopAdvertising(googleApiClient);
//            Nearby.Connections.stopDiscovery(googleApiClient);
            Nearby.Connections.stopAllEndpoints(googleApiClient);
            googleApiClient.reconnect();

            clientsAdapter.clearAll();
            clients.clear();
        });

        /** SendMessage */
        sendMessage.setOnClickListener(view -> {
            //send Current Message
            if (targetId != null && !targetId.isEmpty() &&
                    targetName != null && !targetName.isEmpty()) {
                try {
                    sendBroadcastMessage(
                            Message.newMessage(myDevice.getClientName(), targetId, targetName, textMessage.getText().toString()),
                            false);
                } catch (IOException e) {
                    // TODO: 09.11.2017 Error Serialize
                    e.printStackTrace();
                }
            } else {
                if (!clients.isEmpty()) {
                    try {
                        sendBroadcastMessage(
                                Message.newMessage(myDevice.getClientName(), null, null, textMessage.getText().toString()),
                                false);
                    } catch (IOException e) {
                        // TODO: 09.11.2017 Error Serialize
                        e.printStackTrace();
                    }
                }
            }
        });


        /** Start advertising */
        button.setOnClickListener((View view) -> {
            if (button.getText().toString().contains("Star")) {
                button.setText("Stop Advertising");
                startSearchingClients();
            } else {
                button.setText("Start Advertising");
                stopSearchingClients();
            }
        });

        /** Start discovering */
        searchClients.setOnClickListener(view -> {
            if (searchClients.getText().toString().contains("Star")) {
                searchClients.setText("Stop discovering");
                startDiscovery();

                isDiscovering = true;
            } else {
                searchClients.setText("Start discovering");
                stopDiscovery();

                isDiscovering = false;
            }
        });

        checkPermition();

        createGoogleApiClient();
    }

    private List<String> createClientsEndPoints(Set<DeviceInfo> clients) {
        List<String> allEndPoints = new ArrayList<>();
        for (DeviceInfo endPoint : clients) {
            allEndPoints.add(endPoint.getClientNearbyKey());
        }
        return allEndPoints;
    }

    private void runTimerTask() {
        timer = new Timer();
        SendLostMessagesTask sendLostMessages = new SendLostMessagesTask();
        timer.schedule(sendLostMessages, 5000, 20000);
    }

    class SendLostMessagesTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(() -> {
//                if (!isDiscovering) {

                try {
                    debugLog("Timer SendLostMessage");

                    sendLostMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                }
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
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(connectionFailedListener)
                .addApi(Nearby.CONNECTIONS_API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();

        runTimerTask();
        debugLog("GoogleClient is Start");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        timer.cancel();

        debugLog("GoogleClient is Stop");
    }


    /**
     * ==========
     * 2 ЭТАП
     * ==========
     * startSearchingClients()
     * Запуск рекламации намерения стать точкой доступа
     */

    private void startSearchingClients() {
        if (!googleApiClient.isConnected()) {
            debugLog("Need run googleClient");
            return;
        }

        debugLog("start Advertising");

        Nearby.Connections.startAdvertising(
                googleApiClient,
                myDevice.getClientName(),
                SERVICE_ID,
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

    private void stopSearchingClients() {
        debugLog("stopSearchingClients");

        Nearby.Connections.stopAdvertising(googleApiClient);

        clientsAdapter.clearAll();
        clients.clear();
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
        if (!googleApiClient.isConnected()) {
            debugLog("Need run googleClient");
            return;
        }

        debugLog("start discovering");

        Nearby.Connections.startDiscovery(
                googleApiClient,
                SERVICE_ID,
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

    private void stopDiscovery() {
        Nearby.Connections.stopDiscovery(googleApiClient);

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
            debugLog("Found new endpoint :" + endpointId);

            // TODO: 08.11.2017 add UUID other client
            DeviceInfo temp = DeviceInfo.otherDevice(
                    discoveredEndpointInfo.getEndpointName(), endpointId, null);

            if (!clients.contains(temp)) {
                clients.add(DeviceInfo.otherDevice(
                        discoveredEndpointInfo.getEndpointName(), endpointId, null));

                clientsAdapter.setClient(
                        DeviceInfo.otherDevice(discoveredEndpointInfo.getEndpointName(), endpointId, null));

                //Auto Accept Connection
                //(Раскомментировать) Сам инициирует со всеми Коннект
//            requestConnection(DeviceInfo.otherDevice(
//                    discoveredEndpointInfo.getEndpointName(), endpointId, null));

                foundNewDevice.foundNewDevice(endpointId, discoveredEndpointInfo);
            }
        }

        @Override
        public void onEndpointLost(String endpointId) {
            debugLog("Lost endpoint: " + endpointId);
            footer.setText("PEEK");

            //удаляем отсоединившихся клиентов
            // TODO: 09.11.2017 Если удалять их, то как потом отправлять сообщения ?
            Nearby.Connections.disconnectFromEndpoint(googleApiClient, endpointId);

        }
    };

    private DeviceInfo findRemoveClient(String clientEndPoint) {
        for (Iterator<DeviceInfo> it = clients.iterator(); it.hasNext(); ) {
            DeviceInfo temp = it.next();
            if (temp.getClientNearbyKey().equals(clientEndPoint)) {
//                DeviceInfo deviceInfo =
//                        DeviceInfo.otherDevice(temp.getClientName(), temp.getClientNearbyKey(), temp.getUUID());
                clients.remove(temp);

                return temp;
            }
        }
        return DeviceInfo.empty();
    }

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
        // FIXME: 09.11.2017
        targetId = client.getClientNearbyKey();
        targetName = client.getClientName();

        debugLog(String.format("Current target: %s, %s", targetId, targetName));
        if (needRequestConnect) {
            //For test
            Nearby.Connections.disconnectFromEndpoint(googleApiClient, client.getClientNearbyKey());
            //
            requestConnection(client);
        }
    };

    /**
     * requestConnection
     * Запрос на соединение с клиентом
     */

    private void requestConnection(DeviceInfo client) {
        Nearby.Connections.requestConnection(
                googleApiClient,
                myDevice.getClientName(),
                client.getClientNearbyKey(),
                connectionLifecycleCallback)
                .setResultCallback(
                        status -> {
                            if (status.isSuccess()) {
                                debugLog("We successfully requested a connection");

                                targetId = client.getClientNearbyKey();
                                targetName = client.getClientName();

                                footer.setText(targetName);
                            } else {
                                debugLog("Nearby Connections failed" + status.getStatus());

                                Nearby.Connections.disconnectFromEndpoint(googleApiClient, client.getClientNearbyKey());
//                                clientsAdapter.removeClient(client);
                            }
                        }
                );
    }


    /**
     * ==========
     * 5 ЭТАП
     * ==========
     * sendTargetMessage || sendBroadcastMessage
     * Отправка сообщения
     */

    private void sendBroadcastMessage(Message message, boolean isLost) throws IOException {
        debugLog("Send broadcast");

        Nearby.Connections.sendPayload(
                googleApiClient,
                createClientsEndPoints(clients),
                Payload.fromBytes(
                        MessageConverter.toBytes(message))
        ).setResultCallback(status -> {
            if (status.isSuccess()) {
                debugLog("Send OK!!");

                if (!isLost) {
                    messagesAdapter.setMessages("ME: " + message.getText());
                    textMessage.setText("");

                    scrollToBottom();
                }
            } else {
                debugLog("Send FAIL!!" + status.getStatus());
            }
        });
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
            Message receivedMessage = null;
            try {
                receivedMessage = MessageConverter.getMessage(payload.asBytes());
            } catch (IOException | ClassNotFoundException e) {
                // TODO: 09.11.2017 ErrorCast Message
                e.printStackTrace();
            }

            String from = receivedMessage.getFrom();
            String targetId = receivedMessage.getTargetId();
            String targetName = receivedMessage.getTargetName();
            String textMessage = receivedMessage.getText();

            //Иначе это Broadcast
            if (targetId != null && targetName != null) {
                // FIXME: 09.11.2017 Должно быть что-то одно ()
                if (targetId.equals(myDevice.getClientNearbyKey()) || targetName.equals(myDevice.getClientName())) {
                    //Если сообщение нам
                    showReceivedMessage(from, textMessage);
                } else {
                    //Если сообщение не нам
                    // TODO: 09.11.2017 Логика передачи сообщения дальше по цепочке
                    // FIXME: 09.11.2017 Не добавлять одинаковые сообщения (Добавить UUID собщения)
                    if (!lostMessages.contains(receivedMessage)) {
                        lostMessages.add(receivedMessage);
                    }
                }
            } else {
                showReceivedMessage("", textMessage);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endPointId, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };


    private void searchTargetInNetwork(Message receivedMessage) {
//        lostMessages.add(receivedMessage);

        //Обновляем список клиентов
//        clients.clear();
//        startDiscovery();

    }

    private void showReceivedMessage(String from, String text) {
        messagesAdapter.setMessages(String.format("%s : %s", from, text));
        scrollToBottom();
    }

    /**
     * ConnectionLifecycleCallback
     * Оповещения о состоянии подключения
     */

    private ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endPoint, ConnectionInfo connectionInfo) {
            debugLog("onConnectionInitiated");

            // Automatically accept the connection on both sides.
            Nearby.Connections.acceptConnection(
                    googleApiClient, endPoint, payloadCallback)
                    .setResultCallback(status -> {
                        if (!status.isSuccess()) {
                            debugLog("onConnectionInitiated: OK!!!");
                        }
                    });

            targetId = endPoint;
            targetName = connectionInfo.getEndpointName();

            footer.setText(targetName);

            DeviceInfo temp = checkNewClient(endPoint, connectionInfo);

            if (temp.getState() != DeviceInfo.State.EMPTY) {
                clientsAdapter.setClient(temp);
            }
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution result) {
            debugLog("onConnectionResult");

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
        public void onDisconnected(String s) {
            debugLog("onDisconnected");

            targetId = "";
            targetName = "";
            footer.setText("PEEK"); // FIXME: 07.11.2017 Set Default Text

            Nearby.Connections.disconnectFromEndpoint(googleApiClient, s);
        }
    };


    /**
     * OTHER
     */

    /**
     * Проверка разрешений приложения (Для android 6.0 и выше)
     */
    private void checkPermition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        }
    }

    public void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                }, 0);  // TODO: 07.11.2017 RequestCode ?
    }

    private DeviceInfo checkNewClient(String endPoint, ConnectionInfo connectionInfo) {
        DeviceInfo tempClient = DeviceInfo.otherDevice(connectionInfo.getEndpointName(), endPoint, null);
        if (!clients.contains(tempClient)) {
            clients.add(tempClient);
            return tempClient;
        }
        return DeviceInfo.empty();
    }

    private void debugLog(String textLog) {
        Log.d(TAG, textLog);
        Toast.makeText(getApplicationContext(),
                textLog, Toast.LENGTH_SHORT).show();
    }


    private void setupClientsRecyclerView() {
        clientsRecyclerView = findViewById(R.id.recyclerView);
        clientsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        clientsAdapter = new ClientsAdapter();
        clientsAdapter.setListener(itemClickListener);
        clientsRecyclerView.setAdapter(clientsAdapter);
    }

    private void setupMessagesRecyclerView() {
        messagesRecyclerView = findViewById(R.id.massages_list);

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(true);

        messagesRecyclerView.setLayoutManager(linearLayoutManager);

        messagesAdapter = new MessagesAdapter();
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void scrollToBottom() {
        messagesRecyclerView.scrollToPosition(0);
    }
}