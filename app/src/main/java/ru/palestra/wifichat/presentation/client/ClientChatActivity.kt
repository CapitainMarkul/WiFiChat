package ru.palestra.wifichat.presentation.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import org.parceler.Parcels
import ru.palestra.wifichat.App
import ru.palestra.wifichat.R
import ru.palestra.wifichat.data.models.mappers.MessageMapper
import ru.palestra.wifichat.data.models.viewmodels.Client
import ru.palestra.wifichat.data.models.viewmodels.Message
import ru.palestra.wifichat.databinding.ActivityClientChatBinding
import ru.palestra.wifichat.domain.db.DbClient
import ru.palestra.wifichat.presentation.adapters.MessagesAdapter
import ru.palestra.wifichat.presentation.server_host.ServerActivity
import ru.palestra.wifichat.services.NearbyService
import ru.palestra.wifichat.services.NearbyService.PINE_CLIENT_UUID
import ru.palestra.wifichat.utils.ConfigIntent
import ru.palestra.wifichat.utils.CreateUiListUtil
import ru.palestra.wifichat.utils.TimeUtils
import java.util.UUID

class ClientChatActivity : AppCompatActivity() {

    companion object {
        fun createIntent(context: Context) = Intent(context, ClientChatActivity::class.java)
    }

    private lateinit var binding: ActivityClientChatBinding

