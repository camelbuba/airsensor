package org.qiaolei.airsensor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.qiaolei.airsensor.data.DeviceModel

data class MainUiState(
    val isScanning: Boolean = false,
    val devices: SnapshotStateList<DeviceModel> = mutableStateListOf(),
    var settings: MutableState<Settings> = mutableStateOf(Settings(scanFilterBluetoothAddress = "", scanFilterBluetoothName = ""))
)