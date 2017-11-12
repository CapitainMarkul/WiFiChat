package ru.palestra.wifichat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.palestra.wifichat.model.DeviceInfo;
import ru.palestra.wifichat.model.Message;
import ru.palestra.wifichat.services.SharedPrefServiceImpl;

/**
 * Created by Dmitry on 12.11.2017.
 */

public class MainPresenter {
    private final static String TAG = MainPresenter.class.getSimpleName();
    private DeviceInfo myDevice;

    private String targetId;
    private String targetName;

    private GoogleApiClient googleApiClient;

    private List<Message> lostMessages = new ArrayList<>(); //Недоставленные сообщения
    private List<Message> deliveredLostMessages = new ArrayList<>(); //Доставленные "Недоставленные" сообщения

    private Set<DeviceInfo> potentialClients = new HashSet<>();
    private Set<DeviceInfo> connectedClients = new HashSet<>();

    public static final String SERVICE_ID = "palestra.wifichat";
    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private Map<Message, Set<DeviceInfo>> broadCastBanList = new HashMap<>();
    private boolean sendingLostMessageComplete = true;

    private SharedPrefServiceImpl sharedPrefService;
    private MainActivity mainActivity;

    public MainPresenter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        sharedPrefService = new SharedPrefServiceImpl(mainActivity);
        myDevice = sharedPrefService.getInfoAboutMyDevice();
    }

    public void setDefaultOptions() {
//        Nearby.Connections.stopAllEndpoints(googleApiClient);
//        googleApiClient.reconnect();
        targetId = null;
        targetName = null;
//        connectedClients.clear();
//        potentialClients.clear();
//        mainActivity.updatePotentialClient(connectedClients);
    }

    /**
     * SendMessage
     */
    public void sendMessage(String textMessage) {
        if (!connectedClients.isEmpty()) {
            /** DEBUG */
            if (targetId != null && !targetId.isEmpty() &&
                    targetName != null && !targetName.isEmpty()) {
                mainActivity.debugLog(String.format("Отправка: %s - %s", targetName, targetId));
            } else {
                mainActivity.debugLog(String.format("Отправка всем", targetName, targetId));
            }

            try {
                sendBroadcastMessage(
                        Message.newMessage(myDevice.getClientName(), targetId, targetName, textMessage));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mainActivity.debugLog("Вообще некому отправить :(, но я сохраню");

            //Если хотели отправить сообщение кому-то, но передать сейчас нет возможности,
            //охраняем сообщение у сеья и транслируем всем кого найдем в будущем
            Message lostMessage = Message.newMessage(myDevice.getClientName(), targetId, targetName, textMessage);
            lostMessages.add(lostMessage);
            mainActivity.showMyMessage(lostMessage);
        }
    }

    public void foundNewEndPoint(DeviceInfo deviceInfo) {
        boolean isNewDevice = true;
        for (DeviceInfo device : connectedClients) {
            if (device.getClientName().equals(deviceInfo.getClientName())) {
                if (!device.getClientNearbyKey().equals(device.getClientNearbyKey())) {
                    isNewDevice = false;

                    //что-то произошло с прошлым устройством
                    disconnectedDevice(device.getClientNearbyKey());
                    break;
                } else {
                    isNewDevice = false;
                    break;
                }
            }
        }

        if (isNewDevice) {
            potentialClients.add(deviceInfo);
            mainActivity.updatePotentialClient(deviceInfo);

            //Auto Accept Connection
            //(Раскомментировать) Сам инициирует со всеми Коннект
//            mainActivity.stopDiscovery();
//            mainActivity.requestConnection(deviceInfo, null);

            //Проверим, этому устройству предназначались какие-либо сообщения?
            foundNewDevice(deviceInfo);
        }
    }

    // TODO: 12.11.2017 Упростить
    public void tryConnectDevices() {
        if (connectedClients.isEmpty()) {
            for (DeviceInfo potentialDevice : potentialClients) {
                try {
                    Thread.sleep(200);

                    mainActivity.requestConnection(potentialDevice, null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            for (DeviceInfo connectedDevice : connectedClients) {
                for (DeviceInfo potentialDevice : potentialClients) {
                    if (connectedDevice.getClientName().equals(potentialDevice.getClientName())) {
                        //Пропускаем
                        // TODO: 12.11.2017 Удалять из otentialClients
                        continue;
                    }

                    try {
                        Thread.sleep(200);

                        mainActivity.requestConnection(potentialDevice, null);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void removePotentialClient(DeviceInfo client) {
        potentialClients.remove(client);
    }

    public void addConnectedClient(DeviceInfo client) {
        connectedClients.add(client);
    }

    public void lostEndPoint(String endPointId) {
        //Потерявшиеся клиенты
        DeviceInfo lostDevices = searchLostedDevice(endPointId);
        if (lostDevices.getState() != DeviceInfo.State.EMPTY) {
            potentialClients.remove(lostDevices);
            mainActivity.removePotentialClient(lostDevices);
        }
    }

    public void messageDelivered(Message message) {
        deliveredLostMessages.add(message);
        lostMessages.remove(message);
    }

    public void failDelivered(Message message) {
        try {
            sendBroadcastMessage(message);
            mainActivity.debugLog("Send Lost Message on all");

            broadCastBanList.put(message, connectedClients); //Сохраняем всех подключенных клиентов, кому пытались отправить сообщение
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBroadcastMessage(Message message) throws IOException {
        mainActivity.debugLog("Send broadcast message");

        Nearby.Connections.sendPayload(
                googleApiClient,
                createClientsEndPoints(connectedClients, message.getFrom()),
                Payload.fromBytes(
                        MessageConverter.toBytes(message)))
                .setResultCallback(status -> {
                    if (status.isSuccess()) {
                        mainActivity.debugLog("Send OK!!");

                        if (message.getFrom().equals(myDevice.getClientName())) {
                            mainActivity.showMyMessage(message);
                        }
                    } else {
                        mainActivity.debugLog("Send FAIL!!" + status.getStatus());

                        //Если мы не можем найти пользователя с указанным Id,
                        //посылаем сообщение через рассылку
                        if (status.getStatus().equals("STATUS_ENDPOINT_UNKNOWN")) {
                            try {
//                                lostMessages.remove(message);   // TODO: 12.11.2017 Проверить, нужно или нет? Возможно это сообщение наоборот необходимо добавить в "потерянные"
                                sendBroadcastMessage(
                                        Message.broadcastMessage(message.getFrom(), message.getTargetName(), message.getText()));

                                disconnectedDevice(message.getTargetId());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    public void receivedRequest(String endPointId, Payload payload) {
        Message receivedMessage;
        try {
            receivedMessage = MessageConverter.getMessage(payload.asBytes());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        //Проверяем это Новое сообщение, или ответ о доставленном сообщении
        if (receivedMessage.getState() == Message.State.DELIVERED_MESSAGE) {
            lostMessages.remove(receivedMessage.getDeliveredMessage());
            deliveredLostMessages.add(receivedMessage.getDeliveredMessage());
        } else {
            //Работаем с обычным типом сообщения
            //Определим, это совершенно новое сообщение, или это сообщение мы уже кому-то доставили
            for (Message message : deliveredLostMessages) {
                if (deliveredLostMessages.contains(receivedMessage)) {
                    //даем ответ отправителю, что такое сообщение уже отправлено, и следует прекратить транслировать его
                    try {
                        deliverTargetMessage(
                                Message.deliveredMessage(endPointId, message));
                        return; //Нам не имеет смысла обрабатывать это сообщение, мы его уже доставили
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }

            String targetId = receivedMessage.getTargetId();
            String targetName = receivedMessage.getTargetName();

            //Иначе это Broadcast
            if (targetId != null && targetId.equals(myDevice.getClientNearbyKey())
                    || (targetName != null && targetName.equals(myDevice.getClientName()))) {
//                if (targetId.equals(myDevice.getClientNearbyKey()) ||
//                        targetName.equals(myDevice.getClientName())) {
                //Если у нас сменился Id, то сообщение доставить нам можно только по нашему имени,
                //если это произошло, то отправляем сообщение, что не нужно нас искать

                //Если сообщение нам
                mainActivity.showMessageForMe(receivedMessage);

                //Сообщаем, что получили сообщение
                try {
                    deliverTargetMessage(
                            Message.deliveredMessage(endPointId, receivedMessage));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (targetId == null && targetName == null) {
                //Если target == null, значит это Broadcast
                mainActivity.showBroadcastMessage(receivedMessage);
            } else {
                if (!lostMessages.contains(receivedMessage)) {
                    lostMessages.add(receivedMessage);
                }
            }
        }
    }

    public void connectedNewDevice(DeviceInfo device) {
        connectedClients.add(device);
    }

    public void disconnectedDevice(String endPointId) {
//        setDefaultOptions();

        DeviceInfo disconnectedDevice = searchDisconnectedDevice(endPointId);
        if (disconnectedDevice.getState() != DeviceInfo.State.EMPTY) {
            //Если отключился другой клиент, то не нужно очищать цель
            if (targetId != null && targetName != null) {
                if (targetId.equals(endPointId) || targetName.equals(disconnectedDevice.getClientName())) {
                    targetId = null;
                    targetName = null;
                }
            }

            connectedClients.remove(disconnectedDevice);
            mainActivity.updatePotentialClient(connectedClients);

            Nearby.Connections.disconnectFromEndpoint(googleApiClient, endPointId);
        }
    }

    public DeviceInfo searchDisconnectedDevice(String endPointId) {
        DeviceInfo[] devicesInfo = new DeviceInfo[connectedClients.size()];
        devicesInfo = connectedClients.toArray(devicesInfo);

        for (int i = 0; i < devicesInfo.length; i++) {
            if (devicesInfo[i].getClientNearbyKey().equals(endPointId)) {
                return devicesInfo[i];
            }
        }
        return DeviceInfo.empty();
    }

    public DeviceInfo searchLostedDevice(String endPointId) {
        DeviceInfo[] devicesInfo = new DeviceInfo[potentialClients.size()];
        devicesInfo = potentialClients.toArray(devicesInfo);

        for (int i = 0; i < devicesInfo.length; i++) {
            if (devicesInfo[i].getClientNearbyKey().equals(endPointId)) {
                return devicesInfo[i];
            }
        }
        return DeviceInfo.empty();
    }

    public DeviceInfo checkNewClient(String endPoint, ConnectionInfo connectionInfo) {
        DeviceInfo tempClient = DeviceInfo.otherDevice(connectionInfo.getEndpointName(), endPoint, null);
        if (!connectedClients.contains(tempClient)) {
            connectedClients.add(tempClient);
            return tempClient;
        }
        return DeviceInfo.empty();
    }

    public void updateTargetDevice(String targetId, String targetName) {
        this.targetId = targetId;
        this.targetName = targetName;
    }

    public String createFooterText() {
        String textClients = "";
        if (!connectedClients.isEmpty()) {
            for (DeviceInfo client : connectedClients) {
                textClients += client.getClientName() + ", ";
            }
            return textClients;
        } else {
            return "PEEK";
        }
    }

    public void foundNewDevice(DeviceInfo deviceInfo) {
        // После нахождения нового устройства проверяем, ему ли предназначалось сообщение
        try {
            for (Message message : lostMessages) {
                //Мы только что нашли того, кого искали. Немедленно коннектимся к нему
                if (message.getTargetId().equals(deviceInfo.getClientNearbyKey()) ||
                        message.getTargetName().equals(deviceInfo.getClientName())) {
                    //Можно попробовать, остановить discover, становить соединение, передать сообщение
                    // и снова начать discover
                    mainActivity.requestConnection(deviceInfo, message);
                }
            }
            sendLostMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendLostMessage() throws IOException {
        mainActivity.debugLog(String.format("Timer! Lost: %s, Deliver: %s", lostMessages.size(), deliveredLostMessages.size()));

        //Защита, если мы еще не успели всем передать сообщение, а уже сработало событие
        if (sendingLostMessageComplete) {   //Lock.class ??
            sendingLostMessageComplete = false;
            broadCastBanList.clear();

            if (!lostMessages.isEmpty()) {
//                Intent intent = new Intent(getApplicationContext(), SendLostMessage.class);
//                intent.putExtra("lostMessages", Parcels.wrap(lostMessages));
//                startService(intent);

                Message[] messages = new Message[lostMessages.size()];
                messages = lostMessages.toArray(messages);

                for (Message message : messages) {
                    for (DeviceInfo client : connectedClients) {
                        //Проверяем, пытались ли мы уже отправлять сообщение данному клиенту
                        if (broadCastBanList.get(message) == null ||
                                !broadCastBanList.get(message).contains(client)) {
                            //Проверяем, это тот клиент который нам нужен
                            if ((message.getTargetId() != null && message.getTargetId().equals(client.getClientNearbyKey())) ||
                                    (message.getTargetName()!=null && message.getTargetName().equals(client.getClientName()))) {

                                //Доставили получателю
                                deliverTargetMessage(message);
                                break;
                            }
                        }
                    }
                }
            }

            sendingLostMessageComplete = true;
        }
    }

    public void deliverTargetMessage(Message message) throws IOException {
        mainActivity.debugLog("Send target: " + message.getTargetId());

        Nearby.Connections.sendPayload(
                googleApiClient,
                message.getTargetId(),
                Payload.fromBytes(
                        MessageConverter.toBytes(message)))
                .setResultCallback(status -> {
                    if (status.isSuccess()) {
                        mainActivity.debugLog("Send OK!!");

                        messageDelivered(message);
                    } else {
                        mainActivity.debugLog("Send FAIL!!" + status.getStatus());

                        //Получатель не найден или не удалось доставить
                        failDelivered(message);
                    }
                });
    }

    private List<String> createClientsEndPoints(Set<DeviceInfo> clients, String fromName) {
        List<String> allEndPoints = new ArrayList<>();
        for (DeviceInfo endPoint : clients) {
            //Сообщение не нужно передавать назад отправителю
            if (!endPoint.getClientName().equals(fromName)) {
                allEndPoints.add(endPoint.getClientNearbyKey());
            }
        }
        return allEndPoints;
    }

    public void initGoogleClient(GoogleApiClient.Builder builder) {
        googleApiClient = builder.build();
    }

    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    public String getDeviceName() {
        return myDevice.getClientName();
    }

    public String getServiceId() {
        return SERVICE_ID;
    }
}
