package org.qiaolei.airsensor

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.qiaolei.airsensor.data.DeviceConnectionState
import org.qiaolei.airsensor.data.DeviceModel
import org.qiaolei.airsensor.ui.theme.AirsensorTheme


class MainActivity : ComponentActivity() {

    private lateinit var gattClient: GattClient
    private val viewModel by viewModels<MainViewModel>()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gattClient = GattClient(this, viewModel)
        setContent {
            AirsensorTheme {
                Surface(color = MaterialTheme.colors.background) {
                    CoordinatorScreen(gattClient = gattClient, viewModel = viewModel)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun CoordinatorScreen(gattClient: GattClient, viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main_screen") {
        composable("main_screen") {
            MainScreen(gattClient = gattClient, viewModel = viewModel, navController = navController)
        }
        composable("settings_screen") {
            SettingsScreen(viewModel, navController)
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavHostController) {
    val settings = viewModel.getSettings()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = {
                    navController.navigate("main_screen")
                }) {
                    Icon(ImageVector.vectorResource(R.drawable.arrow_back), "返回")
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth().offset(x = (-24).dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "设置")
                }
            }
        )
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column {
                Text(text = "扫描过滤", style = MaterialTheme.typography.h3)
            }
            Column(
                modifier = Modifier.fillMaxWidth().offset(x = 20.dp, y = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = settings.scanFilterBluetoothAddress,
                    onValueChange = {
                        viewModel.updateSettingsFilterAddress(it)
                    },
                    label = {
                        Text("蓝牙地址")
                    },
                    maxLines = 1
                )
                OutlinedTextField(
                    value = settings.scanFilterBluetoothName,
                    onValueChange = {
                        viewModel.updateSettingsFilterName(it)
                    },
                    label = {
                        Text("蓝牙名字")
                    },
                    maxLines = 1
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MainScreen(
    gattClient: GattClient,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    navController: NavController
) {
    val mainUiState by viewModel.uiState.collectAsState()
    val devices = mainUiState.devices
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (mainUiState.isScanning) "扫描中(" + devices.size + ")" else "") },
            actions = {
                IconButton(onClick = {
                    navController.navigate("settings_screen")
                }) {
                    Icon(ImageVector.vectorResource(R.drawable.settings), "设置")
                }
                IconButton(onClick = {
                    gattClient.scan()
                }) {
                    Icon(ImageVector.vectorResource(R.drawable.bluetooth), "扫描")
                }
            })
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = px2dp(LocalContext.current, 12f))
        )
        {
            if (devices.isEmpty()) {
                NoDevicesScanned()
            } else {
                DeviceList(devices, gattClient)
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
    return Dp(pixels / scale * density)
}

@Composable
fun DeviceList(devices: List<DeviceModel>, gattClient: GattClient) {
    val vspacing = px2dp(LocalContext.current, 18f)
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(vspacing),
        contentPadding = PaddingValues(top = vspacing, bottom = vspacing)
    ) {
        items(items = devices, key = { device ->
            device.address
        }) { device ->
            DeviceCard(device, gattClient)
        }
    }
}

@Composable
fun DeviceStateLine(device: DeviceModel, gattClient: GattClient, modifier: Modifier = Modifier) {
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
            onClick = {
                gattClient.connect(device)
            }, modifier = Modifier
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

                DeviceConnectionState.FAILURE -> {
                    Icon(
                        ImageVector.vectorResource(R.drawable.state_connected_inner),
                        "",
                        tint = Color(0xFFDC1A1A)
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
            Text(
                device.temperature,
                style = MaterialTheme.typography.h1,
                modifier = Modifier.offset(x = 10.dp)
            )
            Text("℃", style = MaterialTheme.typography.h3, modifier = Modifier.offset(x = 12.dp, y = 3.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = 28.dp, y = 18.dp)) {
            Text("湿度", style = MaterialTheme.typography.h2)
            Text(
                device.humidity,
                style = MaterialTheme.typography.h1,
                modifier = Modifier.offset(x = 10.dp)
            )
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
fun DeviceCard(device: DeviceModel, gattClient: GattClient) {
    val context = LocalContext.current
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
//            .height(px2dp(context, 210f))
            .height(210.dp)
    ) {
        Column {
            DeviceStateLine(device, gattClient)
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

                DeviceConnectionState.FAILURE -> {
                    DeviceStateText("连接失败")
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