package ru.palestra.wifichat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.adapters.ClientsAdapter;
import ru.palestra.wifichat.adapters.MessagesAdapter;
import ru.palestra.wifichat.data.models.mappers.ClientMapper;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.databinding.ActivityMainBinding;
import ru.palestra.wifichat.services.NearbyService;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.Logger;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;

    private ClientsAdapter clientsAdapter;
    private MessagesAdapter messagesAdapter;

    private String targetId;
    private String targetUUID;

    private Client myDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        myDevice = App.sharedPreference().getInfoAboutMyDevice();
        setTitle(myDevice.getName());

        checkPermission();

        setupClientsRecyclerView();
        setupMessagesRecyclerView();

        setupWasConnectedClients();

        registerReceiver(acceptConnectionToClientReceiver, new IntentFilter(ConfigIntent.ACTION_CONNECTION_INITIATED));
        registerReceiver(deliveredMessageReceiver, new IntentFilter(ConfigIntent.ACTION_DELIVERED_MESSAGE));

        /** setDefaultOptions */
        binding.btnDefaultOption.setOnClickListener(view -> {

        });

        /** SendMessage */
        binding.bottomSheet.btnSendMessage.setOnClickListener(view -> {
            if (targetUUID == null || targetId == null) return;

            Message sendMessage =
                    Message.newMessage(myDevice.getName(), myDevice.getUUID(), targetId, targetUUID, binding.bottomSheet.textMessage.getText().toString());
            startService(
                    new Intent(this, NearbyService.class)
                            .putExtra(ConfigIntent.MESSAGE, sendMessage));

            messagesAdapter.setMessages(sendMessage);
            binding.bottomSheet.textMessage.setText("");
        });
    }

    private void startServices() {
        startService(new Intent(this, NearbyService.class));
    }

    private void stopServices() {
        stopService(new Intent(this, NearbyService.class));
    }

    BroadcastReceiver acceptConnectionToClientReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String idEndPoint = intent.getStringExtra(ConfigIntent.CONNECTION_TARGET_ID);
            String nameEndPoint = intent.getStringExtra(ConfigIntent.CONNECTION_TARGET_NAME);
            String UUIDEndPoint = intent.getStringExtra(ConfigIntent.CONNECTION_TARGET_UUID);
            String footerText = intent.getStringExtra(ConfigIntent.CONNECTION_FOOTER_TEXT);
            boolean isDisconnect = intent.getBooleanExtra(ConfigIntent.CONNECTION_TARGET_IS_DISCONNECT, true);

            binding.bottomSheet.txtPeek.setText(footerText);

            List<Client> allClientsAdapter = new ArrayList<>();
            allClientsAdapter.addAll(clientsAdapter.getAllClients());

            // TODO: 18.11.2017 Set UUID
            Client newClient = Client.otherDevice(nameEndPoint, idEndPoint, UUIDEndPoint);


            if (!isDisconnect) {
                //Если клиент подключился
                for (Client oldClient : allClientsAdapter) {
                    if (oldClient.getUUID().equals(newClient.getUUID())) {
                        int indexClient = allClientsAdapter.indexOf(oldClient);
                        allClientsAdapter.remove(indexClient);
                        allClientsAdapter.add(indexClient, Client.isOnline(oldClient));
// FIXME: 18.11.2017 Говнокод
                        clientsAdapter.updateClients(allClientsAdapter);
                        return;
                    }
                }

                allClientsAdapter.add(Client.isOnline(newClient));

            } else {
                for (Client oldClient : allClientsAdapter) {
                    if (oldClient.getNearbyKey().equals(idEndPoint)) {
                        int indexClient = allClientsAdapter.indexOf(oldClient);
                        allClientsAdapter.remove(indexClient);
                        allClientsAdapter.add(indexClient, Client.isOffline(oldClient));
                        break;
                    }
                }
            }

            clientsAdapter.updateClients(allClientsAdapter);
        }
    };

    BroadcastReceiver deliveredMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getExtras() == null) return;

            Message message =
                    (Message) intent.getSerializableExtra(ConfigIntent.MESSAGE);

            messagesAdapter.setMessages(message);
            scrollToBottom();
        }
    };


    /**
     * ==========
     * 1 ЭТАП
     * ==========
     * Создание главного объекта доступа – GoogleApiClient.
     * Запуск клиента. Остановка клиента.
     */

    @Override
    protected void onStart() {
        super.onStart();

        startServices();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopServices();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Уже уходите?")
                .setNegativeButton("Нет", null)
                .setPositiveButton("Ага", (arg0, arg1)
                        -> MainActivity.super.onBackPressed())
                .create().show();
    }

    private ClientsAdapter.ItemClick itemClickListener = (client) -> {
        // TODO: 16.11.2017 Create New Chat Goto New Activity

        Logger.debugLog(String.format("Current target: %s - %s",
                client.getName(), client.getNearbyKey()));

        targetId = client.getNearbyKey();
        targetUUID = client.getUUID();
    };


    /**
     * Проверка разрешений приложения (Для android 6.0 и выше)
     */
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestPermission();
        }
    }

    public void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                }, 0);
    }

    private void setupClientsRecyclerView() {
        binding.rvClients.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        clientsAdapter = new ClientsAdapter();
        clientsAdapter.setListener(itemClickListener);
        binding.rvClients.setAdapter(clientsAdapter);
    }

    private void setupMessagesRecyclerView() {
        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(true);

        binding.bottomSheet.massagesList.setLayoutManager(linearLayoutManager);

        messagesAdapter = new MessagesAdapter();
        messagesAdapter.setCurrentDevice(
                App.sharedPreference().getInfoAboutMyDevice().getName());
        binding.bottomSheet.massagesList.setAdapter(messagesAdapter);
    }

    private void setupWasConnectedClients() {
        // FIXME: 18.11.2017 Работа с БД?
        clientsAdapter.updateClients(
                ClientMapper.toListClientView(App.dbClient().getAllWasConnectedClients()));
    }

    private void scrollToBottom() {
        binding.bottomSheet.massagesList.scrollToPosition(messagesAdapter.getItemCount() - 1);
    }
}