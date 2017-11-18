package ru.palestra.wifichat.services;

import android.content.Context;
import android.content.SharedPreferences;

import ru.palestra.wifichat.data.models.viewmodels.Client;

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

//    public void saveWasConnectedClient(Client client) {
//        //Проверим есть ли такой клиент в нашей "Базе"
//        List<Client> savedClients = getAllWasConnectedClient();
//
//        Client[] savedClientsArray = new Client[savedClients.size()];
//        savedClientsArray = savedClients.toArray(savedClientsArray);
//
//        for (Client savedClient : savedClientsArray) {
//            if (savedClient.getClientNearbyKey().equals(client.getClientNearbyKey())) {
//                return;
//            } else if (savedClient.getClientName().equals(client.getClientName())) {
//                //Если имена совпадают, а точки различаются, то обновляем точку
//                removeWasConnectedClient(savedClient);
//                break;
//            }
//        }
//
//        saveWasConnectedClients(Collections.singletonList(client));
//    }
//
//    private void saveWasConnectedClients(List<Client> clients) {
//        HashMap<String, String> clientsMap = new HashMap<>();
//        for (Client client : clients) {
//            clientsMap.put(client.getClientNearbyKey(), client.getClientName());
//        }
//
//        saveWasConnectedClientsPref(clientsMap);
//    }
//
//    private void saveWasConnectedClientsPref(Map<String, String> clients) {
//        Gson gson = new Gson();
//        String hashMapString = gson.toJson(clients);
//
//        getPrefFile().edit()
//                .putString(PREF_KEY_WAS_CONNECTED_CLIENTS, hashMapString)
//                .apply();
//    }
//
//    public void removeWasConnectedClient(Client removedClient) {
//        List<Client> potentialClients = getAllWasConnectedClient();
//
//        if (removedClient.getClientName() != null) {
//            potentialClients.remove(removedClient);
//        } else {
//            Client[] potentialClientsArray = new Client[potentialClients.size()];
//            potentialClientsArray = potentialClients.toArray(potentialClientsArray);
//
//            for (Client client : potentialClientsArray) {
//                if (client.getClientNearbyKey().equals(removedClient.getClientNearbyKey())) {
//                    potentialClients.remove(client);
//                }
//            }
//        }
//
//        saveWasConnectedClients(potentialClients);
//    }
//
//    public List<Client> getAllWasConnectedClient() {
//        List<Client> potentialClients = new ArrayList<>();
//
//        String storedHashMapString = getPrefFile().getString(PREF_KEY_WAS_CONNECTED_CLIENTS, null);
//        if (storedHashMapString == null) return Collections.emptyList();
//
//        Gson gson = new Gson();
//        Type type = new TypeToken<HashMap<String, String>>() {
//        }.getType();
//        HashMap<String, String> testHashMap2 = gson.fromJson(storedHashMapString, type);
//
//        Iterator it = testHashMap2.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry client = (Map.Entry) it.next();
//            potentialClients.add(
//                    Client.otherDevice(client.getValue().toString(), client.getKey().toString(), null));
//        }
//
//        return potentialClients;
//    }

    public void saveInfoAboutMyDevice(Client myDevice) {
        getPrefFile().edit()
                .putString(PREF_KEY_NAME_MY_DEVICE, myDevice.getName())
                .putString(PREF_KEY_UUID_MY_DEVICE, myDevice.getUUID())
                .apply();
    }

    public Client getInfoAboutMyDevice() {
        String name = getPrefFile().getString(PREF_KEY_NAME_MY_DEVICE, null);
        String UUID = getPrefFile().getString(PREF_KEY_UUID_MY_DEVICE, null);

        return name != null && UUID != null ?
                Client.myDevice(name, UUID) : Client.empty();
    }

    private SharedPreferences getPrefFile() {
        return context.getSharedPreferences(
                PREF_FILE_KEY, Context.MODE_PRIVATE);
    }
}
