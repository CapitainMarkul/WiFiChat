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

import java.util.List;

import ru.palestra.wifichat.adapters.ClientsAdapter;
import ru.palestra.wifichat.data.models.mappers.ClientMapper;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.databinding.ActivityMainBinding;
import ru.palestra.wifichat.services.NearbyService;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.Logger;
import ru.palestra.wifichat.utils.CreateUiListUtil;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;

    private ClientsAdapter clientsAdapter;

    private Client myDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        myDevice = App.sharedPreference().getInfoAboutMyDevice();
        setTitle(myDevice.getName());

        checkPermission();

        setupClientsRecyclerView();
        setupWasConnectedClients();

        registerReceiver(acceptConnectionToClientReceiver, new IntentFilter(ConfigIntent.ACTION_CONNECTION_INITIATED));
        startServices();
    }

    @Override
    protected void onDestroy() {
        stopServices();
        super.onDestroy();
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
            List<Client> clients = intent.getParcelableArrayListExtra(ConfigIntent.UPDATED_CLIENTS);
            clientsAdapter.updateClients(clients);
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
    protected void onResume() {
        super.onResume();

        //Проверим, появились ли новые, подключенные клиенты
        if (CreateUiListUtil.getUiClients().size() > 0) {
            clientsAdapter.updateClients(
                    CreateUiListUtil.getUiClients());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        Logger.debugLog(String.format("Start chat: %s - %s",
                client.getName(), client.getNearbyKey()));

        // TODO: 16.11.2017 Create New Chat Goto New Activity
        startActivity(
                new Intent(this, ChatActivity.class)
                        .putExtra(ConfigIntent.CONNECTION_TARGET_ID, client.getNearbyKey())
                        .putExtra(ConfigIntent.CONNECTION_TARGET_NAME, client.getName())
                        .putExtra(ConfigIntent.CONNECTION_TARGET_UUID, client.getUUID()));
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

    private void setupWasConnectedClients() {
        // FIXME: 18.11.2017 Работа с БД?
        clientsAdapter.updateClients(
                ClientMapper.toListClientView(App.dbClient().getAllWasConnectedClients()));
    }
}