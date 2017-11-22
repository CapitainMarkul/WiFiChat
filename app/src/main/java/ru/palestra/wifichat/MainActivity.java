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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.parceler.Parcels;

import java.util.List;

import ru.palestra.wifichat.adapters.ClientsAdapter;
import ru.palestra.wifichat.data.models.mappers.ClientMapper;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.databinding.ActivityMainBinding;
import ru.palestra.wifichat.services.NearbyService;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.CreateUiListUtil;
import ru.palestra.wifichat.utils.Logger;

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

        startServices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(acceptConnectionToClientReceiver, new IntentFilter(ConfigIntent.ACTION_CONNECTION_INITIATED));
        registerReceiver(searchingNewClients, new IntentFilter(ConfigIntent.ACTION_DISCOVERY));

        setupWasConnectedClients();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(acceptConnectionToClientReceiver);
        unregisterReceiver(searchingNewClients);
        super.onStop();
    }

    private void startServices() {
        startService(new Intent(this, NearbyService.class));
    }

    private void stopServices() {
        stopService(new Intent(this, NearbyService.class));
    }

    private BroadcastReceiver acceptConnectionToClientReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<Client> clients = Parcels.unwrap(intent.getParcelableExtra(ConfigIntent.UPDATED_CLIENTS));
            clientsAdapter.updateClients(clients);
        }
    };


    private BroadcastReceiver searchingNewClients = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isDiscovery = intent.getBooleanExtra(ConfigIntent.STATUS_DISCOVERY, false);

            if (isDiscovery) {
                binding.progressDiscovery.setVisibility(View.VISIBLE);
            } else {
                binding.progressDiscovery.setVisibility(View.INVISIBLE);
            }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.default_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorite:
                new AlertDialog.Builder(this)
                        .setMessage("Уже уходите?")
                        .setNegativeButton("Нет", null)
                        .setPositiveButton("Ага", (arg0, arg1)
                                -> {
                            CreateUiListUtil.clearViewClients();

                            stopServices();
                            finish();
                        })
                        .create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private ClientsAdapter.ItemClick itemClickListener = (client) -> {
        Logger.debugLog(String.format("Start chat: %s - %s",
                client.getName(), client.getNearbyKey()));

        startActivity(new Intent(this, ChatActivity.class)
                .putExtra(ConfigIntent.CONNECTION_TARGET_CLIENT, Parcels.wrap(client)));
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
        clientsAdapter.updateClients(
                ClientMapper.toListClientView(App.dbClient().getAllWasConnectedClients()));
    }
}