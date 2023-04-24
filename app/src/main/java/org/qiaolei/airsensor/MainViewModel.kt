package org.qiaolei.airsensor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.qiaolei.airsensor.data.DeviceConnectionState
import org.qiaolei.airsensor.data.DeviceModel

class MainViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()


    fun updateDevices() {
        val devices = listOf(DeviceModel("device 3", DeviceConnectionState.FOUND, null))
        _uiState.update { currentState ->
            currentState.copy(
                devices = devices,
                isScanning = !currentState.isScanning
            )
        }
    }
}