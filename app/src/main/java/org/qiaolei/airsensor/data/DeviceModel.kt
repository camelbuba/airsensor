package org.qiaolei.airsensor.data

data class DeviceModel(
    val name: String,
    val address: String,
    var state: DeviceConnectionState,
    val output: SensorOutput?
)
