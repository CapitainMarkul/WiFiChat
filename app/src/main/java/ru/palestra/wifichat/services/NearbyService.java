package ru.palestra.wifichat.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import androidx.annotation.Nullable;

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

import org.parceler.Parcels;

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
import ru.palestra.wifichat.data.models.mappers.ClientMapper;
import ru.palestra.wifichat.data.models.mappers.MessageMapper;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.data.models.viewmodels.Message;
import ru.palestra.wifichat.domain.db.DbClient;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.CreateUiListUtil;
import ru.palestra.wifichat.utils.Logger;
import ru.palestra.wifichat.utils.MessageConverter;
import ru.palestra.wifichat.utils.ValidMessageUtil;

/**
 * Created by da.pavlov1 on 09.11.2017.
 */

public class NearbyService extends Service {
    private static final String TAG = NearbyService.class.getSimpleName() + "_SERVICE";

    private static final int TIME_DISCOVERY = 3;
    private static final int TIME_RESTART_ADVERTISING = 30;
    private static final int TIME_WAIT_DISCOVERY = 8;
    private static final int TIME_RESEND_MSG = 5;


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

    private List<Client> requestedClients = new ArrayList<>();  // те, кому мы кинули запрос
    private List<Client> connectedClients = new ArrayList<>();
    private Set<Client> potentialClients = new HashSet<>(); //Постоянно находит одинаковые точки

    private List<Message> lostMessages = new ArrayList<>(); //Недоставленные сообщения
    private List<Message> deliveredLostMessages = new ArrayList<>(); //Доставленные "Недоставленные" сообщения

    private Map<Message, Set<String>> banListSendMessage = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.debugLog("Start: NearbyService & ConnectToClientsService");

        myDevice = App.sharedPreference().getInfoAboutMyDevice();
        dbClient = App.dbClient();

        CreateUiListUtil.init(
                dbClient.getAllWasConnectedClients());