    private val fadeAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(this, R.anim.fade_anim)
    }

    private var myDeviceInfo: Client? = null
    private var pineDeviceInfo: Client? = null

    private val permissionChecker = ServerActivity.PermissionChecker(this)

    private val uiListMessages = mutableListOf<Message>()
    private val messagesAdapter: MessagesAdapter by lazy {
        MessagesAdapter()
    }

    private val acceptConnectionToClientReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val findClients = Parcels.unwrap<List<Client>>(
                intent.getParcelableExtra(ConfigIntent.UPDATED_CLIENTS)
            )

            findClients.forEach { findClient ->
                if (findClient.uuid == PINE_CLIENT_UUID && pineDeviceInfo != null) {
                    /* Уже есть соединение, нужно обновить статус. */
                    updatePineOnlineState(findClient.isOnline)

                    if (!findClient.isOnline) {
                        /* В этой точке мы уже уверены, что потеряли связь с сосной. Обновляем статус. */
                        updatePineOnlineState(false)

                        pineDeviceInfo = null
                        myDeviceInfo?.let { initializeScreenByUserUseCase(it) }
                    }

                    /* Остальные устройства - не интересны. */
                    return
                } else if (findClient.uuid == PINE_CLIENT_UUID && pineDeviceInfo == null) {
                    /* Нашли сосну, подключаемся к чату. */
                    pineDeviceInfo = findClient
                    myDeviceInfo?.let { initializeScreenByUserUseCase(it, findClient) }

                    unsubscribeToPaneSearch()
                    /* Остальные устройства - не интересны. */
                    return
                }
            }
        }
    }

    private val deliveredMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.extras == null) return
            val message = intent.getSerializableExtra(ConfigIntent.MESSAGE) as Message?

            myDeviceInfo?.let { currentDevice ->
                /* Пропускаем все сообщения кроме своих. */
                if (message == null || uiListMessages.contains(message)) {
                    return
                }

                uiListMessages.clear()
                uiListMessages.addAll(messagesAdapter.messages)
                messagesAdapter.updateMessages(CreateUiListUtil.createUiMessagesList(uiListMessages, message))
                scrollToBottom()
            }
        }
    }

    private val searchingNewClients: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isDiscovery = intent.getBooleanExtra(ConfigIntent.STATUS_DISCOVERY, false)
            binding.containerSearchPineUi.progressPineSearch.isVisible = isDiscovery
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientChatBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        App.sharedPreference().infoAboutMyDevice.also { device ->
            initializeScreenByUserUseCase(device)
        }
    }

    override fun onStart() {
        super.onStart()
        permissionChecker.runIfAllPermissionsAccessed {
            subscribeToPaineSearchIfNeeded()
            subscribeToNewMessageIfNeeded()
        }
    }

    override fun onStop() {
        unsubscribeToPaneSearch()
        unsubscribeToNewMessage()

        super.onStop()
    }

    override fun onDestroy() {
        stopServices()
        super.onDestroy()
    }

    private fun startServices() {
        startService(Intent(this, NearbyService::class.java))
    }

    private fun stopServices() {
        stopService(Intent(this, NearbyService::class.java))
    }

    private fun initializeScreenByUserUseCase(
        myDeviceInfoLocal: Client,
        pineDeviceInfoLocal: Client? = pineDeviceInfo
    ) {
        myDeviceInfo = myDeviceInfoLocal

        if (myDeviceInfoLocal.state == Client.State.EMPTY) {
            /* Инициализируем и показываем UI авторизации. */
            initializeLoginUi()
            showLoginUi()
        } else if (pineDeviceInfoLocal == null) {
            startServices()

            /* Еще не нашли сосну, начинаем поиск. */
            subscribeToPaineSearchIfNeeded()

            /* Инициализируем и показываем UI поиска. */
            initializeSearchPineUi(myDeviceInfoLocal)
            showSearchPineUi()
        } else {
            /* Нашли сосну, подключаемся к чату. */
            updateSearchTextAnimation(false)
            /* Прекращаем поиск других устройств. */
            unsubscribeToPaneSearch()

            /* Инициализируем и показываем UI чата. */
            initializeChatUi(myDeviceInfoLocal, pineDeviceInfoLocal)
            showChatUi()
        }
    }

    private fun initializeLoginUi() = with(binding.containerLoginUi) {
        btnLogin.setOnClickListener {
            permissionChecker.runIfAllPermissionsAccessed {
                val createdClientModel = Client.myDevice(
                    etYourNickname.text.toString(), UUID.randomUUID().toString()
                )
                App.sharedPreference().saveInfoAboutMyDevice(createdClientModel)

                /* Меняем UI. */
                showSearchPineUi()

                /* Изменился сценарий - обрабатываем новое поведение UI. */
                initializeScreenByUserUseCase(createdClientModel)
            }
        }
    }

    private fun initializeSearchPineUi(currentDevice: Client) {
        updateSearchTextAnimation(true)

        /* Имя клиента. */
        binding.containerSearchPineUi.txtPineSearchInProgress.text =
            getString(R.string.client_search_a_pine_title, currentDevice.name)
    }

    private fun initializeChatUi(currentDevice: Client, pineDeviceInfo: Client) = with(binding.containerChatUi) {
        subscribeToNewMessageIfNeeded()

        /* Обработчик кнопки отправки сообщения в чат. */
        btnSendMessage.setOnClickListener {
            val textMessage = etxtTextMessage.text.toString()
            if (textMessage.isEmpty()) return@setOnClickListener

            val sendMessage = Message.newMessage(
                currentDevice.name, currentDevice.uuid, pineDeviceInfo.nearbyKey, pineDeviceInfo.uuid, textMessage
            )

            sendMessage(sendMessage)

            uiListMessages.clear()
            uiListMessages.addAll(messagesAdapter.messages)

            sendMessage.timeSend = -1
            uiListMessages.add(sendMessage)

            messagesAdapter.updateMessages(uiListMessages/*CreateUiListUtil.createUiMessagesList(uiListMessages, sendMessage)*/)
            scrollToBottom()
            etxtTextMessage.setText("")
        }

        txtTargetName.text = pineDeviceInfo.name
        updatePineOnlineState(pineDeviceInfo.isOnline)

        LinearLayoutManager(
            this@ClientChatActivity, LinearLayoutManager.VERTICAL, false
        ).apply {
            stackFromEnd = true
            rvChatMessages.layoutManager = this
        }

        rvChatMessages.adapter = messagesAdapter.apply {
            messagesAdapter.setCurrentDevice(currentDevice.uuid)

            /* Кнопка переотправки сообщения. */
            messagesAdapter.setResendOnClick { message ->
                sendMessage(message.apply {
                    timeSend = TimeUtils.timeNowLong()
                })

                uiListMessages.remove(message)

                val reSendMessage = Message.reSendMessage(message)
                uiListMessages.add(reSendMessage)
                CreateUiListUtil.createUiMessagesList(uiListMessages, reSendMessage)

                Toast.makeText(this@ClientChatActivity, R.string.message_resend, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage(message: Message) {
        startService(
            Intent(this@ClientChatActivity, NearbyService::class.java)
                .putExtra(ConfigIntent.MESSAGE, message)
        )
    }

    private fun updateSearchTextAnimation(needAnimation: Boolean) =
        with(binding.containerSearchPineUi.txtPineSearchInProgress) {
            if (needAnimation) startAnimation(fadeAnimation)
            else clearAnimation()
        }

    private fun updatePineOnlineState(isOnline: Boolean) {
        pineDeviceInfo?.isOnline = isOnline
        if (!isOnline) {
            pineDeviceInfo = null
        }
    }

    private fun scrollToBottom() {
        binding.containerChatUi.rvChatMessages.scrollToPosition(messagesAdapter.itemCount - 1)
    }

    private fun subscribeToPaineSearchIfNeeded() {
        if (myDeviceInfo?.state != Client.State.EMPTY && pineDeviceInfo == null) {
            registerReceiver(
                acceptConnectionToClientReceiver,
                IntentFilter(ConfigIntent.ACTION_CONNECTION_INITIATED)
            )
            registerReceiver(searchingNewClients, IntentFilter(ConfigIntent.ACTION_DISCOVERY))
        }
    }

    private fun unsubscribeToPaneSearch() {
        if (acceptConnectionToClientReceiver.isOrderedBroadcast) {
            unregisterReceiver(acceptConnectionToClientReceiver)
        }
        if (searchingNewClients.isOrderedBroadcast) {
            unregisterReceiver(searchingNewClients)
        }
    }

    private fun subscribeToNewMessageIfNeeded() {
        if (myDeviceInfo?.state != Client.State.EMPTY && pineDeviceInfo != null) {
            registerReceiver(
                acceptConnectionToClientReceiver,
                IntentFilter(ConfigIntent.ACTION_CONNECTION_INITIATED)
            )
            registerReceiver(deliveredMessageReceiver, IntentFilter(ConfigIntent.ACTION_DELIVERED_MESSAGE))
        }
    }

    private fun unsubscribeToNewMessage() {
        if (acceptConnectionToClientReceiver.isOrderedBroadcast) {
            unregisterReceiver(acceptConnectionToClientReceiver)
        }
        if (deliveredMessageReceiver.isOrderedBroadcast) {
            unregisterReceiver(deliveredMessageReceiver)
        }
    }

    private fun showLoginUi() = with(binding.containerLoginUi.root) {
        isVisible = true
        binding.containerChatUi.root.isVisible = !isVisible
        binding.containerSearchPineUi.root.isVisible = !isVisible
    }

    private fun showSearchPineUi() = with(binding.containerSearchPineUi.root) {
        isVisible = true
        binding.containerChatUi.root.isVisible = !isVisible
        binding.containerLoginUi.root.isVisible = !isVisible
    }

    private fun showChatUi() = with(binding.containerChatUi.root) {
        isVisible = true
        binding.containerLoginUi.root.isVisible = !isVisible
        binding.containerSearchPineUi.root.isVisible = !isVisible
    }
}