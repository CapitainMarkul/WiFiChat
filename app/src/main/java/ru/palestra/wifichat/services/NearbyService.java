package ru.palestra.wifichat.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import ru.palestra.wifichat.App;
import ru.palestra.wifichat.MessageConverter;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.domain.db.DbClient;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.Logger;
import ru.palestra.wifichat.utils.TimeUtils;

/**
 * Created by da.pavlov1 on 09.11.2017.
 */

public class NearbyService extends Service {
    private static final String TAG = NearbyService.class.getSimpleName() + "_SERVICE";

    private static final int MSG_START_DISCOVERY = 1001;
    private static final int MSG_STOP_DISCOVERY = 1002;

    private static final int MSG_START_CONNECT_TO_CLIENTS = 1004;

    public static final String SERVICE_ID = "tensor.off_chat";
    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private DbClient dbClient;

    private Client myDevice;

    private Handler sendMessageServiceHandler;
    private Handler searchClientsServiceHandler;

    private Timer timerSendMessage;
    private Timer connectionTimer;

    private SendMessageServiceTimer sendMessageSendMessageServiceTimer;
    private ConnectionServiceTimer connectionServiceTimer;

    private boolean sendingLostMessageComplete = true;
    private boolean sendingDeliveredMessageComplete = true;

    private boolean isAdvertising;
    private boolean isDiscovering;

    private List<Client> connectedClients = new ArrayList<>();
    private Set<Client> potentialClients = new HashSet<>(); //Постоянно находит одинаковые точки

    private List<Message> lostMessages = new ArrayList<>(); //Недоставленные сообщения
    private List<Message> deliveredLostMessages = new ArrayList<>(); //Доставленные "Недоставленные" сообщения

    private Map<Message, Set<String>> banListSendMessage = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.debugLog("Start: NearbyService & ConnectToClientsService");

        dbClient = App.dbClient();
        myDevice = App.sharedPreference().getInfoAboutMyDevice();

