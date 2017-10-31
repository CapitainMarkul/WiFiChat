package ru.palestra.wifichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final IntentFilter intentFilter = new IntentFilter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        Button button = findViewById(R.id.btn_start_search);
        boolean bool = true;
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
        channel = manager.initialize(this, getMainLooper(), new WifiP2pManager.ChannelListener() {
            @Override
            public void onChannelDisconnected() {
                Log.e("Disconnect:", "true");
            }
        });

        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void startSearchingClients() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e("onSuccess", "true");
            }

            @Override
            public void onFailure(int i) {
                Log.e("onFailure", "true");
            }
        });
    }

    private void stopSearchingClients() {
        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e("StopOnSuccess", "true");
            }

            @Override
            public void onFailure(int i) {
                Log.e("StopOnFailure", "true");
            }
        });
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
                    // Wifi Direct mode is enabled
//                activity.setIsWifiP2pEnabled(true);
                } else {
//                activity.setIsWifiP2pEnabled(false);
//                activity.resetData();

                }
//            Log.d(Activity.TAG, "P2P state changed - " + state);
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                Log.e("PEERS_CHANGED_ACTION:", "true");
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
//            if (manager != null) {
//                manager.requestPeers(channel, (WifiP2pManager.PeerListListener) activity.getFragmentManager()
//                        .findFragmentById(R.id.frag_list));
//            }
//            Log.d(WiFiDirectActivity.TAG, "P2P peers changed");
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.e("ON_CHANGED_ACTION:", "true");
                if (manager == null) {
                    return;
                }

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {

                } else {
                    // It's a disconnect
//                activity.resetData();
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                Log.e("DEVICE_CHANGED_ACTION:", "true");
//            DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
//                    .findFragmentById(R.id.frag_list);
//            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
//                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

            }
        }
    };
}
