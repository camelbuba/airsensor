package org.qiaolei.airsensor.data

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt

data class DeviceModel(
    val name: String,
    val address: String,
    var state: DeviceConnectionState,
    val output: SensorOutput?,
    val bluetoothDevice: BluetoothDevice,
    var gatt: BluetoothGatt?
)
