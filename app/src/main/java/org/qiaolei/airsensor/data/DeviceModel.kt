package org.qiaolei.airsensor.data

import androidx.compose.runtime.MutableState

data class DeviceModel(
    val name: String,
    val address: String,
    var state: MutableState<DeviceConnectionState>,
    val output: SensorOutput?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceModel

        return address == other.address
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + (output?.hashCode() ?: 0)
        return result
    }
}
