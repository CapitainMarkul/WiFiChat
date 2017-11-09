package ru.palestra.wifichat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
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

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.adapters.ClientsAdapter;
import ru.palestra.wifichat.adapters.MessagesAdapter;
import ru.palestra.wifichat.model.DeviceInfo;
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

    private String currentEndPoint = "";

    private GoogleApiClient googleApiClient;
    // client's name that's visible to other devices when connecting
    private List<DeviceInfo> clients = new ArrayList<>();

    public static final String SERVICE_ID = "palestra.wifichat";
    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private SharedPrefServiceImpl sharedPrefService = new SharedPrefServiceImpl(this);

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
            currentEndPoint = "";
            Toast.makeText(getApplicationContext(), "Broadcast: On", Toast.LENGTH_SHORT).show();
        });

        /** SendMessage */
        sendMessage.setOnClickListener(view -> {
            //send Current Message
            if (currentEndPoint != null && !currentEndPoint.isEmpty()) {
                sendTargetMessage(currentEndPoint, textMessage.getText().toString());
            } else {
                if (!clients.isEmpty()) {
                    sendBroadcastMessage(textMessage.getText().toString());
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
            } else {
                searchClients.setText("Start discovering");
                stopDiscovery();
            }
        });

        checkPermition();

        createGoogleApiClient();
    }

    private List<String> createClientsEndPoints(List<DeviceInfo> clients) {
        List<String> allEndPoints = new ArrayList<>();
        for (DeviceInfo endPoint : clients) {
            allEndPoints.add(endPoint.getClientNearbyKey());
        }
        return allEndPoints;
    }


    /**
     * GoogleApiClient.ConnectionCallbacks
     * ???????????
     */

    GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

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

        debugLog("GoogleClient is Start");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }

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
            Toast.makeText(getApplicationContext(),
                    "Need run googleClient", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getApplicationContext(),
                "start Advertising", Toast.LENGTH_SHORT).show();

        Nearby.Connections.startAdvertising(
                googleApiClient,
                myDevice.getClientName(),
                SERVICE_ID,
                connectionLifecycleCallback,
                new AdvertisingOptions(STRATEGY))
                .setResultCallback(result -> {
                    if (result.getStatus().isSuccess()) {
                        Toast.makeText(getApplicationContext(),
                                "startAdvertising:onResult: SUCCESS", Toast.LENGTH_SHORT).show();
                        debugLog("startAdvertising:onResult: SUCCESS");
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "startAdvertising:onResult: FAILURE", Toast.LENGTH_SHORT).show();
                        debugLog("startAdvertising:onResult: FAILURE");
                    }
                });
    }

    /**
     * stopSearchingClients()
     * Прекращение намерения стать точкой доступа
     */

    private void stopSearchingClients() {
        Toast.makeText(getApplicationContext(),
                "stopSearchingClients", Toast.LENGTH_SHORT).show();

        Nearby.Connections.stopAdvertising(googleApiClient);
        clientsAdapter.clearAll();
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
            Toast.makeText(getApplicationContext(),
                    "Need run googleClient", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getApplicationContext(),
                "start discovering", Toast.LENGTH_SHORT).show();

        Nearby.Connections.startDiscovery(
                googleApiClient,
                SERVICE_ID,
                endpointDiscoveryCallback,
                new DiscoveryOptions(STRATEGY))
                .setResultCallback(
                        status -> {
                            if (status.isSuccess()) {
                                Toast.makeText(getApplicationContext(),
                                        "startDiscovery:onResult: SUCCESS", Toast.LENGTH_SHORT).show();
                                debugLog("startDiscovery:onResult: SUCCESS");
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "startDiscovery:onResult: FAILURE", Toast.LENGTH_SHORT).show();
                                debugLog("startDiscovery:onResult: FAILURE");
                            }
                        });
    }

    private void stopDiscovery() {
        Nearby.Connections.stopDiscovery(googleApiClient);

        Toast.makeText(getApplicationContext(),
                "stopDiscovery: SUCCESS", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getApplicationContext(),
                    "Found new endpoint :" + endpointId, Toast.LENGTH_SHORT).show();
            debugLog("Found new endpoint :" + endpointId);

            // TODO: 08.11.2017 add UUID other client
            clients.add(DeviceInfo.otherDevice(
                    discoveredEndpointInfo.getEndpointName(), endpointId, null));

            clientsAdapter.setClient(
                    DeviceInfo.otherDevice(discoveredEndpointInfo.getEndpointName(), endpointId, null));

            //Auto Accept Connection
            requestConnection(DeviceInfo.otherDevice(
                    discoveredEndpointInfo.getEndpointName(), endpointId, null));
        }

        @Override
        public void onEndpointLost(String endpointId) {
            Toast.makeText(getApplicationContext(),
                    "Lost endpoint: " + endpointId, Toast.LENGTH_SHORT).show();
            debugLog("Lost endpoint: " + endpointId);


            //удаляем отсоединившихся клиентов
            // TODO: 09.11.2017 Если удалять их, то как потом отправлять сообщения ?
            clientsAdapter.removeClient(
                    findRemoveClient(endpointId));
        }
    };

    private DeviceInfo findRemoveClient(String clientEndPoint) {
        //used i, for return deleted DeviceInfo
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getClientNearbyKey().equals(clientEndPoint)) {
                return clients.remove(i);
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
        void onItemClick(DeviceInfo client);
    }

    private ItemClick itemClickListener = client -> requestConnection(client);

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
                                Toast.makeText(getApplicationContext(),
                                        "We successfully requested a connection", Toast.LENGTH_SHORT).show();
                                debugLog("We successfully requested a connection");

                                currentEndPoint = client.getClientNearbyKey();
                                footer.setText(client.getClientNearbyKey());
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Nearby Connections failed", Toast.LENGTH_SHORT).show();
                                debugLog("Nearby Connections failed");
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

    private void sendTargetMessage(String targetEndPoint, String message) {
        Toast.makeText(getApplicationContext(),
                "Send message to" + currentEndPoint, Toast.LENGTH_SHORT).show();
        debugLog("Send message to" + currentEndPoint);

        Nearby.Connections.sendPayload(
                googleApiClient,
                targetEndPoint,
                Payload.fromBytes(message.getBytes())
        ).setResultCallback(status -> {
            if (status.isSuccess()) {
                Toast.makeText(getApplicationContext(),
                        "Send OK!!", Toast.LENGTH_SHORT).show();

                messagesAdapter.setMessages("ME: " + textMessage.getText().toString());
                textMessage.setText("");

                scrollToBottom();
            } else {
                Toast.makeText(getApplicationContext(),
                        "Send FAIL!!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendBroadcastMessage(String message) {
        Toast.makeText(getApplicationContext(),
                "Send broadcast", Toast.LENGTH_SHORT).show();
        debugLog("Send broadcast");

        Nearby.Connections.sendPayload(
                googleApiClient,
                createClientsEndPoints(clients),
                Payload.fromBytes(message.getBytes())
//                Payload.fromBytes(Parcels.wrap(
//                        Message.newMessage(
//                                myDevice.getClientName(),
//                                myDevice.getClientNearbyKey(),
//                                myDevice.getUUID())).toString().getBytes())
        ).setResultCallback(status -> {
            if (status.isSuccess()) {
                Toast.makeText(getApplicationContext(),
                        "Send OK!!", Toast.LENGTH_SHORT).show();

                messagesAdapter.setMessages("ME: " + textMessage.getText().toString());
                textMessage.setText("");

                scrollToBottom();
            } else {
                Toast.makeText(getApplicationContext(),
                        "Send FAIL!!", Toast.LENGTH_SHORT).show();
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
            messagesAdapter.setMessages(String.format("%s : %s", endPointId, new String(payload.asBytes())));

            scrollToBottom();
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
            Toast.makeText(getApplicationContext(),
                    "onConnectionInitiated", Toast.LENGTH_SHORT).show();

            // Automatically accept the connection on both sides.
            Nearby.Connections.acceptConnection(
                    googleApiClient, endPoint, payloadCallback)
                    .setResultCallback(status -> {
                        if (!status.isSuccess()) {
                            Toast.makeText(getApplicationContext(),
                                    "onConnectionInitiated: OK!!!", Toast.LENGTH_SHORT).show();
                        }
                    });

            currentEndPoint = endPoint;
            footer.setText(currentEndPoint);

            DeviceInfo temp = checkNewClient(endPoint, connectionInfo);

            if (temp.getState() != DeviceInfo.State.EMPTY) {
                clientsAdapter.setClient(temp);
            }
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution result) {
            Toast.makeText(getApplicationContext(),
                    "onConnectionResult", Toast.LENGTH_SHORT).show();

            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Toast.makeText(getApplicationContext(),
                            "Connect: OK", Toast.LENGTH_SHORT).show();
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Toast.makeText(getApplicationContext(),
                            "Connect: FAIL", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @Override
        public void onDisconnected(String s) {
            Toast.makeText(getApplicationContext(),
                    "onDisconnected", Toast.LENGTH_SHORT).show();

            currentEndPoint = null;
            footer.setText("PEEK"); // FIXME: 07.11.2017 Set Default Text
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
//        linearLayoutManager.setReverseLayout(true);

        messagesRecyclerView.setLayoutManager(linearLayoutManager);

        messagesAdapter = new MessagesAdapter();
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void scrollToBottom() {
        messagesRecyclerView.scrollToPosition(0);
    }
}