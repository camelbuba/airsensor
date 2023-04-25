package org.qiaolei.airsensor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.qiaolei.airsensor.data.DeviceModel

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun addDevice(deviceModel: DeviceModel) {
        viewModelScope.launch {
            val devices = _uiState.value.devices
            devices.all { it.address != deviceModel.address }.let {
                if (it) {
                    Log.i("xxx", "addDevice")
                    devices.add(deviceModel)
                    _uiState.emit(_uiState.value)
//                    _uiState.update { currentState ->
//                        currentState.copy(devices = devices)
//                    }
                }
            }
        }
    }

    fun getDevice(address: String): DeviceModel? {
        val devices = _uiState.value.devices
        return devices.find { it.address == address }
    }

    fun updateDevice(device: DeviceModel) {
        val newDevice = DeviceModel(name = device.name, address = device.address, output = device.output, state = device.state)
        viewModelScope.launch {
            val devices = _uiState.value.devices
            devices.forEachIndexed { index, it ->
                if (it.address == device.address) {
                    devices[index] = newDevice
                }
            }
            _uiState.update {
                it.copy(devices = devices)
            }
        }
    }

    fun startScan() {
        _uiState.update {
            it.copy(isScanning = true)
        }
    }

    fun stopScan() {
        _uiState.update {
            it.copy(isScanning = false)
        }
    }
}