        runConnectionThread();
        runSendMessageThread();
    }

    @Override
    public void onDestroy() {
        stopConnectionTimer();
        stopSendMessageTimer();

        stopAdvertising();
        stopDiscovery();
        Nearby.Connections.stopAllEndpoints(App.googleApiClient());

        setDefaultValue();
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

            //предотвращаем дубликаты сообщений
            if (!lostMessages.contains(msgFromMe)) {
                sendTargetMessage(msgFromMe, msgFromMe.getTargetId());

                dbClient.saveSentMsg(
                        MessageMapper.toMessageDb(msgFromMe));
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * startAdvertising()
     * Запуск намерения стать точкой доступа
     */
    private synchronized void startAdvertising() {
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
    private synchronized void startDiscovery() {
        if (!isConnected()) return;
        Logger.debugLog("Start discovery");
        isDiscovering = true;

        sendBroadcast(new Intent(ConfigIntent.ACTION_DISCOVERY)
                .putExtra(ConfigIntent.STATUS_DISCOVERY, true));

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
    private synchronized void stopDiscovery() {
        if (!isConnected()) return;
        Logger.debugLog("Stop discovery");
        isDiscovering = false;

        sendBroadcast(new Intent(ConfigIntent.ACTION_DISCOVERY)
                .putExtra(ConfigIntent.STATUS_DISCOVERY, false));

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
        }

        @Override
        public void onEndpointLost(String idEndPoint) {
            Logger.debugLog("Lost endpoint: " + idEndPoint);

            removePotentialClient(
                    Client.otherDevice(null, idEndPoint, null));
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
            if (requestedClients.contains(potentialClient)) continue;

            for (Client client : connectedClients) {
                if (client.getNearbyKey().equals(potentialClient.getNearbyKey())) {
                    return;
                }
            }

            Logger.errorLog(String.format("Run request: %s : %s", potentialClient.getName(), potentialClient.getNearbyKey()));

            requestedClients.add(potentialClient);
            requestConnection(potentialClient);
        }
    }

    private synchronized void requestConnection(Client potentialClient) {
        Nearby.Connections.requestConnection(
                App.googleApiClient(),
                myDevice.getName(),
                potentialClient.getNearbyKey(),
                connectionLifecycleCallback
        ).setResultCallback(status -> {
            if (status.isSuccess()) {
//                removePotentialClient(potentialClient);
                requestedClients.remove(potentialClient);
            } else {
                Logger.errorLog(String.format("Error request connection: %s : %s", status.getStatus(), potentialClient.getNearbyKey()));
                //Мертвые точки
                if (status.getStatus().toString().contains("STATUS_ENDPOINT_UNKNOWN")) {
                    removePotentialClient(potentialClient);
                } else if (status.getStatus().toString().contains("STATUS_ALREADY_CONNECTED_TO_ENDPOINT")) {
                    removePotentialClient(potentialClient);
                } else if (status.getStatus().toString().contains("STATUS_BLUETOOTH_ERROR")) {
                    restartBluetooth();
                } else if (status.getStatus().toString().contains("STATUS_ENDPOINT_IO_ERROR")) {
                    Nearby.Connections.disconnectFromEndpoint(App.googleApiClient(), potentialClient.getNearbyKey());
                }

                requestedClients.remove(potentialClient);
            }
        });
    }

    private ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endPoint, ConnectionInfo connectionInfo) {
            Logger.debugLog("onConnectionInitiated: START!");

            // FIXME: 14.11.2017
            // Можно добавить задержку (Проверить)
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            acceptConnection(connectionInfo.getEndpointName(), endPoint);
        }

        @Override
        public void onConnectionResult(String idEndPoint, ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Logger.debugLog("Connect: OK");
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
            Logger.errorLog("Disconnect endpoint: " + endPoint);

            Nearby.Connections.disconnectFromEndpoint(App.googleApiClient(), endPoint);
            removeConnectedClient(endPoint);
        }
    };

    private void removeConnectedClient(String idEndPoint) {
        Client[] connectedClientsArray = new Client[connectedClients.size()];
        connectedClientsArray = connectedClients.toArray(connectedClientsArray);

        for (Client disconnectedClient : connectedClientsArray) {
            if (disconnectedClient.getNearbyKey().equals(idEndPoint)) {
                connectedClients.remove(disconnectedClient);

                disconnectedClient.setOnline(false);
                sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
                        .putExtra(ConfigIntent.UPDATED_CLIENTS, Parcels.wrap(CreateUiListUtil.updateUiClientsList(disconnectedClient))));

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
                Logger.errorLog(String.format("Send Ping to: %s:%s", idEndPoint, idEndPoint));
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
        switch (message.getState()) {
            case PING_PONG_MESSAGE:
                isPingPongMsg(message, idEndPoint);
                break;
            case DELIVERED_MESSAGE:
                isDeliveredMessage(message, idEndPoint);
                break;
            case FOR_ME_MESSAGE:
                isMsgForMe(message, idEndPoint);
                break;
            default:
                if (!lostMessages.contains(message)) {
                    lostMessages.add(message);
                }
                break;
        }
    }

    private void isPingPongMsg(Message message, String idEndPoint) {
        if (message.getState() != Message.State.PING_PONG_MESSAGE) return;
        Logger.errorLog(String.format("Ping from: %s:%s", message.getFromName(), idEndPoint));

        Client connectedClient =
                Client.otherDevice(message.getFromName(), idEndPoint, message.getFromUUID());

        //Соединение установленно
        connectedClients.add(connectedClient);
        removePotentialClient(connectedClient);

        connectedClient
                .setNearbyKey(idEndPoint)
                .setOnline(true);

        sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
                .putExtra(ConfigIntent.UPDATED_CLIENTS, Parcels.wrap(CreateUiListUtil.updateUiClientsList(connectedClient))));

        dbClient.saveConnectedClient(
                ClientMapper.toClientDb(connectedClient));
    }

    private void isDeliveredMessage(Message message, String idEndPoint) {
        if (message.getState() != Message.State.DELIVERED_MESSAGE) return;

        Message deliveredMessage = message.getDeliveredMsg();

        if (lostMessages.contains(deliveredMessage))
            lostMessages.remove(deliveredMessage);
        if (!deliveredLostMessages.contains(message))
            deliveredLostMessages.add(message);

        putClientInBanList(message, idEndPoint);
        dbClient.updateMsgStatus(deliveredMessage);

        if (deliveredMessage.getFromUUID().equals(myDevice.getUUID())) {
            //Обновляем UI (Посылаем сообщение со статусом доставлено)
            deliveredMessage.setDelivered(true);
            updateMessageAdapter(deliveredMessage);
        }
    }

    private void isMsgForMe(Message message, String idEndPoint) {
        String targetUUID = message.getTargetUUID();
        if (targetUUID != null && !targetUUID.equals(myDevice.getUUID())) return;

        Message deliveredMessage =
                Message.deliveredMessage(myDevice.getName(), myDevice.getUUID(), message);

        //Для увеличения скорости работы (визуальной), ответ отправителю даем незамедлительно
        sendTargetMessage(deliveredMessage, idEndPoint);
        putClientInBanList(deliveredMessage, idEndPoint);
        deliveredLostMessages.add(deliveredMessage);

        updateMessageAdapter(message);
        dbClient.saveSentMsg(
                MessageMapper.toMessageDb(message));
    }

    private void sendLostMessage() {
        //Защита, если мы еще не успели всем передать сообщение, а уже сработало событие
        if (sendingLostMessageComplete) {   //Lock.class ??
            sendingLostMessageComplete = false;

            for (Message message : ValidMessageUtil.obtainValidMessage(lostMessages)) {
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

    private synchronized void sendDeliveredMessage() {
        //Защита, если мы еще не успели всем передать сообщение, а уже сработало событие
        if (sendingDeliveredMessageComplete) {   //Lock.class ??
            sendingDeliveredMessageComplete = false;

            for (Message message : ValidMessageUtil.obtainValidMessage(deliveredLostMessages)) {
                for (Client client : connectedClients) {
                    //Проверяем, пытались ли мы уже отправлять сообщение данному клиенту
                    if (banListSendMessage.get(message) == null ||
                            !banListSendMessage.get(message).contains(client.getNearbyKey())) {
                        //Отправляем всем подключенным клиентам
                        sendTargetMessage(message, client.getNearbyKey());
                    }
                }
            }

            sendingDeliveredMessageComplete = true;
        }
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
                        Logger.debugLog(String.format("Message text: %s | Send target: %s | IsPing: %s | IsDelivered: %s", message.getText(), idEndPoint, message.isPingPongTypeMsg(), message.isDelivered()));

                        //На "Понг" сообщения нам должен ответить САМ получатель, иначе считаем, что контакт не был установлен
                        if (message.getState() == Message.State.PING_PONG_MESSAGE) return;

                        if (message.getTargetId() != null && message.getTargetId().equals(idEndPoint)) {
                            // TODO: 14.11.2017 Update Ui, что сообщение доставлено (пометочку сделать)
                            message.setDelivered(true);
                            updateMessageAdapter(message);
                        }

                        putClientInBanList(message, idEndPoint);
                    } else {
                        Logger.errorLog("Send FAIL!!" + status.getStatus());

                        //На "Понг" сообщения нам должен ответить САМ получатель, иначе считаем, что контакт не был установлен
                        if (message.getState() == Message.State.PING_PONG_MESSAGE) return;

                        //Мертвые точки
                        if (status.getStatus().toString().contains("STATUS_ENDPOINT_UNKNOWN")) {
                            // TODO: 14.11.2017 Сообщение не доставлено, Update Ui
                            //Если точка изменилась, делаем из сообщения Бродкаст
                            lostMessages.add(
                                    Message.broadcastMessage(message.getFromName(), message.getFromUUID(), message.getTargetUUID(), message.getText(), message.getMsgUUID()));

//                            sendLostMessage();
                        } else {
                            lostMessages.add(message);
                        }
                    }
                });
    }

    private void updateMessageAdapter(Message message) {
        sendBroadcast(
                new Intent(ConfigIntent.ACTION_DELIVERED_MESSAGE)
                        .putExtra(ConfigIntent.MESSAGE, message));
    }

    private void updatePotentialClients(Client newPotentialClient) {
        Client[] potentialClientsArray = new Client[potentialClients.size()];
        potentialClientsArray = potentialClients.toArray(potentialClientsArray);

        for (Client potentialClient : potentialClientsArray) {
            //Добавление нового клиента
            if (potentialClient.getName().equals(newPotentialClient.getName()) &&
                    !potentialClient.getNearbyKey().equals(newPotentialClient.getNearbyKey())) {
                //Нужно обновить запись о клиенте, у него сменился idEndPoint
                potentialClients.remove(potentialClient);
                potentialClients.add(newPotentialClient);

                return;
            }
        }

        for (Client connectedClient : connectedClients) {
            //Добавление нового клиента
            if (connectedClient.getName().equals(newPotentialClient.getName())) {
//                    connectedClient.getNearbyKey().equals(newPotentialClient.getNearbyKey())) {
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

    private synchronized void restartBluetooth() {
        stopAdvertising();
        stopDiscovery();

        startAdvertising();
        startDiscovery();
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

            //Попытка коннекта, отправка сообщений каждые 5 секунд
            timerSendMessage.schedule(
                    sendMessageSendMessageServiceTimer,
                    TimeUnit.SECONDS.toMillis(TIME_RESEND_MSG));
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
                    TimeUnit.SECONDS.toMillis(isDiscovering ? TIME_DISCOVERY : TIME_WAIT_DISCOVERY));
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
