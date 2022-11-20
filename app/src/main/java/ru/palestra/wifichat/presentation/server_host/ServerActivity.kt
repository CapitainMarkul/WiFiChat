package ru.palestra.wifichat.presentation.server_host

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.parceler.Parcels
import ru.palestra.wifichat.App
import ru.palestra.wifichat.R
import ru.palestra.wifichat.data.models.mappers.ClientMapper
import ru.palestra.wifichat.data.models.viewmodels.Client
import ru.palestra.wifichat.databinding.ActivityServerBinding
import ru.palestra.wifichat.presentation.adapters.ClientsAdapter
import ru.palestra.wifichat.presentation.common.dialogs.DialogManager
import ru.palestra.wifichat.presentation.common.dialogs.DialogManagerApi
import ru.palestra.wifichat.presentation.common.permissions.PermissionManager
import ru.palestra.wifichat.presentation.common.permissions.PermissionManagerApi
import ru.palestra.wifichat.services.NearbyService
import ru.palestra.wifichat.services.NearbyService.PINE_CLIENT_UUID
import ru.palestra.wifichat.utils.ConfigIntent
import ru.palestra.wifichat.utils.CreateUiListUtil
import java.text.MessageFormat.format

class ServerActivity : AppCompatActivity() {

    companion object {
        private const val PINE_CLIENT_NAME = "Чат \"Любителей природы\""
        private const val ZERO_CONNECTED = 0

        fun createIntent(context: Context) = Intent(context, ServerActivity::class.java)
    }

    private lateinit var binding: ActivityServerBinding

    private var myDeviceInfo: Client? = null
    private var clientsAdapter: ClientsAdapter? = null

    private val permissionChecker = PermissionChecker(this)

