package ru.palestra.wifichat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pgrenaud.android.p2p.entity.PeerEntity;
import com.pgrenaud.android.p2p.service.PeerService;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private RecyclerView clientsRecyclerView;
    private ClientsAdapter clientsAdapter;

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;

    private RecyclerView.LayoutManager layoutManager;

    private TextView footer;
    private Button sendMessage;
    private EditText textMessage;

    private PeerEntity currentPeer;
    private PeerService service; // TODO: 06.11.2017 createPeerService
    private boolean bound = false;
    private Intent nfcIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupClientsRecyclerView();
        setupMessagesRecyclerView();

        footer = findViewById(R.id.txt_peek);

//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        /**
         *  SendMessage
         * */
        textMessage = findViewById(R.id.text_message);

        sendMessage = findViewById(R.id.btn_send_message);
// TODO: 06.11.2017 send Message


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

        startSdkService();
    }

    /**
     * StartSdkService
     */

    private void startSdkService() {
        Intent intent = new Intent(this, PeerService.class);
//        intent.putExtra(PeerService.EXTRA_DIRECTORY_PATH, directoryPatch);
        intent.putExtra(PeerService.EXTRA_PEER_NAME, "Тестовое устройство");
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent intent = new Intent(this, PeerService.class);
        stopService(intent);
    }


    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, PeerService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (bound) {
            unbindService(serviceConnection);
            bound = false;
            service.setListener(null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            PeerService.PeerServiceBinder binder = (PeerService.PeerServiceBinder) iBinder;
            service = binder.getService();
            service.setListener(listener);

            service.registerNfcCallback(getActivity());
            service.handleNfcIntent(nfcIntent);

//            service.getPeerRepository(); // TODO: Initialize your UI
//            service.getSelfPeerEntity(); // TODO: Initialize your UI
//            service.getFileRepository(); // TODO: Initialize your UI

            service.getPeerHive().sync(); // Start workers for known peers

            nfcIntent = null;
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            service.setListener(null);

            service.unregisterNfcCallback(getActivity());

            bound = false;
        }
    };



    @Override
    protected void onResume() {
        super.onResume();

        nfcIntent = getIntent();
    }

    private PeerService.PeerServiceListener listener = new PeerService.PeerServiceListener() {
        @Override
        public void onPeerConnection(PeerEntity peerEntity) {
            Log.e(TAG, "onPeerConnection: " + peerEntity.getDisplayName());
        }

        @Override
        public void onPeerDisplayNameUpdate(PeerEntity peerEntity) {
            Log.e(TAG, "onPeerDisplayNameUpdate: " + peerEntity.getDisplayName());
        }

        @Override
        public void onPeerLocationUpdate(PeerEntity peerEntity) {
            Log.e(TAG, "onPeerLocationUpdate: " + peerEntity.getDisplayName());
        }

        @Override
        public void onPeerDirectoryChange(PeerEntity peerEntity) {
            Log.e(TAG, "onPeerDirectoryChange: " + peerEntity.getDisplayName());
        }
    };


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

    }

    private void stopSearchingClients() {

    }


    /**
     * MessageListener
     */

    /**
     * DiscoveryListener
     */

    /**
     * PeerListener
     */


//
//    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
//        @Override
//        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
//            Log.e(TAG, String.format("Found %d devices", wifiP2pDeviceList.getDeviceList().size()));
//            clientsAdapter.setClients(
//                    (List<WifiP2pDevice>) wifiP2pDeviceList.getDeviceList());
//        }
//    };

    /**
     * ItemClick
     */

    interface ItemClick {
        void onItemClick(PeerEntity device);
    }

    private ItemClick itemClickListener = new ItemClick() {
        @Override
        public void onItemClick(PeerEntity device) {

        }
    };

    public Activity getActivity() {
        return this;
    }
}

//    private ItemClick itemClickListener = device -> {
////        Log.d(TAG, "Выбрано устройство: " + device.getPeerId());
////        currentPeer = device;
////        connectToDevice(device);
//    };

//    private void connectToDevice(WifiP2pDevice device) {
//        manager.connect(channel, createWifiP2pConfig(device), actionListener);
//        manager.requestConnectionInfo(channel, connectionInfoListener);
//    }

//    private WifiP2pConfig createWifiP2pConfig(WifiP2pDevice wifiP2pDevice) {
//        WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
//        wifiP2pConfig.deviceAddress = wifiP2pDevice.deviceAddress;
//        wifiP2pConfig.wps.setup = WpsInfo.PBC;
//        return wifiP2pConfig;
//    }


//    WifiP2pManager.ActionListener actionListener = new WifiP2pManager.ActionListener() {
//        @Override
//        public void onSuccess() {
//
//        }
//
//        @Override
//        public void onFailure(int i) {
//
//        }
//    };

//    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
//        @Override
//        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
//            manager.stopPeerDiscovery(channel, actionListener);
//
//            footer.setText("Chat with: " + wifiP2pInfo.groupOwnerAddress);
//
//            if (wifiP2pInfo.isGroupOwner) {
//                setupServerSocket(wifiP2pInfo);
//            } else {
//                setupClientSocket(wifiP2pInfo);
//            }
//        }
//    };

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
//    private void checkConnection() {
//
//    }
//
//    private DataOutputStream dataOutputStream = null;
//
//    private DataInputStream dataInputStream = null;
//    private Socket socket = null;
//
//    private void startReadingResponse() throws IOException {
//        while (!Thread.currentThread().isInterrupted()
//                && !socket.isClosed()) {
////            dataInputStream.readUTF();
//
//            // TODO: 01.11.2017 То, что пришло помещаем в REcyclerView
//            messagesAdapter.setMessages(dataInputStream.readUTF());
//        }
//    }
//
//    // TODO: 01.11.2017 Нажатие кнопки, отправлять сообщения
//    private void sendMessage(String message) {
//        try {
//            dataOutputStream.writeUTF(message);
//            dataOutputStream.flush();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private void setupServerSocket(WifiP2pInfo wifiP2pInfo) {
//        new Thread(() -> {
//            ServerSocket serverSocket = null;
//            try {
//                serverSocket = new ServerSocket(1001);
//                while (true) {
//                    socket = serverSocket.accept(); //ожидание подключения
//
//                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
//                    dataInputStream = new DataInputStream(socket.getInputStream());
//
//                    startReadingResponse();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//    }
//
//    private void setupClientSocket(WifiP2pInfo wifiP2pInfo) {
//        new Thread(() -> {
//            try {
//                socket = new Socket();
//                socket.connect(
//                        new InetSocketAddress(wifiP2pInfo.groupOwnerAddress, 1001),
//                        5000);
//
//                dataOutputStream = new DataOutputStream(socket.getOutputStream());
//                dataInputStream = new DataInputStream(socket.getInputStream());
//
//                startReadingResponse();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                if (socket != null) {
//                    try {
//                        socket.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
//    }
//
//    private Socket createClientSocket() {
//
//    }

