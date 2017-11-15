package ru.palestra.wifichat.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.Nullable;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.threeten.bp.Clock;
import org.threeten.bp.LocalDateTime;
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
import ru.palestra.wifichat.model.DeviceInfo;
import ru.palestra.wifichat.model.Message;
import ru.palestra.wifichat.utils.ConfigIntent;
import ru.palestra.wifichat.utils.Logger;

/**
 * Created by da.pavlov1 on 09.11.2017.
 */

public class SendMessageService extends Service {
    private static final String TAG = SendMessageService.class.getSimpleName() + "_SERVICE";

    private static final int MSG_START_CONNECT_TO_CLIENTS = 1001;

    private String myDeviceName;

    private Looper mServiceLooper;
    private Handler mServiceHandler;

    private Timer mTimer;
    private ServiceTimer mServiceTimer;

    private boolean sendingLostMessageComplete = true;
    private boolean sendingDeliveredMessageComplete = true;

    private List<DeviceInfo> connectedClients;

    private List<Message> lostMessages = new ArrayList<>(); //Недоставленные сообщения
    private List<Message> deliveredLostMessages = new ArrayList<>(); //Доставленные "Недоставленные" сообщения

    private Map<Message, Set<String>> banListLostMessage = new HashMap<>();
    private Map<Message, Set<String>> banListDeliveredMessage = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.debugLog("Start: SendMessageService");

