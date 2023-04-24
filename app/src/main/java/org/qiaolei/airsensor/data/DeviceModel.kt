package org.qiaolei.airsensor.data

data class DeviceModel(
    val name: String,
    val state: DeviceConnectionState,
    val output: SensorOutput?
)
