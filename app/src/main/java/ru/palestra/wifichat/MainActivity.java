package ru.palestra.wifichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final IntentFilter intentFilter = new IntentFilter();

    private RecyclerView clientsRecyclerView;
    private ClientsAdapter clientsAdapter;

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;

    private RecyclerView.LayoutManager layoutManager;

    private TextView footer;
    private Button sendMessage;
    private EditText textMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupClientsRecyclerView();
        setupMessagesRecyclerView();

        footer = findViewById(R.id.txt_peek);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        /**
         *  SendMessage
         * */
        textMessage = findViewById(R.id.text_message);

        sendMessage = findViewById(R.id.btn_send_message);
        sendMessage.setOnClickListener(view -> {
            sendMessage(textMessage.getText().toString());
            textMessage.setText("");
        });


        Button button = findViewById(R.id.btn_start_search);

        button.setOnClickListener((View view) -> {
            if (button.getText().toString().contains("Star")) {
                button.setText("Stop searching");
                startSearchingClients();
            } else {
                button.setText("Start searching");
                stopSearchingClients();
            }
        });

        manager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        registerReceiver(broadcastReceiver, intentFilter);
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
        messagesRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        messagesAdapter = new MessagesAdapter();
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void startSearchingClients() {
        manager.discoverPeers(channel, actionListener);
    }

    private void stopSearchingClients() {
        manager.stopPeerDiscovery(channel, actionListener);
    }

//    WIFI_P2P_STATE_CHANGED_ACTION
//    Показывает включен ли Wi-Fi P2P

//    WIFI_P2P_PEERS_CHANGED_ACTION
//    Указывает, что список доступных узлов изменился.

//    WIFI_P2P_CONNECTION_CHANGED_ACTION
//    Указывает, что состояние Wi-Fi P2P соединения изменилось.

//    WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
//    Указывает, что детали конфигурации этого устройства изменились.

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                Log.e("STATE_CHANGED_ACTION:", "true");
                // UI update to indicate wifi p2p status.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                } else {

                }
//            Log.d(Activity.TAG, "P2P state changed - " + state);
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                Log.e("PEERS_CHANGED_ACTION:", "true");

                if (manager != null) {
                    manager.requestPeers(channel, peerListListener);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.e("ON_CHANGED_ACTION:", "true");
                if (manager == null) {
                    return;
                }

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {

                } else {

                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                Log.e("DEVICE_CHANGED_ACTION:", "true");

            }
        }
    };

    /**
     * PeerListener
     */

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            Log.e(TAG, String.format("Found %d devices", wifiP2pDeviceList.getDeviceList().size()));
            clientsAdapter.setClients(
                    (List<WifiP2pDevice>) wifiP2pDeviceList.getDeviceList());
        }
    };

    /**
     * ItemClick
     */

    interface ItemClick {
        void onItemClick(WifiP2pDevice device);
    }

    private ItemClick itemClickListener = device -> connectToDevice(device);

    private void connectToDevice(WifiP2pDevice device) {
        manager.connect(channel, createWifiP2pConfig(device), actionListener);
        manager.requestConnectionInfo(channel, connectionInfoListener);
    }

    private WifiP2pConfig createWifiP2pConfig(WifiP2pDevice wifiP2pDevice) {
        WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
        wifiP2pConfig.deviceAddress = wifiP2pDevice.deviceAddress;
        wifiP2pConfig.wps.setup = WpsInfo.PBC;
        return wifiP2pConfig;
    }

    /**
     * ResultConnect
     */

    WifiP2pManager.ActionListener actionListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailure(int i) {

        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            manager.stopPeerDiscovery(channel, actionListener);

            footer.setText("Chat with: " + wifiP2pInfo.groupOwnerAddress);

            if (wifiP2pInfo.isGroupOwner) {
                setupServerSocket(wifiP2pInfo);
            } else {
                setupClientSocket(wifiP2pInfo);
            }
        }
    };

    /**
     * Setup Sockets
     */

//    public Observable<String> send(String message) {
//        return Observable.just(message)
//                .doOnNext(cmd -> checkConnection())
//                .map(cmd -> cmd.getBytes())
//                .map(bytes -> addHeader(bytes))
//                .map(bytes -> sendBytes(bytes))
//                .timeout(MAX_SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS)
//                .map(result -> readAnswer())
//                .doOnError(throwable -> disconnect())
//                .retry(MAX_RETRY_COUNT)
//                .subscribeOn(Schedulers.io());
//    }
    private void checkConnection() {

    }

    private DataOutputStream dataOutputStream = null;

    private DataInputStream dataInputStream = null;
    private Socket socket = null;

    private void startReadingResponse() throws IOException {
        while (!Thread.currentThread().isInterrupted()
                && !socket.isClosed()) {
//            dataInputStream.readUTF();

            // TODO: 01.11.2017 То, что пришло помещаем в REcyclerView
            messagesAdapter.setMessages(dataInputStream.readUTF());
        }
    }

    // TODO: 01.11.2017 Нажатие кнопки, отправлять сообщения
    private void sendMessage(String message) {
        try {
            dataOutputStream.writeUTF(message);
            dataOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void setupServerSocket(WifiP2pInfo wifiP2pInfo) {
        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(1001);
                while (true) {
                    socket = serverSocket.accept(); //ожидание подключения

                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataInputStream = new DataInputStream(socket.getInputStream());

                    startReadingResponse();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void setupClientSocket(WifiP2pInfo wifiP2pInfo) {
        new Thread(() -> {
            try {
                socket = new Socket();
                socket.connect(
                        new InetSocketAddress(wifiP2pInfo.groupOwnerAddress, 1001),
                        5000);

                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                startReadingResponse();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
//
//    private Socket createClientSocket() {
//
//    }
}
