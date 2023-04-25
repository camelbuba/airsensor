@file:OptIn(DelicateCoroutinesApi::class)

package org.qiaolei.airsensor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import kotlinx.coroutines.*
import org.qiaolei.airsensor.data.DeviceConnectionState
import org.qiaolei.airsensor.data.DeviceModel
import java.util.*

class GattClient(private val activity: MainActivity, private val viewModel: MainViewModel) {

    companion object {
        const val DEVICE_NAME = "air sensor"
        const val AUTO_CONNECT = true
        val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val TEMPERATURE_MESSAGE_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val HUMIDITY_MESSAGE_UUID: UUID = UUID.fromString("32db52a6-5c87-407b-8bc0-c4170df277cd")
        const val REQUEST_BLUETOOTH_CODE = 19
        const val TAG = "GattClient"
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val address = result?.device?.address
            val device = result?.device
            Log.i(TAG, "on scanning...")
            if (address != null && device != null) {
                Log.i(TAG, "found device: $address")
                val name: String = if (device.name == null || device.name.isEmpty()) "匿名设备" else device.name
                val deviceModel =
                    DeviceModel(
                        address = address,
                        name = name,
                        state = DeviceConnectionState.FOUND,
                        temperature = "--",
                        humidity = "--",
                        bluetoothDevice = device,
                        gatt = null,
                        job = null
                    )
                viewModel.addDevice(deviceModel)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            Log.i(TAG, "onBatchScanResult")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.i(TAG, "scan failed")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun scan() {
        if (ActivityCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), REQUEST_BLUETOOTH_CODE
            )
            return
        }

        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!bluetoothManager.adapter.isEnabled) {
            return
        }

        val leScanner = bluetoothManager.adapter.bluetoothLeScanner
        val scanSettings: ScanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()
        val filter = ScanFilter.Builder().setDeviceName(DEVICE_NAME)
            .build()

        if (!viewModel.uiState.value.isScanning) {
            viewModel.startScan()
            leScanner.startScan(listOf(filter), scanSettings, scanCallback)
            Log.i(TAG, "start scan")
        } else {
            Log.i(TAG, "stop scan")
            leScanner.stopScan(scanCallback)
            viewModel.stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: DeviceModel) {
        val waitConnect = when (device.state) {
            DeviceConnectionState.FOUND -> {
                true
            }

            DeviceConnectionState.CONNECTING -> {
                false
            }

            DeviceConnectionState.CONNECTED -> {
                false
            }

            DeviceConnectionState.OFFLINE -> {
                true
            }
        }
        if (waitConnect) {
            viewModel.updateDeviceState(device, DeviceConnectionState.CONNECTING)
            val gattCallback = GattClientCallback(device, viewModel)
            device.bluetoothDevice.connectGatt(
                activity.applicationContext,
                AUTO_CONNECT,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            viewModel.updateDeviceState(device, DeviceConnectionState.OFFLINE)
            val dev = viewModel.getDevice(device.address)
            dev?.job?.cancel()
            dev?.gatt?.disconnect()
        }
    }

    private class GattClientCallback(private val device: DeviceModel, private val viewModel: MainViewModel) :
        BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    viewModel.updateDeviceState(device, DeviceConnectionState.CONNECTED)
                    gatt?.discoverServices()
                    val dev = viewModel.getDevice(device.address)
                    dev?.gatt = gatt
                }
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.i(TAG, "on services discovered")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.services?.forEach {
                    Log.i(TAG, "found service: " + it.uuid)
                }
                val service = gatt?.getService(SERVICE_UUID)
                val temperatureChar = service?.getCharacteristic(TEMPERATURE_MESSAGE_UUID)
                val humidityChar = service?.getCharacteristic(HUMIDITY_MESSAGE_UUID)
                val job = GlobalScope.launch {
                    val dev = viewModel.getDevice(device.address)
                    dev?.let {
                        while (true) {
                            temperatureChar?.let {
                                if (!gatt.readCharacteristic(temperatureChar)) {
                                    Log.e(TAG, "read temperature value error")
                                }
                            }
                            humidityChar?.let {
                                if (!gatt.readCharacteristic(humidityChar)) {
                                    Log.e(TAG, "read humidity value error")
                                }
                            }
                            delay(2000)
                        }
                    }
                }
                device.job = job
            }

        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.let {
                    if (it.uuid == TEMPERATURE_MESSAGE_UUID) {
                        val temperature = String(it.value)
                        viewModel.updateDeviceTemperature(device, temperature)
                        Log.i(TAG, "temperature: $temperature")
                    }
                    if (it.uuid == HUMIDITY_MESSAGE_UUID) {
                        val humidity = String(it.value)
                        viewModel.updateDeviceTemperature(device, humidity)
                        Log.i(TAG, "temperature: $humidity")
                    }
                }
            }
        }
    }
}