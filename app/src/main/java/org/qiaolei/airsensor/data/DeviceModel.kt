package org.qiaolei.airsensor.data

data class DeviceModel(
    val name: String,
    val address: String,
    val state: DeviceConnectionState,
    val output: SensorOutput?


) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceModel

        return address == other.address
    }
}
