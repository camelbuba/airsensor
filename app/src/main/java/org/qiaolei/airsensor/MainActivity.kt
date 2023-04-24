package org.qiaolei.airsensor

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.qiaolei.airsensor.data.DeviceModel
import org.qiaolei.airsensor.data.DeviceConnectionState
import org.qiaolei.airsensor.data.SensorOutput
import org.qiaolei.airsensor.ui.theme.AirsensorTheme


class MainActivity : ComponentActivity() {
    private lateinit var devices: List<DeviceModel>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        devices = mockDeviceList()
        devices = mockEmptyDeviceList()
        setContent {

            AirsensorTheme {
                Surface(color = MaterialTheme.colors.background) {
                    MainScreen()
                }
            }
        }
    }
}

fun mockDeviceList(): List<DeviceModel> {
    return listOf(
        DeviceModel("long long long device 1", DeviceConnectionState.CONNECTED, SensorOutput(28.30f, 40.0f)),
        DeviceModel("device 2", DeviceConnectionState.OFFLINE, null),
        DeviceModel("device 3", DeviceConnectionState.FOUND, null),
        DeviceModel("device 4", DeviceConnectionState.FOUND, null)
    )
}

fun mockEmptyDeviceList(): List<DeviceModel> {
    return listOf()
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, mainViewModel: MainViewModel = viewModel()) {
    val mainUiState by mainViewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (mainUiState.isScanning) "扫描中" else "Air Sensor") },
            actions = {
                IconButton(onClick = {
                    mainViewModel.updateDevices()
                }) {
                    Icon(ImageVector.vectorResource(R.drawable.bluetooth), "扫描")
                }
            })
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = px2dp(LocalContext.current, 12f))
        )
        {
            if (mainUiState.devices.isEmpty()) {
                NoDevicesScanned()
            } else {
                DeviceList(mainUiState.devices)
            }
        }
    }
}

@Composable
fun NoDevicesScanned() {
    Text(
        "还未扫描附近的设备", modifier = Modifier.fillMaxWidth().offset(y = 36.dp), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.h2
    )
}

fun px2dp(context: Context, pixels: Float): Dp {
    val density = context.resources.displayMetrics.density
    val scale = 2
    Log.i("xxx", density.toString())
    return Dp(pixels / scale * density)
}

@Composable
fun DeviceList(devices: List<DeviceModel>) {
    val vspacing = px2dp(LocalContext.current, 18f)
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(vspacing),
        contentPadding = PaddingValues(top = vspacing, bottom = vspacing)
    ) {
        items(devices.size) { index ->
            val device = devices[index]
            Device(device)
        }
    }
}

@Composable
fun DeviceStateLine(device: DeviceModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Text(
            text = device.name, Modifier.offset(x = px2dp(context, 12f))
                .weight(1f)
                .wrapContentWidth(Alignment.Start)
                .fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = {}, modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.End)
        ) {
            Icon(
                ImageVector.vectorResource(R.drawable.state_connected_outter),
                "",
                tint = Color(0xFFFFFFFF)
            )
            when (device.state) {
                DeviceConnectionState.CONNECTED -> {
                    Icon(
                        ImageVector.vectorResource(R.drawable.state_connected_inner),
                        "",
                        tint = Color(0xFF1FC719)
                    )
                }

                DeviceConnectionState.CONNECTING -> {
                    Icon(
                        ImageVector.vectorResource(R.drawable.state_connected_inner),
                        "",
                        tint = Color(0xFFDCD041)
                    )
                }

                DeviceConnectionState.FOUND -> {
                    Icon(
                        ImageVector.vectorResource(R.drawable.state_connected_inner),
                        "",
                        tint = Color(0xFF4157DC)
                    )
                }

                DeviceConnectionState.OFFLINE -> {
                    Icon(
                        ImageVector.vectorResource(R.drawable.state_connected_inner),
                        "",
                        tint = Color(0xFFD1D1D1)
                    )
                }
            }
        }

    }
}

@Composable
fun SensorOutput(device: DeviceModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = 28.dp)) {
            Text("温度", style = MaterialTheme.typography.h2)
            if (device.output != null) {
                Text(
                    device.output.temperature.toString(),
                    style = MaterialTheme.typography.h1,
                    modifier = Modifier.offset(x = 10.dp)
                )
            }
            Text("℃", style = MaterialTheme.typography.h3, modifier = Modifier.offset(x = 12.dp, y = 3.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = 28.dp, y = 18.dp)) {
            Text("湿度", style = MaterialTheme.typography.h2)
            if (device.output != null) {
                Text(
                    device.output.humidity.toString(),
                    style = MaterialTheme.typography.h1,
                    modifier = Modifier.offset(x = 10.dp)
                )
            }
            Text("%", style = MaterialTheme.typography.h3, modifier = Modifier.offset(x = 12.dp, y = 3.dp))
        }
    }
}

@Composable
fun DeviceStateText(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().offset(y = (-20).dp)
//            .border(1.dp, Color.Red)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun Device(device: DeviceModel) {
    val context = LocalContext.current
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
//            .height(px2dp(context, 210f))
            .height(210.dp)
    ) {
        Column {
            DeviceStateLine(device)
            when (device.state) {
                DeviceConnectionState.CONNECTING -> {
                    DeviceStateText("连接设备中")
                }

                DeviceConnectionState.CONNECTED -> {
                    SensorOutput(device)
                }

                DeviceConnectionState.FOUND -> {
                    DeviceStateText("等待连接设备")
                }

                DeviceConnectionState.OFFLINE -> {
                    DeviceStateText("设备已离线")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AirsensorTheme {
    }
}