        myDeviceName = App.sharedPreference().getInfoAboutMyDevice().getClientName();
        connectedClients = new ArrayList<>();

        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mServiceHandler.sendEmptyMessage(MSG_START_CONNECT_TO_CLIENTS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getExtras() != null) {
            Message msgFromMe =
                    (Message) intent.getSerializableExtra(ConfigIntent.MESSAGE);

            sendTargetMessage(msgFromMe);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private final class ServiceHandler extends Handler {

        ServiceHandler(Looper looper) {
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

            stopTimer();
            initTimer();
        }
    }

    private void initTimer() {
        if (mTimer == null) {
            mTimer = new Timer();

            if (mServiceTimer != null) mServiceTimer.cancel();
            mServiceTimer = new ServiceTimer();

            //Попытка коннекта каждые 15 секунд
            mTimer.schedule(
                    mServiceTimer,
                    TimeUnit.SECONDS.toMillis(15));
        }
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private final class ServiceTimer extends TimerTask {

        @Override
        public void run() {
            if (mServiceHandler != null) {
                Logger.debugLog("Timer sendLostMessageService");
                mServiceHandler.sendEmptyMessage(MSG_START_CONNECT_TO_CLIENTS);
            }
        }
    }

    private void runRequestedConnection() {
        List<DeviceInfo> potentialClients = App.sharedPreference().getAllPotentialClient();

        DeviceInfo[] potentialClientsArray = new DeviceInfo[potentialClients.size()];
        potentialClientsArray = potentialClients.toArray(potentialClientsArray);

        for (DeviceInfo potentialClient : potentialClientsArray) {
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

    private void requestConnection(DeviceInfo potentialClient) {
        Nearby.Connections.requestConnection(
                App.googleApiClient(),
                myDeviceName,
                potentialClient.getClientNearbyKey(),
                connectionLifecycleCallback
        ).setResultCallback(status -> {
            if (status.isSuccess()) {
                connectedClients.add(potentialClient);
                App.sharedPreference().removePotentialClient(potentialClient);
            } else {
                //Мертвые точки
                if (status.getStatus().toString().contains("STATUS_ENDPOINT_UNKNOWN")) {
                    App.sharedPreference().removePotentialClient(potentialClient);
                    // TODO: 14.11.2017 Update UI
                }
            }
        });
    }

    private ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endPoint, ConnectionInfo connectionInfo) {
            Logger.debugLog("onConnectionInitiated: START!");

            acceptConnection(endPoint, connectionInfo.getEndpointName());
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Logger.debugLog("Connect: OK");
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
        DeviceInfo[] connectedClientsArray = new DeviceInfo[connectedClients.size()];
        connectedClientsArray = connectedClients.toArray(connectedClientsArray);

        for (DeviceInfo connectedClient : connectedClientsArray) {
            if (connectedClient.getClientNearbyKey().equals(idEndPoint)) {
                connectedClients.remove(connectedClient);

                sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
                        .putExtra(ConfigIntent.CONNECTION_TARGET_ID, idEndPoint)
                        .putExtra(ConfigIntent.CONNECTION_FOOTER_TEXT, createFooterText())
                        .putExtra(ConfigIntent.CONNECTION_TARGET_IS_DISCONNECT, true));

                break;
            }
        }
    }

    private void acceptConnection(String idEndPoint, String nameEndPoint) {
        Nearby.Connections.acceptConnection(
                App.googleApiClient(),
                idEndPoint,
                messageListener
        ).setResultCallback(status -> {
            if (status.isSuccess()) {

                Logger.debugLog(String.format("Connected to: %s:%s", nameEndPoint, idEndPoint));
                //Соединение установленно
                connectedClients.add(
                        DeviceInfo.otherDevice(nameEndPoint, idEndPoint, null));

                sendBroadcast(new Intent(ConfigIntent.ACTION_CONNECTION_INITIATED)
                        .putExtra(ConfigIntent.CONNECTION_TARGET_ID, idEndPoint)
                        .putExtra(ConfigIntent.CONNECTION_TARGET_NAME, nameEndPoint)
                        .putExtra(ConfigIntent.CONNECTION_FOOTER_TEXT, createFooterText())
                        .putExtra(ConfigIntent.CONNECTION_TARGET_IS_DISCONNECT, false));
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

            responseFromClient(idEndPoint, receivedMessage);
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private void responseFromClient(String idEndPoint, Message message) {
        //Проверяем это Новое сообщение, или ответ о доставленном сообщении
        if (message.getState() == Message.State.DELIVERED_MESSAGE) {
            Message deliveredMessage = message.getDeliveredMessage();

            if (lostMessages.contains(deliveredMessage))
                lostMessages.remove(deliveredMessage);
            if (deliveredLostMessages.contains(deliveredMessage))
                deliveredLostMessages.add(deliveredMessage);
        } else {
            //Работаем с обычным типом сообщения
            //Определим, это совершенно новое сообщение, или это сообщение мы уже кому-то доставили
//            for (Message deliveredMessage : deliveredLostMessages) {
            if (deliveredLostMessages.contains(message)) {
                //даем ответ, что такое сообщение уже отправлено, и следует прекратить транслировать его
                sendBroadcastMessage(
                        Message.deliveredMessage(message), idEndPoint);
                return; //Нам не имеет смысла обрабатывать это сообщение, мы его уже доставили
            }
//            }

            String targetId = message.getTargetId();
            String targetName = message.getTargetName();


            if ((targetName != null && targetName.equals(myDeviceName)))
//                    || targetId != null && targetId.equals(myDevice.getClientNearbyKey())) Fixme Мы не знаем наш Id (
            {
                //Если у нас сменился Id, то сообщение доставить нам можно только по нашему имени,
                //если это произошло, то отправляем сообщение, что не нужно нас искать

                //Если сообщение нам
                // TODO: 14.11.2017 Save Message, Update Ui
//                mainActivity.showMessageForMe(message);

                //Сообщаем, что получили сообщение
                deliveredLostMessages.add(message);
//                deliverTargetMessage(
//                        Message.deliveredMessage(endPointId, message));
            } else if (targetId == null && targetName == null) {
                // TODO: 14.11.2017 Save Message, Update Ui. Проверить, есть ли сейчас Broadcast без конечной цели (Вроде нет)
                //Если target == null, значит это Broadcast
//                mainActivity.showBroadcastMessage(message);
            } else {
                if (!lostMessages.contains(message)) {
                    lostMessages.add(message);
                }
            }
        }
    }

    private void sendLostMessage() {
        //Защита, если мы еще не успели всем передать сообщение, а уже сработало событие
        if (sendingLostMessageComplete) {   //Lock.class ??
            sendingLostMessageComplete = false;

            if (!lostMessages.isEmpty()) {
                for (Message message : createValidMessage(lostMessages)) {
                    for (DeviceInfo client : connectedClients) {
                        //Отправляем всем
                        sendBroadcastMessage(message, client.getClientNearbyKey());
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

            if (!deliveredLostMessages.isEmpty()) {
                for (Message message : createValidMessage(deliveredLostMessages)) {
                    for (DeviceInfo client : connectedClients) {
                        //Отправляем всем
                        sendBroadcastMessage(message, client.getClientNearbyKey());
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
            if (ChronoUnit.MINUTES.between(message.getTimeSend(), LocalDateTime.now(Clock.systemDefaultZone())) > 2) {
                messages.remove(message);
                continue;
            }

            validMessages.add(message);
        }
        return validMessages;
    }

    private void sendTargetMessage(Message message) {
        Nearby.Connections.sendPayload(
                App.googleApiClient(),
                message.getTargetId(),
                Payload.fromBytes(
                        MessageConverter.toBytes(message)))
                .setResultCallback(status -> {
                    if (status.isSuccess()) {
                        Logger.debugLog("Send target: " + message.getTargetId());
                        // TODO: 14.11.2017 Update Ui
                    } else {
                        Logger.debugLog("Send FAIL!!" + status.getStatus());

                        //Мертвые точки
                        if (status.getStatus().toString().contains("STATUS_ENDPOINT_UNKNOWN")) {
                            // TODO: 14.11.2017 Update Ui
                            //Если точка изменилась, делаем из сообщения Бродкаст
                            lostMessages.add(
                                    Message.broadcastMessage(message.getFrom(), message.getTargetName(), message.getText(), message.getUUID()));
                        } else {
                            lostMessages.add(message);
                        }
                    }
                });
    }

    private void putClientInBanList(Message message, String idEndPoint, Map<Message, Set<String>> banList) {
        if (banList.get(message) == null) {
            banList.put(message, new HashSet<>(Collections.singletonList(idEndPoint)));
        } else if (!banList.get(message).contains(idEndPoint)) {
            banList.get(message).add(idEndPoint);
        }
    }

    private void sendBroadcastMessage(Message message, String idEndPoint) {
        List<String> receivers = createListReceiversMessage(message, idEndPoint);
        if (receivers.isEmpty()) return;

        Nearby.Connections.sendPayload(
                App.googleApiClient(),
                receivers,
                Payload.fromBytes(
                        MessageConverter.toBytes(message)))
                .setResultCallback(status -> {
                    if (status.isSuccess()) {
                        Logger.debugLog("Send target: " + message.getTargetId());

                        if (message.getState() == Message.State.DELIVERED_MESSAGE) {
                            putClientInBanList(message, idEndPoint, banListLostMessage);
                        } else {
                            putClientInBanList(message, idEndPoint, banListDeliveredMessage);
                        }

                        deliveredLostMessages.add(message);
                    } else {
                        Logger.debugLog("Send FAIL!!" + status.getStatus());

                        //Мертвые точки
                        if (status.getStatus().toString().contains("STATUS_ENDPOINT_UNKNOWN")) {
                            // TODO: 14.11.2017 Получатель не найден или не удалось доставить

                        }
                    }
                });
    }

    private List<String> createListReceiversMessage(Message message, String senderIdEndPoint) {
        List<String> validEndPoints = new ArrayList<>();

        for (DeviceInfo idEndPoint : connectedClients) {
            //Сообщение не нужно передавать назад отправителю
            if (idEndPoint.getClientName() != null
                    && idEndPoint.getClientName().equals(message.getFrom())
                    || idEndPoint.getClientNearbyKey() != null
                    && idEndPoint.getClientNearbyKey().equals(senderIdEndPoint)) continue;

            //Проверяем, пытались ли мы уже отправлять сообщение данному клиенту
            if (banListLostMessage.get(message) == null ||
                    !banListLostMessage.get(message).contains(senderIdEndPoint)) {
                validEndPoints.add(idEndPoint.getClientNearbyKey());
            }
        }
        return validEndPoints;
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
}
