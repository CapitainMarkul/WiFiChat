package ru.palestra.wifichat.services;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ru.palestra.wifichat.model.DeviceInfo;

/**
 * Created by da.pavlov1 on 07.11.2017.
 */

public class SharedPrefServiceImpl {
    private final Context context;

    private static final String PREF_FILE_KEY = "pref_offline_chat";
    private static final String PREF_KEY_NAME_MY_DEVICE = "name_device";
    private static final String PREF_KEY_UUID_MY_DEVICE = "UUID_device";
    private static final String PREF_KEY_WAS_CONNECTED_CLIENTS = "was_connected_clients";

    public SharedPrefServiceImpl(Context context) {
        this.context = context;
    }

    public void saveWasConnectedClient(DeviceInfo client) {
        //Проверим есть ли такой клиент в нашей "Базе"
        List<DeviceInfo> savedClients = getAllWasConnectedClient();

        DeviceInfo[] savedClientsArray = new DeviceInfo[savedClients.size()];
        savedClientsArray = savedClients.toArray(savedClientsArray);

        for (DeviceInfo savedClient : savedClientsArray) {
            if (savedClient.getClientNearbyKey().equals(client.getClientNearbyKey())) {
                return;
            } else if (savedClient.getClientName().equals(client.getClientName())) {
                //Если имена совпадают, а точки различаются, то обновляем точку
                removeWasConnectedClient(savedClient);
                break;
            }
        }

        saveWasConnectedClients(Collections.singletonList(client));
    }

    private void saveWasConnectedClients(List<DeviceInfo> clients) {
        HashMap<String, String> clientsMap = new HashMap<>();
        for (DeviceInfo client : clients) {
            clientsMap.put(client.getClientNearbyKey(), client.getClientName());
        }

        saveWasConnectedClientsPref(clientsMap);
    }

    private void saveWasConnectedClientsPref(Map<String, String> clients) {
        Gson gson = new Gson();
        String hashMapString = gson.toJson(clients);

        getPrefFile().edit()
                .putString(PREF_KEY_WAS_CONNECTED_CLIENTS, hashMapString)
                .apply();
    }

    public void removeWasConnectedClient(DeviceInfo removedClient) {
        List<DeviceInfo> potentialClients = getAllWasConnectedClient();

        if (removedClient.getClientName() != null) {
            potentialClients.remove(removedClient);
        } else {
            DeviceInfo[] potentialClientsArray = new DeviceInfo[potentialClients.size()];
            potentialClientsArray = potentialClients.toArray(potentialClientsArray);

            for (DeviceInfo client : potentialClientsArray) {
                if (client.getClientNearbyKey().equals(removedClient.getClientNearbyKey())) {
                    potentialClients.remove(client);
                }
            }
        }

        saveWasConnectedClients(potentialClients);
    }

    public List<DeviceInfo> getAllWasConnectedClient() {
        List<DeviceInfo> potentialClients = new ArrayList<>();

        String storedHashMapString = getPrefFile().getString(PREF_KEY_WAS_CONNECTED_CLIENTS, null);
        if (storedHashMapString == null) return Collections.emptyList();

        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, String>>() {
        }.getType();
        HashMap<String, String> testHashMap2 = gson.fromJson(storedHashMapString, type);

        Iterator it = testHashMap2.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry client = (Map.Entry) it.next();
            potentialClients.add(
                    DeviceInfo.otherDevice(client.getValue().toString(), client.getKey().toString(), null));
        }

        return potentialClients;
    }

    public void saveInfoAboutMyDevice(DeviceInfo myDevice) {
        getPrefFile().edit()
                .putString(PREF_KEY_NAME_MY_DEVICE, myDevice.getClientName())
                .putString(PREF_KEY_UUID_MY_DEVICE, myDevice.getUUID())
                .apply();
    }

    public DeviceInfo getInfoAboutMyDevice() {
        String name = getPrefFile().getString(PREF_KEY_NAME_MY_DEVICE, null);
        String UUID = getPrefFile().getString(PREF_KEY_UUID_MY_DEVICE, null);

        return name != null && UUID != null ?
                DeviceInfo.myDevice(name, UUID) : DeviceInfo.empty();
    }

    private SharedPreferences getPrefFile() {
        return context.getSharedPreferences(
                PREF_FILE_KEY, Context.MODE_PRIVATE);
    }
}