    private val acceptConnectionToClientReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val clients = Parcels.unwrap<List<Client>>(intent.getParcelableExtra(ConfigIntent.UPDATED_CLIENTS))
            clientsAdapter!!.updateClients(clients)
        }
    }

    private val searchingNewClients: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isDiscovery = intent.getBooleanExtra(ConfigIntent.STATUS_DISCOVERY, false)
            if (isDiscovery) {
                binding.progressDiscovery.visibility = View.VISIBLE
            } else {
                binding.progressDiscovery.visibility = View.INVISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerBinding.inflate(layoutInflater).also {
            setContentView(it.root)
            it.txtTryStartServer.isActivated = false
        }

        binding.txtTryStartServer.setOnClickListener {
            permissionChecker.runIfAllPermissionsAccessed {
                binding.txtTryStartServer.isActivated = true
                binding.txtTryStartServer.setText(R.string.running_pine_server)
                binding.progressDiscovery.visibility = View.VISIBLE

                startServices()
            }
        }

        updateConnectedClientCount(ZERO_CONNECTED)

        App.sharedPreference().infoAboutMyDevice.also { device ->
            initializePineClient(device)
        }

        setupClientsRecyclerView()
    }

    private fun initializePineClient(currentDevice: Client) {
        myDeviceInfo = currentDevice

        if (currentDevice.state == Client.State.EMPTY) {
            val createdClientModel = Client.myDevice(PINE_CLIENT_NAME, PINE_CLIENT_UUID)
            App.sharedPreference().saveInfoAboutMyDevice(createdClientModel)

            initializePineClient(createdClientModel)
        }
    }

    override fun onStart() {
        super.onStart()
        permissionChecker.runIfAllPermissionsAccessed {
            registerReceiver(acceptConnectionToClientReceiver, IntentFilter(ConfigIntent.ACTION_CONNECTION_INITIATED))
            registerReceiver(searchingNewClients, IntentFilter(ConfigIntent.ACTION_DISCOVERY))
            setupWasConnectedClients()
        }
    }

    override fun onResume() {
        super.onResume()

        //Проверим, появились ли новые, подключенные клиенты
        if (CreateUiListUtil.getUiClients().size > 0) {
            clientsAdapter!!.updateClients(
                CreateUiListUtil.getUiClients()
            )
        }
    }

    override fun onStop() {
        if (acceptConnectionToClientReceiver.isOrderedBroadcast) {
            unregisterReceiver(acceptConnectionToClientReceiver)
        }
        if (searchingNewClients.isOrderedBroadcast) {
            unregisterReceiver(searchingNewClients)
        }
        super.onStop()
    }

    override fun onDestroy() {
        binding.txtTryStartServer.isActivated = false

        stopServices()
        super.onDestroy()
    }

    private fun startServices() {
        startService(Intent(this, NearbyService::class.java))
    }

    private fun stopServices() {
        stopService(Intent(this, NearbyService::class.java))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.default_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorite -> {
                AlertDialog.Builder(this)
                    .setMessage("Уже уходите?")
                    .setNegativeButton("Нет", null)
                    .setPositiveButton("Ага") { _: DialogInterface?, _: Int ->
                        CreateUiListUtil.clearViewClients()
                        stopServices()
                        finish()
                    }
                    .create().show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupClientsRecyclerView() {
        binding.rvClients.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        clientsAdapter = ClientsAdapter()
        binding.rvClients.adapter = clientsAdapter
    }

    private fun setupWasConnectedClients() {
        val wasConnectedClients =
            ClientMapper.toListClientView(App.dbClient().allWasConnectedClients)
        clientsAdapter?.updateClients(wasConnectedClients)

        updateConnectedClientCount(wasConnectedClients.size)
    }

    private fun updateConnectedClientCount(newCount: Int) {
        binding.txtConnectedDevices.text = format(
            getString(
                if (newCount == 0) R.string.server_counter_zero_connected_title
                else R.string.server_counter_connected_title
            )
        )
    }

    class PermissionChecker(private val activity: AppCompatActivity) {
        private val permissionManager: PermissionManagerApi<String> = PermissionManager(activity)
        private val dialogManager: DialogManagerApi = DialogManager(activity)

        fun runIfAllPermissionsAccessed(block: () -> Unit) {
            if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!PermissionManager.isPermissionGranted(activity, NEARBY_WIFI_DEVICES)) {
                    permissionManager.requestSinglePermission(
                        NEARBY_WIFI_DEVICES,
                        onPermissionGranted = { runIfAllPermissionsAccessed(block) },
                        onPermissionDenied = {
                            dialogManager.showGpsPermissionDeniedInfoDialog {
                                activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        })
                }
            }

            if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when {
                    !PermissionManager.isPermissionGranted(activity, BLUETOOTH_CONNECT) -> {
                        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionManager.requestSinglePermission(
                                BLUETOOTH_CONNECT,
                                onPermissionGranted = { runIfAllPermissionsAccessed(block) },
                                onPermissionDenied = {
                                    dialogManager.showSimpleErrorDialog(R.string.error_dialog_start_server) {
                                        runIfAllPermissionsAccessed(block)
                                    }
                                })
                        }
                    }
                    !PermissionManager.isPermissionGranted(activity, BLUETOOTH_ADVERTISE) -> {
                        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionManager.requestSinglePermission(
                                BLUETOOTH_ADVERTISE,
                                onPermissionGranted = { runIfAllPermissionsAccessed(block) },
                                onPermissionDenied = {
                                    dialogManager.showSimpleErrorDialog(R.string.error_dialog_start_server) {
                                        runIfAllPermissionsAccessed(block)
                                    }
                                })
                        }
                    }
                    !PermissionManager.isPermissionGranted(activity, BLUETOOTH_SCAN) -> {
                        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionManager.requestSinglePermission(
                                BLUETOOTH_SCAN,
                                onPermissionGranted = { runIfAllPermissionsAccessed(block) },
                                onPermissionDenied = {
                                    dialogManager.showSimpleErrorDialog(R.string.error_dialog_start_server) {
                                        runIfAllPermissionsAccessed(block)
                                    }
                                })
                        }
                    }
                }
            }

            when {
                !PermissionManager.isPermissionGranted(activity, ACCESS_FINE_LOCATION) -> {
                    permissionManager.requestSinglePermission(
                        ACCESS_FINE_LOCATION,
                        onPermissionGranted = { runIfAllPermissionsAccessed(block) },
                        onPermissionDenied = {
                            dialogManager.showGpsPermissionDeniedInfoDialog {
                                activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        })
                }

                else -> checkHardwareAvailable(block)
            }
        }

        @SuppressLint("MissingPermission")
        private inline fun checkHardwareAvailable(crossinline block: () -> Unit) {
            if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == false) {
                dialogManager.showDisabledBluetoothInfoDialog {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    activity.startActivityForResult(enableBtIntent, 0)
                }
            } else if (!isGpsLocationEnabled()) {
                dialogManager.showDisabledGpsInfoDialog {
                    activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            } else block()
        }

        private fun isGpsLocationEnabled(): Boolean {
            val mLocationManager =
                activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            return mLocationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        }
    }
}