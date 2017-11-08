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

import ru.palestra.wifichat.model.DeviceInfo;
import ru.palestra.wifichat.services.SharedPrefServiceImpl;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    private RecyclerView clientsRecyclerView;
    private ClientsAdapter clientsAdapter;

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;

    private TextView footer;
    private Button sendMessage;
    private Button searchClients;
    private EditText textMessage;

    private String currentEndPoint = "";

    private GoogleApiClient googleApiClient;
    // client's name that's visible to other devices when connecting
    public static final String CLIENT_NAME = "New NickName";
    public static final String SERVICE_ID = "palestra.wifichat";
    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private SharedPrefServiceImpl sharedPrefService = new SharedPrefServiceImpl(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupClientsRecyclerView();
        setupMessagesRecyclerView();

        setTitle(sharedPrefService.getInfoAboutMyDevice().getClientName());

        footer = findViewById(R.id.txt_peek);

        /**
         *  SendMessage
         * */
        textMessage = findViewById(R.id.text_message);

        sendMessage = findViewById(R.id.btn_send_message);
        sendMessage.setOnClickListener(view -> {
            if (currentEndPoint != null) {
                Toast.makeText(getApplicationContext(),
                        "Send to" + currentEndPoint, Toast.LENGTH_SHORT).show();
                Nearby.Connections.sendPayload(
                        googleApiClient,
                        currentEndPoint,
                        Payload.fromBytes(textMessage.getText().toString().getBytes())
                ).setResultCallback(status -> {
                    if (status.isSuccess()) {
                        Toast.makeText(getApplicationContext(),
                                "Send OK!!", Toast.LENGTH_SHORT).show();
                        // We're discovering!
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Send FAIL!!", Toast.LENGTH_SHORT).show();
                        // We were unable to start discovering.
                    }
                });

                messagesAdapter.setMessages(textMessage.getText().toString());
                textMessage.setText("");
            }
        });

        Button button = findViewById(R.id.btn_start_nearby);

        button.setOnClickListener((View view) -> {
            if (button.getText().toString().contains("Star")) {
                button.setText("Stop searching");
                startSearchingClients();
            } else {
                button.setText("Start searching");
                stopSearchingClients();
            }
        });

        searchClients = findViewById(R.id.btn_start_search);
        searchClients.setOnClickListener(view ->
                Nearby.Connections.startDiscovery(
                        googleApiClient,
                        SERVICE_ID,
                        endpointDiscoveryCallback,
                        new DiscoveryOptions(STRATEGY))
                        .setResultCallback(
                                status -> {
                                    if (status.isSuccess()) {
                                        Toast.makeText(getApplicationContext(),
                                                "We're discovering!", Toast.LENGTH_SHORT).show();
                                        // We're discovering!
                                    } else {
                                        Toast.makeText(getApplicationContext(),
                                                "We were unable to start discovering", Toast.LENGTH_SHORT).show();
                                        // We were unable to start discovering.
                                    }
                                }));

        checkPermition();

        createGoogleApiClient();
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
                    "An endpoint was found!" + endpointId, Toast.LENGTH_SHORT).show();

            clientsAdapter.setClient(
                    DeviceInfo.otherDevice(discoveredEndpointInfo.getEndpointName(), endpointId, null));
        }

        @Override
        public void onEndpointLost(String endpointId) {
            Toast.makeText(getApplicationContext(),
                    "A previously discovered endpoint has gone away", Toast.LENGTH_SHORT).show();

        }
    };

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


    private void createGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(connectionFailedListener)
                .addApi(Nearby.CONNECTIONS_API)
                .build();
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

    @Override
    protected void onStart() {
        super.onStart();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    /**
     * startSearchingClients()
     * Сканирование сети, поиск клиентов
     */

    private void startSearchingClients() {
        Toast.makeText(getApplicationContext(),
                "start Advertising", Toast.LENGTH_SHORT).show();

        Nearby.Connections.startAdvertising(
                googleApiClient,
                CLIENT_NAME,
                SERVICE_ID,
                connectionLifecycleCallback,
                new AdvertisingOptions(STRATEGY))
                .setResultCallback(result -> {
                    if (result.getStatus().isSuccess()) {
                        Toast.makeText(getApplicationContext(),
                                "We're advertising!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "We were unable to start advertising", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * startSearchingClients()
     * Прекращение поиска клиентов
     */

    private void stopSearchingClients() {
        Toast.makeText(getApplicationContext(),
                "stopSearchingClients", Toast.LENGTH_SHORT).show();

        clientsAdapter.clearAll();
    }

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
            footer.setText("PICK"); // FIXME: 07.11.2017 Set Default Text
        }
    };


    /**
     * PayloadCallback
     * Прием сообщений от других клиентов
     */

    private PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endPointId, Payload payload) {
            messagesAdapter.setMessages(new String(payload.asBytes()));
        }

        @Override
        public void onPayloadTransferUpdate(String endPointId, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    /**
     * requestConnection
     * Запрос на соединение с клиентом
     */

    private void requestConnection(DeviceInfo client) {
        Nearby.Connections.requestConnection(
                googleApiClient,
                CLIENT_NAME,
                client.getClientNearbyKey(),
                connectionLifecycleCallback)
                .setResultCallback(
                        status -> {
                            if (status.isSuccess()) {
                                Toast.makeText(getApplicationContext(),
                                        "We successfully requested a connection", Toast.LENGTH_SHORT).show();

                                currentEndPoint = client.getClientNearbyKey();
                                footer.setText(client.getClientNearbyKey());
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Nearby Connections failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
    }

    /**
     * ItemClick
     */

    interface ItemClick {
        void onItemClick(DeviceInfo client);
    }

    private ItemClick itemClickListener = client -> requestConnection(client);

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
        messagesRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        messagesAdapter = new MessagesAdapter();
        messagesRecyclerView.setAdapter(messagesAdapter);
    }
}