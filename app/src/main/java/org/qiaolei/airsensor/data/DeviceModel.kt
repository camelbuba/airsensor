package org.qiaolei.airsensor.data

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import kotlinx.coroutines.Job

data class DeviceModel(
    val name: String,
    val address: String,
    var state: DeviceConnectionState,
    val output: SensorOutput?,
    val bluetoothDevice: BluetoothDevice,
    var gatt: BluetoothGatt?,
    var job: Job?
)
