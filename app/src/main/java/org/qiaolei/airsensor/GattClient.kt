package org.qiaolei.airsensor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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
import org.qiaolei.airsensor.data.DeviceConnectionState
import org.qiaolei.airsensor.data.DeviceModel

class GattClient(private val activity: MainActivity, private val viewModel: MainViewModel) {

    companion object {
        const val DEVICE_NAME = "air sensor"
        const val AUTO_CONNECT = true
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
                        output = null,
                        bluetoothDevice = device,
                        gatt = null
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
            viewModel.updateDevice(device, DeviceConnectionState.CONNECTING)
            val gattCallback = GattClientCallback(device)
            device.bluetoothDevice.connectGatt(activity.applicationContext, AUTO_CONNECT, gattCallback)
        } else {
            viewModel.updateDevice(device, DeviceConnectionState.OFFLINE)
            device.gatt?.disconnect()
        }
    }

    private class GattClientCallback(private val device: DeviceModel) : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED
            if (isSuccess && isConnected) {
                gatt?.discoverServices()
                device.gatt = gatt
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.services?.forEach {
                    Log.i(TAG, "found service: " + it.uuid)
                }
//                val service = gatt?.getService(SERVICE_UUID)
//                messageCharacteristic = service.getCharacteristic(MESSAGE_UUID)
            }

        }
    }
}