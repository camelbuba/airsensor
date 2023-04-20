package org.qiaolei.airsensor.data

data class Device(
    val name: String,
    val state: DeviceConnectionState,
    val output: SensorOutput?
)
