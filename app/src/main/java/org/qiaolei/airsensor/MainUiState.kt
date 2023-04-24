package org.qiaolei.airsensor

import org.qiaolei.airsensor.data.DeviceModel

data class MainUiState(
    val isScanning: Boolean = false,
    val devices: MutableList<DeviceModel> = mutableListOf()
)