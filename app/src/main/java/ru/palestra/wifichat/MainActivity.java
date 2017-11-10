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
    private RecyclerView clientsRecyclerView;
    private RecyclerView messagesRecyclerView;

    private ClientsAdapter clientsAdapter;
    private MessagesAdapter messagesAdapter;

    private DeviceInfo myDevice;

    private TextView footer;
    private Button sendMessage;
    private Button searchClients;
    private Button sendBroadcast;
    private EditText textMessage;

    private String targetId = "";
    private String targetName = "";

    private GoogleApiClient googleApiClient;

    private List<Message> lostMessages = new ArrayList<>(); //Недоставленные сообщения
    private List<Message> deliveredLostMessages = new ArrayList<>(); //Доставленные "Недоставленные" сообщения

    private Set<DeviceInfo> clients = new HashSet<>();

    public static final String SERVICE_ID = "palestra.wifichat";
    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private Timer timer;

    private Map<Message, Set<DeviceInfo>> broadCastBanList = new HashMap<>();
    private boolean sendingLostMessageComplete = true;

    private SharedPrefServiceImpl sharedPrefService = new SharedPrefServiceImpl(this);

    public interface FoundNewDevice {
        void foundNewDevice(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo);
    }

    private FoundNewDevice foundNewDevice = (endpointId, discoveredEndpointInfo) -> {
        // После нахождения нового устройства проверяем, ему ли предназначалось сообщение
        try {
            for (Message message : lostMessages) {
                //Мы только что нашли того, кого искали. Немедленно коннектимся к нему
                if (message.getTargetId().equals(endpointId) ||
                        message.getTargetName().equals(discoveredEndpointInfo.getEndpointName())) {
                    requestConnection(
                            DeviceInfo.otherDevice(discoveredEndpointInfo.getEndpointName(), endpointId, null), message);
                }
            }
            sendLostMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    private void sendLostMessage() throws IOException {
        debugLog(String.format("Timer! Lost: %s, Deliver: %s", lostMessages.size(), deliveredLostMessages.size()));

        //Защита, если мы еще не успели всем передать сообщение, а уже сработало событие
        if (sendingLostMessageComplete) {   //Lock.class ??
            sendingLostMessageComplete = false;
            broadCastBanList.clear();

            if (!lostMessages.isEmpty()) {
//                Intent intent = new Intent(getApplicationContext(), SendLostMessage.class);
//                intent.putExtra("lostMessages", Parcels.wrap(lostMessages));
//                startService(intent);

                Message[] messages = new Message[lostMessages.size()];
                messages = lostMessages.toArray(messages);

                for (Message message : messages) {
                    for (DeviceInfo client : clients) {
                        //Проверяем, пытались ли мы уже отправлять сообщение данному клиенту
                        if (broadCastBanList.get(message) == null ||
                                !broadCastBanList.get(message).contains(client)) {
                            //Проверяем, это тот клиент который нам нужен
                            if (message.getTargetId().equals(client.getClientNearbyKey()) ||
                                    message.getTargetName().equals(client.getClientName())) {

                                //Доставили получателю
                                deliverTargetMessage(message);
                                break;
                            }
                        }
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

        myDevice = sharedPrefService.getInfoAboutMyDevice();

        setupClientsRecyclerView();
        setupMessagesRecyclerView();

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
            footer.setText("PEEK");
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
                            Message.newMessage(myDevice.getClientName(), targetId, targetName, textMessage.getText().toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                debugLog("А кому доставлять-то ?");
            }
//            else {
//                if (!clients.isEmpty()) {
//                    try {
//                        sendBroadcastMessage(
//                                Message.newMessage(myDevice.getClientName(), null, null, textMessage.getText().toString()));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
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
            } else {
                searchClients.setText("Start discovering");
                stopDiscovery();
            }
        });

        checkPermition();

        createGoogleApiClient();
    }

    private List<String> createClientsEndPoints(Set<DeviceInfo> clients, String fromName) {
        List<String> allEndPoints = new ArrayList<>();
        for (DeviceInfo endPoint : clients) {
            //Сообщение не нужно передавать назад отправителю
            if (!endPoint.getClientName().equals(fromName)) {
                allEndPoints.add(endPoint.getClientNearbyKey());
            }
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
                try {
                    sendLostMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            debugLog("Found new endpoint: " + endpointId);

            DeviceInfo temp = DeviceInfo.otherDevice(
                    discoveredEndpointInfo.getEndpointName(), endpointId, null);

            boolean isNewDevice = true;
            for (DeviceInfo device : clients) {
                if (device.getClientName().equals(discoveredEndpointInfo.getEndpointName())) {
                    isNewDevice = false;

                    //что-то произошло с прошлым устройством
                    removeDisconnectedClient(endpointId);
                    break;
                }
            }

            if (isNewDevice) {
                clients.add(temp);
                clientsAdapter.setClient(temp);

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
//            Nearby.Connections.disconnectFromEndpoint(googleApiClient, client.getClientNearbyKey());
            //
            stopDiscovery();
            searchClients.setText("Start Discovering");

            requestConnection(client, null);
//            startDiscovery();
        }
    };

    /**
     * requestConnection
     * Запрос на соединение с клиентом
     */

    private void requestConnection(DeviceInfo client, Message message) {
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

                                if (message != null) {
                                    try {
                                        deliverTargetMessage(message);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
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
     * deliverTargetMessage || sendBroadcastMessage
     * Отправка сообщения
     */

    private void deliverTargetMessage(Message message) throws IOException {
        debugLog("Send target: " + message.getTargetId());

        Nearby.Connections.sendPayload(
                googleApiClient,
                message.getTargetId(),
                Payload.fromBytes(
                        MessageConverter.toBytes(message))
        ).setResultCallback(status -> {
            if (status.isSuccess()) {
                debugLog("Send OK!!");

                deliveredLostMessages.add(message);
                lostMessages.remove(message);
            } else {
                debugLog("Send FAIL!!" + status.getStatus());

                //Получатель не найден или не удалось доставить
                try {
                    sendBroadcastMessage(message);

                    broadCastBanList.put(message, clients); //Сохраняем всех подключенных клиентов, кому пытались отправить сообщение
                    debugLog("Send Lost Message on all");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendBroadcastMessage(Message message) throws IOException {
        debugLog("Send broadcast");

        Nearby.Connections.sendPayload(
                googleApiClient,
                createClientsEndPoints(clients, message.getFrom()),
                Payload.fromBytes(
                        MessageConverter.toBytes(message))
        ).setResultCallback(status -> {
            if (status.isSuccess()) {
                debugLog("Send OK!!");

                if (message.getFrom().equals(myDevice.getClientName())) {
                    showMyMessage(message);

                    scrollToBottom();
                }
            } else {
                debugLog("Send FAIL!!" + status.getStatus());
            }
        });
    }

    private void showMyMessage(Message message) {
        messagesAdapter.setMessages(message);
        textMessage.setText("");
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
            Message receivedMessage;
            try {
                receivedMessage = MessageConverter.getMessage(payload.asBytes());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }

            //Проверяем это Новое сообщение, или ответ о доставленном сообщении
            if (receivedMessage.getState() == Message.State.DELIVERED_MESSAGE) {
                lostMessages.remove(receivedMessage.getDeliveredMessage());
                deliveredLostMessages.add(receivedMessage.getDeliveredMessage());
            } else {
                //Работаем с обычным типом сообщения
                //Определим, это совершенно новое сообщение, или это сообщение мы уже кому-то доставили
                for (Message message : deliveredLostMessages) {
                    if (deliveredLostMessages.contains(receivedMessage)) {
                        //даем ответ отправителю, что такое сообщение уже отправлено, и следует прекратить транслировать его
                        try {
                            deliverTargetMessage(
                                    Message.deliveredMessage(endPointId, message));
                            return; //Нам не имеет смысла обрабатывать это сообщение, мы его уже доставили
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                }

                String targetId = receivedMessage.getTargetId();
                String targetName = receivedMessage.getTargetName();

                //Иначе это Broadcast
                if (targetId != null && targetName != null) {
                    // FIXME: 09.11.2017 Должно быть что-то одно ()
                    if (targetId.equals(myDevice.getClientNearbyKey()) ||
                            targetName.equals(myDevice.getClientName())) {
                        //Если сообщение нам
                        messagesAdapter.setMessages(receivedMessage);
                        scrollToBottom();
                    } else {
                        //Если сообщение не нам
                        if (!lostMessages.contains(receivedMessage)) {
                            lostMessages.add(receivedMessage);
                        }
                    }
                }
            }
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
            debugLog("onConnectionInitiated");


//            try {
//                Thread.sleep(200);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

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
        public void onDisconnected(String endPoint) {
            debugLog("onDisconnected");

            targetId = "";
            targetName = "";
            footer.setText("PEEK"); // FIXME: 07.11.2017 Set Default Text

            Nearby.Connections.disconnectFromEndpoint(googleApiClient, endPoint);

            clientsAdapter.removeClient(
                    removeDisconnectedClient(endPoint));
        }
    };

    private DeviceInfo removeDisconnectedClient(String endPoint) {
        DeviceInfo[] devicesInfo = new DeviceInfo[clients.size()];
        devicesInfo = clients.toArray(devicesInfo);

        //Удаляем дубликаты клиентов с неверными точками доступа
        for (int i = 0; i < devicesInfo.length; i++) {
            if (devicesInfo[i].getClientNearbyKey().equals(endPoint)) {
                clients.remove(devicesInfo[i]);
                return devicesInfo[i];
            }
        }
        return DeviceInfo.empty();
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
        messagesAdapter.setCurrentDevice(myDevice);
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void scrollToBottom() {
        messagesRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1);
    }
}