        runConnectionThread();
        runSendMessageThread();
    }

    @Override
    public void onDestroy() {
        // TODO: 16.11.2017 Можно ничего не делать
        stopConnectionTimer();
        stopSendMessageTimer();

        setDefaultValue();

        stopAdvertising();
        stopDiscovery();

        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {
            Message msgFromMe =
                    (Message) intent.getSerializableExtra(ConfigIntent.MESSAGE);

            sendTargetMessage(msgFromMe, msgFromMe.getTargetId());
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * SEARCH NEW CLIENT PART
     * */

    /**
     * startAdvertising()
     * Запуск намерения стать точкой доступа
     */
    private void startAdvertising() {
        if (!isConnected()) return;
        Logger.debugLog("start Advertising");
        isAdvertising = true;

        Nearby.Connections.startAdvertising(
                App.googleApiClient(),
                myDevice.getName(),
                SERVICE_ID,
                connectionLifecycleCallback,
                new AdvertisingOptions(STRATEGY))
                .setResultCallback(statusAdvertising);
    }

    /**
     * stopAdvertising()
     * Прекращение намерения стать точкой доступа
     */
    private void stopAdvertising() {
        if (!isConnected()) return;
        Logger.debugLog("stop Advertising");
        Nearby.Connections.stopAdvertising(App.googleApiClient());
    }

    private ResultCallback<? super Connections.StartAdvertisingResult> statusAdvertising = result -> {
        if (result.getStatus().isSuccess()) {
            Logger.debugLog("startAdvertising: SUCCESS");
        } else {
            Logger.errorLog("startAdvertising: FAILURE " + result.getStatus());
        }
    };

    /**
     * startDiscovery()
     * Запуск поиска точек для соединения
     */
    private void startDiscovery() {
        if (!isConnected()) return;
        Logger.debugLog("Start discovery");
        isDiscovering = true;

        Nearby.Connections.startDiscovery(
                App.googleApiClient(),
                SERVICE_ID,
                endpointDiscoveryCallback,
                new DiscoveryOptions(STRATEGY))
                .setResultCallback(statusDiscovery);
    }

    /**
     * stopDiscovery()
     * Прекращение поиска точек для соединения
     */
    private void stopDiscovery() {
        if (!isConnected()) return;
        Logger.debugLog("Stop discovery");
        isDiscovering = false;

        Nearby.Connections.stopDiscovery(App.googleApiClient());
    }

    /**
     * EndpointDiscoveryCallback()
     * Оповещает о найденных точках доступа
     */
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(
                String idEndPoint, DiscoveredEndpointInfo discoveredEndpointInfo) {
            Logger.errorLog("Found new endpoint: " + idEndPoint);

            updatePotentialClients(
                    Client.otherDevice(discoveredEndpointInfo.getEndpointName(), idEndPoint, null));

//            sendBroadcast(new Intent(ConfigIntent.ACTION_SEARCH_CLIENT)
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_ID, idEndPoint)
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_NAME, discoveredEndpointInfo.getEndpointName())
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_IS_LOST, false));
        }

        @Override
        public void onEndpointLost(String idEndPoint) {
            Logger.debugLog("Lost endpoint: " + idEndPoint);

            removePotentialClient(
                    Client.otherDevice(null, idEndPoint, null));

//            sendBroadcast(new Intent(ConfigIntent.ACTION_SEARCH_CLIENT)
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_ID, idEndPoint)
//                    .putExtra(ConfigIntent.DISCOVERY_TARGET_IS_LOST, true));
        }
    };

    private ResultCallback<? super Status> statusDiscovery = (ResultCallback<Status>) status -> {
        if (status.isSuccess()) {
            Logger.debugLog("startDiscovery: SUCCESS");
        } else {
            Logger.errorLog("startDiscovery: FAILURE" + status.getStatus());

            if (status.getStatus().toString().contains("STATUS_ALREADY_DISCOVERING")) {
                isDiscovering = true;
            }
        }
    };

    /**
     * SEND MESSAGE PART
     */
    private void runRequestedConnection() {
        Client[] potentialClientsArray = new Client[potentialClients.size()];
        potentialClientsArray = potentialClients.toArray(potentialClientsArray);

        for (Client potentialClient : potentialClientsArray) {
            requestConnection(potentialClient);
            // FIXME: 14.11.2017
            // Можно добавить задержку (Проверить)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestConnection(Client potentialClient) {
        Nearby.Connections.requestConnection(
                App.googleApiClient(),
                myDevice.getName(),
                potentialClient.getNearbyKey(),
                connectionLifecycleCallback
        ).setResultCallback(status -> {
            if (status.isSuccess()) {
//                connectedClients.add(potentialClient);
//                updatePotentialClients(potentialClient);
            } else {
                //Мертвые точки
                if (status.getStatus().toString().contains("STATUS_ENDPOINT_UNKNOWN")) {
                    removePotentialClient(potentialClient);
                    // TODO: 14.11.2017 Update UI
                }
            }
        });
    }

    private ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endPoint, ConnectionInfo connectionInfo) {
            Logger.debugLog("onConnectionInitiated: START!");

            acceptConnection(connectionInfo.getEndpointName(), endPoint);
        }

        @Override
        public void onConnectionResult(String idEndPoint, ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Logger.debugLog("Connect: OK");

                    // TODO: 18.11.2017 После установки соединения, кидаем пинг-сообщение с нашим UUID
//                    Logger.errorLog(String.format("Ping to: %s:%s", nameEndPoint, idEndPoint));
                    Logger.errorLog(String.format("Ping to: %s:%s", idEndPoint, idEndPoint));
                    sendTargetMessage(
                            Message.pingPongMessage(myDevice.getName(), myDevice.getUUID(), idEndPoint), idEndPoint);

                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Logger.errorLog("Connect: FAIL" + result.getStatus());
                    break;
            }
        }

        @Override
        public void onDisconnected(String endPoint) {
            removeConnectedClient(endPoint);
        }
    };

    private void removeConnectedClient(String idEndPoint) {
        Client[] connectedClientsArray = new Client[connectedClients.size()];
        connectedClientsArray = connectedClients.toArray(connectedClientsArray);

        for (Client connectedClient : connectedClientsArray) {
            if (connectedClient.getNearbyKey().equals(idEndPoint)) {
                connectedClients.remove(connectedClient);

                sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
                        .putExtra(ConfigIntent.CONNECTION_TARGET_ID, idEndPoint)
                        .putExtra(ConfigIntent.CONNECTION_FOOTER_TEXT, createFooterText())
                        .putExtra(ConfigIntent.CONNECTION_TARGET_IS_DISCONNECT, true));

                break;
            }
        }
    }

    private void acceptConnection(String nameEndPoint, String idEndPoint) {
        Nearby.Connections.acceptConnection(
                App.googleApiClient(),
                idEndPoint,
                messageListener
        ).setResultCallback(status -> {
            if (status.isSuccess()) {

                 Logger.errorLog(String.format("Connected to: %s:%s", nameEndPoint, idEndPoint));

//                // TODO: 18.11.2017 После установки соединения, кидаем пинг-сообщение с нашим UUID
//                Logger.errorLog(String.format("Ping to: %s:%s", nameEndPoint, idEndPoint));
//                sendTargetMessage(
//                        Message.pingPongMessage(myDevice.getName(), myDevice.getUUID(), idEndPoint), idEndPoint);
            } else {
                //Соединение не удалось
                // TODO: 14.11.2017 Действовать в соответствии с кодом ошибки
            }
        });
    }

    private PayloadCallback messageListener = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String idEndPoint, Payload payload) {
            Message receivedMessage = MessageConverter.getMessage(payload.asBytes());
            if (receivedMessage.getState() == Message.State.EMPTY_MESSAGE) return;

            responseFromClient(receivedMessage, idEndPoint);
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private void responseFromClient(Message message, String idEndPoint) {
        if (isPingPongMsg(message, idEndPoint)) ;
        else if (isDeliveredMessage(message, idEndPoint)) ;
        else if (isMsgForMe(message)) ;
        else if (isBroadcastMSG(message)) ;
        else {
            if (!lostMessages.contains(message)) {
                lostMessages.add(message);
            }
        }
    }


    private boolean isPingPongMsg(Message message, String idEndPoint) {
        if (message.getState() != Message.State.PING_PONG_MESSAGE) return false;
        Logger.errorLog(String.format("Ping from: %s:%s", message.getFromName(), idEndPoint));

        //Даем ответ пользователю, что его "Пинг сообщение" успешно получено, и обработано
//        deliveredLostMessages.add(
//                Message.deliveredMessage(myDevice.getClientName(), myDevice.getUUID(), message));

        //Соединение установленно
                connectedClients.add(
                        Client.otherDevice(message.getFromName(), idEndPoint, message.getFromUUID()));
                removePotentialClient(
                        Client.otherDevice(message.getFromName(), idEndPoint, null));

                sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
                        .putExtra(ConfigIntent.CONNECTION_TARGET_ID, idEndPoint)
                        .putExtra(ConfigIntent.CONNECTION_TARGET_NAME, message.getFromName())
                        .putExtra(ConfigIntent.CONNECTION_TARGET_UUID, message.getFromUUID())
                        .putExtra(ConfigIntent.CONNECTION_FOOTER_TEXT, createFooterText())
                        .putExtra(ConfigIntent.CONNECTION_TARGET_IS_DISCONNECT, false));

        // TODO: 18.11.2017 Save Client in Db
        return true;
    }

    private boolean isDeliveredMessage(Message message, String idEndPoint) {
        if (message.getState() != Message.State.DELIVERED_MESSAGE) return false;

        Message deliveredMessage = message.getDeliveredMsg();

        if (lostMessages.contains(deliveredMessage))
            lostMessages.remove(deliveredMessage);
        if (!deliveredLostMessages.contains(message))
            deliveredLostMessages.add(message);

        putClientInBanList(message, idEndPoint);

        return true;
    }

    private boolean isMsgForMe(Message message) {
        String targetId = message.getTargetId();
        String targetUUID = message.getTargetUUID();     // FIXME: 17.11.2017 Здесь будет UUID
        if (targetUUID != null && !targetUUID.equals(myDevice.getUUID())) return false;

        //Если у нас сменился Id, то сообщение доставить нам можно только по нашему имени,
        //если это произошло, то отправляем сообщение, что не нужно нас искать

        //Если сообщение нам
        // TODO: 14.11.2017 Save Message, Update Ui
        showMessage(message);
        deliveredLostMessages.add(Message.deliveredMessage(myDevice.getName(), myDevice.getUUID(), message));

        return true;
    }

    private boolean isBroadcastMSG(Message message) {
        return message.getTargetId() == null && message.getTargetUUID() == null;
        // TODO: 14.11.2017 Save Message, Update Ui. Проверить, есть ли сейчас Broadcast без конечной цели (Вроде нет)
    }

    private void sendLostMessage() {
        //Защита, если мы еще не успели всем передать сообщение, а уже сработало событие
        if (sendingLostMessageComplete) {   //Lock.class ??
            sendingLostMessageComplete = false;

            for (Message message : createValidMessage(lostMessages)) {
                for (Client client : connectedClients) {
                    //Сообщение не нужно передавать назад отправителю
                    if (client.getUUID() != null
                            && client.getUUID().equals(message.getFromUUID())) continue;

                    //Проверяем, пытались ли мы уже отправлять сообщение данному клиенту
                    if (banListSendMessage.get(message) == null ||
                            !banListSendMessage.get(message).contains(client.getNearbyKey())) {
                        sendTargetMessage(message, client.getNearbyKey());
                    }
                }
            }

            sendingLostMessageComplete = true;
        }
    }

    private void sendDeliveredMessage() {
        //Защита, если мы еще не успели всем передать сообщение, а уже сработало событие
        if (sendingDeliveredMessageComplete) {   //Lock.class ??
            sendingDeliveredMessageComplete = false;

            for (Message message : createValidMessage(deliveredLostMessages)) {
                for (Client client : connectedClients) {
                    //Проверяем, пытались ли мы уже отправлять сообщение данному клиенту
                    if (banListSendMessage.get(message) == null ||
                            !banListSendMessage.get(message).contains(client.getNearbyKey())) {
                        //Отправляем тому, кто прислал нам это сообщение
                        sendTargetMessage(message, client.getNearbyKey());
                    }
                }
            }

            sendingDeliveredMessageComplete = true;
        }
    }

    private List<Message> createValidMessage(List<Message> messages) {
        List<Message> validMessages = new ArrayList<>();

        Message[] oldMessages = new Message[messages.size()];
        oldMessages = messages.toArray(oldMessages);

        for (Message message : oldMessages) {
            //Удаляются сообщения старше 2 минут
            if (ChronoUnit.MINUTES.between(TimeUtils.longToLocalDateTime(message.getTimeSend()), TimeUtils.timeNowLocalDateTime()) > 2) {
                messages.remove(message);
                continue;
            }

            validMessages.add(message);
        }
        return validMessages;
    }

    private void putClientInBanList(Message message, String idEndPoint) {
        if (banListSendMessage.get(message) == null) {
            banListSendMessage.put(message, new HashSet<>(Collections.singletonList(idEndPoint)));
        } else if (!banListSendMessage.get(message).contains(idEndPoint)) {
            banListSendMessage.get(message).add(idEndPoint);
        }
    }

    private void sendTargetMessage(Message message, String idEndPoint) {
        Nearby.Connections.sendPayload(
                App.googleApiClient(),
                idEndPoint,
                Payload.fromBytes(
                        MessageConverter.toBytes(message)))
                .setResultCallback(status -> {
                    if (status.isSuccess()) {
                        Logger.debugLog(String.format("Message: %s | Send target: %s", message.getText(), idEndPoint));

                        //На "Понг" сообщения нам должен ответить САМ получатель, иначе считаем, что контакт не был установлен
                        if (message.getState() == Message.State.PING_PONG_MESSAGE) return;

                        if(message.getTargetId() != null && message.getTargetId().equals(idEndPoint)){
                            // TODO: 14.11.2017 Update Ui, что сообщение доставлено (пометочку сделать)

                        }

                        putClientInBanList(message, idEndPoint);
                    } else {
                        Logger.debugLog("Send FAIL!!" + status.getStatus());

                        //На "Понг" сообщения нам должен ответить САМ получатель, иначе считаем, что контакт не был установлен
                        if (message.getState() == Message.State.PING_PONG_MESSAGE) return;

                        //Мертвые точки
                        if (status.getStatus().toString().contains("STATUS_ENDPOINT_UNKNOWN")) {
                            // TODO: 14.11.2017 Update Ui
                            //Если точка изменилась, делаем из сообщения Бродкаст
                            lostMessages.add(
                                    Message.broadcastMessage(message.getFromName(), message.getFromUUID(), message.getTargetUUID(), message.getText(), message.getMsgUUID()));
                        } else {
                            lostMessages.add(message);
                        }
                    }
                });
    }

    private void showMessage(Message message) {
        sendBroadcast(
                new Intent(ConfigIntent.ACTION_DELIVERED_MESSAGE)
                        .putExtra(ConfigIntent.MESSAGE, message));
    }

    private void updatePotentialClients(Client newPotentialClient) {
        Client[] potentialClientsArray = new Client[potentialClients.size()];
        potentialClientsArray = potentialClients.toArray(potentialClientsArray);

        for (Client client : potentialClientsArray) {
            //Добавление нового клиента
            if (client.getName().equals(newPotentialClient.getName()) &&
                    !client.getNearbyKey().equals(newPotentialClient.getNearbyKey())) {
                //Нужно обновить запись о клиенте, у него сменился idEndPoint
                potentialClients.remove(client);
                potentialClients.add(newPotentialClient);

                return;
            }
        }

        potentialClients.add(newPotentialClient);
    }

    private void removePotentialClient(Client removePotentialClient) {
        Client[] potentialClientsArray = new Client[potentialClients.size()];
        potentialClientsArray = potentialClients.toArray(potentialClientsArray);

        for (Client client : potentialClientsArray) {
            //Удаление клиента
            if (client.getNearbyKey().equals(removePotentialClient.getNearbyKey())) {
                potentialClients.remove(removePotentialClient);

                return;
            }
        }
    }

    public String createFooterText() {
        String textClients = "";
        if (!connectedClients.isEmpty()) {
            for (Client client : connectedClients) {
                textClients += client.getName() + ", ";
            }
            return textClients;
        } else {
            return "PEEK";
        }
    }

    private boolean isConnected() {
        if (!App.googleApiClient().isConnected()) {
            Logger.errorLog("Need run googleClient");
            setDefaultValue();
            return false;
        }
        return true;
    }

    private void setDefaultValue() {
        isAdvertising = false;
        isDiscovering = false;
        connectedClients.clear();
    }

    /**
     * Send Message Run Thread
     */

    private void runSendMessageThread() {
        HandlerThread threadMessage = new HandlerThread("SendMessage", Process.THREAD_PRIORITY_BACKGROUND);
        threadMessage.start();

        sendMessageServiceHandler = new SendMessageServiceHandler(threadMessage.getLooper());

        sendMessageServiceHandler.sendEmptyMessage(MSG_START_CONNECT_TO_CLIENTS);
    }

    private final class SendMessageServiceHandler extends Handler {

        SendMessageServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_START_CONNECT_TO_CLIENTS:
                    Logger.debugLog(String.format("Lost: %s, Deliver: %s", lostMessages.size(), deliveredLostMessages.size()));

                    runRequestedConnection();

                    sendLostMessage();
                    sendDeliveredMessage();
                    break;
                default:
                    break;
            }

            stopSendMessageTimer();
            initSendMessageTimer();
        }
    }

    private void initSendMessageTimer() {
        if (timerSendMessage == null) {
            timerSendMessage = new Timer();

            if (sendMessageSendMessageServiceTimer != null)
                sendMessageSendMessageServiceTimer.cancel();
            sendMessageSendMessageServiceTimer = new SendMessageServiceTimer();

            //Попытка коннекта каждые 10 секунд
            timerSendMessage.schedule(
                    sendMessageSendMessageServiceTimer,
                    TimeUnit.SECONDS.toMillis(10));
        }
    }

    private void stopSendMessageTimer() {
        if (timerSendMessage != null) {
            timerSendMessage.cancel();
            timerSendMessage = null;
        }
    }

    private final class SendMessageServiceTimer extends TimerTask {

        @Override
        public void run() {
            if (sendMessageServiceHandler != null) {
                Logger.debugLog("Timer sendLostMessageService");
                sendMessageServiceHandler.sendEmptyMessage(MSG_START_CONNECT_TO_CLIENTS);
            }
        }
    }

    /**
     * SearchClients Run Thread
     */

    private void runConnectionThread() {
        HandlerThread threadConnection = new HandlerThread("ConnectionClient", Process.THREAD_PRIORITY_BACKGROUND);
        threadConnection.start();

        searchClientsServiceHandler = new ConnectionServiceHandler(threadConnection.getLooper());

        searchClientsServiceHandler.sendEmptyMessage(MSG_START_DISCOVERY);
    }

    private final class ConnectionServiceHandler extends Handler {

        ConnectionServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            if (!isAdvertising)
                startAdvertising();

            switch (msg.what) {
                case MSG_START_DISCOVERY:
                    startDiscovery();
                    break;
                case MSG_STOP_DISCOVERY:
                    stopDiscovery();
                    break;
                default:
                    break;
            }

            stopConnectionTimer();
            initConnectionTimer();
        }
    }

    private void initConnectionTimer() {
        if (connectionTimer == null) {
            connectionTimer = new Timer();

            if (connectionServiceTimer != null) connectionServiceTimer.cancel();
            connectionServiceTimer = new ConnectionServiceTimer();

            connectionTimer.schedule(
                    connectionServiceTimer,
                    TimeUnit.SECONDS.toMillis(isDiscovering ? 3 : 10));
        }
    }

    private void stopConnectionTimer() {
        if (connectionTimer != null) {
            connectionTimer.cancel();
            connectionTimer = null;
        }
    }

    private final class ConnectionServiceTimer extends TimerTask {

        @Override
        public void run() {
            if (searchClientsServiceHandler != null) {
                Logger.debugLog("Timer connectToClientService");
                searchClientsServiceHandler.sendEmptyMessage(
                        isDiscovering ? MSG_STOP_DISCOVERY : MSG_START_DISCOVERY);
            }
        }
    }
}
