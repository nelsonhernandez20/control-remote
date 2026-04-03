package com.controlremote.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.controlremote.androidtv.proto.RemoteKeyCode
import com.controlremote.tv.AndroidTvConnectionPhase
import com.controlremote.tv.RemoteUiState
import com.controlremote.tv.RemoteViewModel
import com.controlremote.tv.TvBackend
import com.controlremote.tv.androidtv.DiscoveredTv
import com.controlremote.tv.remote.SamsungKey
import com.controlremote.tv.ui.theme.BackgroundDark
import com.controlremote.tv.ui.theme.KeyPurple
import com.controlremote.tv.ui.theme.KeyPurplePressed
import com.controlremote.tv.ui.theme.SurfaceCard
import com.controlremote.tv.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    showAds: Boolean = true,
    onRemoveAdsClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var keyboardText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.backend) {
        selectedTab = 0
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Control TV",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Wi‑Fi · Sin root",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    if (showAds) {
                        TextButton(onClick = onRemoveAdsClick) {
                            Text("Quitar anuncios", color = KeyPurplePressed)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = KeyPurplePressed
                )
            )
        },
        bottomBar = {
            if (showAds) {
                // Con edge-to-edge, el banner debe quedar por encima de la barra de gestos / nav.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(BackgroundDark)
                ) {
                    AdsBanner()
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceCard,
                contentColor = KeyPurplePressed
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Dispositivo") },
                    selectedContentColor = KeyPurplePressed,
                    unselectedContentColor = TextMuted
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Conectar") },
                    selectedContentColor = KeyPurplePressed,
                    unselectedContentColor = TextMuted
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Mando") },
                    selectedContentColor = KeyPurplePressed,
                    unselectedContentColor = TextMuted
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Teclado") },
                    selectedContentColor = KeyPurplePressed,
                    unselectedContentColor = TextMuted
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> DeviceTabContent(viewModel, state)
                    1 -> ConnectTabContent(viewModel, state)
                    2 -> PadTabContent(viewModel, state)
                    3 -> KeyboardCard(
                        backend = state.backend,
                        androidTvConnected = state.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED,
                        tvIpFilled = state.tvIp.isNotBlank(),
                        keyboardText = keyboardText,
                        onKeyboardTextChange = { keyboardText = it },
                        onSend = { viewModel.sendKeyboardText(keyboardText) },
                        isBusy = state.isBusy
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceTabContent(viewModel: RemoteViewModel, state: RemoteUiState) {
    Text(
        text = "Tipo de TV",
        style = MaterialTheme.typography.labelLarge,
        color = TextMuted
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RemoteSegmentButton(
            label = "Android TV",
            selected = state.backend == TvBackend.ANDROID_TV,
            onClick = { viewModel.setBackend(TvBackend.ANDROID_TV) },
            modifier = Modifier.weight(1f)
        )
        RemoteSegmentButton(
            label = "Samsung",
            selected = state.backend == TvBackend.SAMSUNG,
            onClick = { viewModel.setBackend(TvBackend.SAMSUNG) },
            modifier = Modifier.weight(1f)
        )
    }

    OutlinedTextField(
        value = state.tvIp,
        onValueChange = viewModel::setTvIp,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("IP de la TV") },
        placeholder = { Text("p. ej. 192.168.1.42") },
        singleLine = true,
        enabled = !state.isBusy && !state.isScanningLan,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KeyPurple,
            unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
            focusedLabelColor = KeyPurplePressed,
            cursorColor = KeyPurplePressed,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (state.backend) {
            TvBackend.ANDROID_TV -> {
                RemoteKeyButton(
                    label = "Buscar Android TV",
                    onClick = { viewModel.startAndroidTvLanDiscovery() },
                    enabled = !state.isBusy && !state.isScanningLan,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )
            }
            TvBackend.SAMSUNG -> {
                RemoteKeyButton(
                    label = "Buscar Samsung",
                    onClick = { viewModel.startSamsungLanScan() },
                    enabled = !state.isBusy && !state.isScanningLan,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )
            }
        }
        TextButton(
            onClick = { viewModel.clearDiscoveredList() },
            enabled = state.discoveredDevices.isNotEmpty()
        ) {
            Text("Limpiar", color = TextMuted)
        }
    }

    if (state.isScanningLan) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = KeyPurple,
            trackColor = SurfaceCard
        )
    }

    if (state.discoveredDevices.isNotEmpty()) {
        Text(
            text = "Encontrados — toca para usar IP",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(4.dp)) {
                state.discoveredDevices.forEach { d: DiscoveredTv ->
                    TextButton(
                        onClick = { viewModel.selectDiscoveredDevice(d) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${d.name}\n${d.host}:${d.port}",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    state.lastMessage?.let { msg ->
        Text(
            text = msg,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConnectTabContent(viewModel: RemoteViewModel, state: RemoteUiState) {
    when (state.backend) {
        TvBackend.ANDROID_TV -> {
            Text(
                text = "Android TV / Google TV",
                style = MaterialTheme.typography.titleSmall,
                color = TextMuted
            )
            AndroidTvPairingCard(viewModel, state)
        }
        TvBackend.SAMSUNG -> {
            Text(
                text = "Samsung",
                style = MaterialTheme.typography.titleSmall,
                color = TextMuted
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Las TVs Samsung no usan emparejamiento TLS. Indica la IP en la pestaña «Dispositivo» o búscala en la red; luego usa «Mando» y «Teclado».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PadTabContent(viewModel: RemoteViewModel, state: RemoteUiState) {
    when (state.backend) {
        TvBackend.ANDROID_TV -> {
            if (state.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED) {
                Text(
                    text = "Mando",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextMuted
                )
                AndroidTvPadCard(viewModel, state)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Conecta el control remoto en la pestaña «Conectar» para usar el mando.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        TvBackend.SAMSUNG -> {
            Text(
                text = "Samsung · puerto 8001",
                style = MaterialTheme.typography.titleSmall,
                color = TextMuted
            )
            SamsungRemotePad(viewModel, state)
        }
    }
}

@Composable
private fun KeyboardCard(
    backend: TvBackend,
    androidTvConnected: Boolean,
    tvIpFilled: Boolean,
    keyboardText: String,
    onKeyboardTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isBusy: Boolean
) {
    val enabled = when (backend) {
        TvBackend.ANDROID_TV -> androidTvConnected && !isBusy
        TvBackend.SAMSUNG -> tvIpFilled && !isBusy
    }
    val hint = when (backend) {
        TvBackend.ANDROID_TV -> "Abre el buscador en la TV, toca el campo de texto y envía (usa el teclado del sistema, IME)."
        TvBackend.SAMSUNG -> "Solo números 0‑9 (limitación de la API Samsung)"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Teclado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            OutlinedTextField(
                value = keyboardText,
                onValueChange = onKeyboardTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Texto a enviar…") },
                enabled = enabled,
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (keyboardText.isNotEmpty()) {
                        IconButton(onClick = { onKeyboardTextChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Borrar", tint = TextMuted)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KeyPurple,
                    unfocusedBorderColor = TextMuted.copy(alpha = 0.35f),
                    focusedLabelColor = KeyPurplePressed,
                    cursorColor = KeyPurplePressed
                )
            )
            RemoteKeyButton(
                label = "Enviar texto a la TV",
                onClick = onSend,
                enabled = enabled && keyboardText.isNotBlank()
            )
        }
    }
}

@Composable
private fun RemoteSegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = !selected,
        shape = RoundedCornerShape(14.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = KeyPurple,
                disabledContainerColor = KeyPurple,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = SurfaceCard,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RemoteKeyButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
    icon: ImageVector? = null,
    iconOnly: Boolean = false,
    iconSize: Dp = 26.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KeyPurple,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = SurfaceCard,
            disabledContentColor = TextMuted
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        )
    ) {
        when {
            icon != null && iconOnly -> {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(iconSize)
                )
            }
            icon != null -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
            else -> Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AndroidTvPairingCard(
    viewModel: RemoteViewModel,
    state: RemoteUiState
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "1 · Emparejamiento (primera vez)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            RemoteKeyButton(
                label = "Iniciar emparejamiento",
                onClick = viewModel::androidTvStartPairing,
                enabled = !state.isBusy && state.androidTvPhase != AndroidTvConnectionPhase.REMOTE_CONNECTED
            )
            OutlinedTextField(
                value = state.androidTvPin,
                onValueChange = viewModel::setAndroidTvPin,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código en la TV (6 hex)") },
                singleLine = true,
                enabled = !state.isBusy && state.androidTvPhase == AndroidTvConnectionPhase.PAIRING_NEED_PIN,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KeyPurple,
                    unfocusedBorderColor = TextMuted.copy(alpha = 0.35f)
                )
            )
            RemoteKeyButton(
                label = "Confirmar código",
                onClick = viewModel::androidTvFinishPairing,
                enabled = !state.isBusy && state.androidTvPhase == AndroidTvConnectionPhase.PAIRING_NEED_PIN
            )
            Text(
                text = "2 · Conectar control",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            RemoteKeyButton(
                label = "Conectar control remoto",
                onClick = viewModel::androidTvConnectRemote,
                enabled = !state.isBusy && state.androidTvPhase != AndroidTvConnectionPhase.REMOTE_CONNECTED
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RemoteKeyButton(
                    label = "Desconectar",
                    onClick = viewModel::androidTvDisconnect,
                    enabled = state.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED,
                    modifier = Modifier.weight(1f).height(48.dp)
                )
                Button(
                    onClick = viewModel::androidTvForgetDevice,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceCard,
                        contentColor = TextMuted
                    )
                ) {
                    Text("Olvidar TV")
                }
            }
        }
    }
}

@Composable
private fun AndroidTvPadCard(
    viewModel: RemoteViewModel,
    state: RemoteUiState
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AtvWide("Power", Icons.Filled.PowerSettingsNew, RemoteKeyCode.KEYCODE_POWER, viewModel, state.isBusy)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AtvPad(Icons.Filled.ChevronLeft, "Izquierda", RemoteKeyCode.KEYCODE_DPAD_LEFT, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AtvPad(Icons.Filled.KeyboardArrowUp, "Arriba", RemoteKeyCode.KEYCODE_DPAD_UP, viewModel, state.isBusy, Modifier.size(56.dp), 30.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    AtvPad(Icons.Filled.RadioButtonChecked, "OK", RemoteKeyCode.KEYCODE_DPAD_CENTER, viewModel, state.isBusy, Modifier.size(56.dp), 28.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    AtvPad(Icons.Filled.KeyboardArrowDown, "Abajo", RemoteKeyCode.KEYCODE_DPAD_DOWN, viewModel, state.isBusy, Modifier.size(56.dp), 30.dp)
                }
                AtvPad(Icons.Filled.ChevronRight, "Derecha", RemoteKeyCode.KEYCODE_DPAD_RIGHT, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AtvKeyLabeled(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", RemoteKeyCode.KEYCODE_BACK, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                AtvKeyLabeled(Icons.Filled.Home, "Home", RemoteKeyCode.KEYCODE_HOME, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                AtvKeyLabeled(Icons.Filled.Menu, "Menú", RemoteKeyCode.KEYCODE_MENU, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AtvKeyLabeled(Icons.AutoMirrored.Filled.VolumeUp, "Vol +", RemoteKeyCode.KEYCODE_VOLUME_UP, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                AtvKeyLabeled(Icons.AutoMirrored.Filled.VolumeDown, "Vol −", RemoteKeyCode.KEYCODE_VOLUME_DOWN, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                AtvKeyLabeled(Icons.AutoMirrored.Filled.VolumeOff, "Mute", RemoteKeyCode.KEYCODE_VOLUME_MUTE, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
            }
        }
    }
}

@Composable
private fun SamsungRemotePad(
    viewModel: RemoteViewModel,
    state: RemoteUiState
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SamsungWide("Power", Icons.Filled.PowerSettingsNew, SamsungKey.POWER, viewModel, state.isBusy)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SamsungPad(Icons.Filled.ChevronLeft, "Izquierda", SamsungKey.LEFT, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SamsungPad(Icons.Filled.KeyboardArrowUp, "Arriba", SamsungKey.UP, viewModel, state.isBusy, Modifier.size(56.dp), 30.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    SamsungPad(Icons.Filled.RadioButtonChecked, "OK", SamsungKey.ENTER, viewModel, state.isBusy, Modifier.size(56.dp), 28.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    SamsungPad(Icons.Filled.KeyboardArrowDown, "Abajo", SamsungKey.DOWN, viewModel, state.isBusy, Modifier.size(56.dp), 30.dp)
                }
                SamsungPad(Icons.Filled.ChevronRight, "Derecha", SamsungKey.RIGHT, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SamsungKeyLabeled(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", SamsungKey.BACK, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                SamsungKeyLabeled(Icons.Filled.Home, "Home", SamsungKey.HOME, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                SamsungKeyLabeled(Icons.Filled.Menu, "Menú", SamsungKey.MENU, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SamsungKeyLabeled(Icons.AutoMirrored.Filled.VolumeUp, "Vol +", SamsungKey.VOL_UP, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                SamsungKeyLabeled(Icons.AutoMirrored.Filled.VolumeDown, "Vol −", SamsungKey.VOL_DOWN, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                SamsungKeyLabeled(Icons.AutoMirrored.Filled.VolumeOff, "Mute", SamsungKey.MUTE, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SamsungKeyLabeled(Icons.Filled.Add, "CH +", SamsungKey.CH_UP, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                SamsungKeyLabeled(Icons.Filled.Remove, "CH −", SamsungKey.CH_DOWN, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
                SamsungKeyLabeled(Icons.AutoMirrored.Filled.Input, "HDMI", SamsungKey.SOURCE, viewModel, state.isBusy, Modifier.weight(1f).height(48.dp))
            }
        }
    }
}

@Composable
private fun SamsungWide(
    label: String,
    icon: ImageVector,
    key: SamsungKey,
    viewModel: RemoteViewModel,
    disabled: Boolean
) {
    RemoteKeyButton(
        label = label,
        onClick = { viewModel.sendKeySamsung(key) },
        enabled = !disabled,
        modifier = Modifier.fillMaxWidth(),
        icon = icon,
        iconOnly = false
    )
}

@Composable
private fun SamsungPad(
    icon: ImageVector,
    contentDescription: String,
    key: SamsungKey,
    viewModel: RemoteViewModel,
    disabled: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp
) {
    RemoteKeyButton(
        label = contentDescription,
        onClick = { viewModel.sendKeySamsung(key) },
        enabled = !disabled,
        modifier = modifier.padding(4.dp),
        icon = icon,
        iconOnly = true,
        iconSize = iconSize
    )
}

@Composable
private fun SamsungKeyLabeled(
    icon: ImageVector,
    label: String,
    key: SamsungKey,
    viewModel: RemoteViewModel,
    disabled: Boolean,
    modifier: Modifier = Modifier
) {
    RemoteKeyButton(
        label = label,
        onClick = { viewModel.sendKeySamsung(key) },
        enabled = !disabled,
        modifier = modifier,
        icon = icon,
        iconOnly = false
    )
}

@Composable
private fun AtvWide(
    label: String,
    icon: ImageVector,
    key: RemoteKeyCode,
    viewModel: RemoteViewModel,
    disabled: Boolean
) {
    RemoteKeyButton(
        label = label,
        onClick = { viewModel.sendKeyAndroidTv(key) },
        enabled = !disabled,
        modifier = Modifier.fillMaxWidth(),
        icon = icon,
        iconOnly = false
    )
}

@Composable
private fun AtvPad(
    icon: ImageVector,
    contentDescription: String,
    key: RemoteKeyCode,
    viewModel: RemoteViewModel,
    disabled: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp
) {
    RemoteKeyButton(
        label = contentDescription,
        onClick = { viewModel.sendKeyAndroidTv(key) },
        enabled = !disabled,
        modifier = modifier.padding(4.dp),
        icon = icon,
        iconOnly = true,
        iconSize = iconSize
    )
}

@Composable
private fun AtvKeyLabeled(
    icon: ImageVector,
    label: String,
    key: RemoteKeyCode,
    viewModel: RemoteViewModel,
    disabled: Boolean,
    modifier: Modifier = Modifier
) {
    RemoteKeyButton(
        label = label,
        onClick = { viewModel.sendKeyAndroidTv(key) },
        enabled = !disabled,
        modifier = modifier,
        icon = icon,
        iconOnly = false
    )
}
