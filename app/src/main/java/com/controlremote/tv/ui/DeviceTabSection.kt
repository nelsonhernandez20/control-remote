package com.controlremote.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlremote.tv.AndroidTvConnectionPhase
import com.controlremote.tv.R
import com.controlremote.tv.KnownDeviceInfo
import com.controlremote.tv.RemoteUiState
import com.controlremote.tv.RemoteViewModel
import com.controlremote.tv.TvBackend
import com.controlremote.tv.androidtv.DiscoveredTv
import com.controlremote.tv.ui.theme.DeviceListCard
import com.controlremote.tv.ui.theme.KeyPurple
import com.controlremote.tv.ui.theme.KeyPurplePressed
import com.controlremote.tv.ui.theme.MandoGradientEnd
import com.controlremote.tv.ui.theme.MandoGradientMid
import com.controlremote.tv.ui.theme.MandoGradientStart
import com.controlremote.tv.ui.theme.MandoOnlineGreen
import com.controlremote.tv.ui.theme.SurfaceCard
import com.controlremote.tv.ui.theme.TextMuted
/**
 * Pestaña Dispositivo: lista de TVs (conectar / desconectar / olvidar, reordenar)
 * y flujo «Buscar nuevo» con modal de emparejamiento Android TV.
 */
@Composable
fun DeviceTabSection(
    modifier: Modifier = Modifier,
    viewModel: RemoteViewModel,
    state: RemoteUiState,
    onNavigateToConnectTab: () -> Unit
) {
    var addMode by remember { mutableStateOf(false) }
    var connectDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshKnownDevices()
    }

    if (connectDialogVisible && state.backend == TvBackend.ANDROID_TV && state.tvIp.isNotBlank()) {
        AndroidTvConnectDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = {
                connectDialogVisible = false
                viewModel.cancelAndroidTvPairing()
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (addMode) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DeviceAddForm(
                    viewModel = viewModel,
                    state = state,
                    onCloseAddMode = { addMode = false },
                    onShowConnectDialog = {
                        connectDialogVisible = true
                    }
                )
            }
        } else {
            DeviceListOnly(
                modifier = Modifier.weight(1f),
                viewModel = viewModel,
                state = state,
                onSearchNew = { addMode = true },
                onNavigateToConnectTab = onNavigateToConnectTab,
                onShowPairingDialog = { connectDialogVisible = true }
            )
        }
    }
}

@Composable
private fun AndroidTvConnectDialog(
    state: RemoteUiState,
    viewModel: RemoteViewModel,
    onDismiss: () -> Unit
) {
    val host = state.tvIp.trim()
    if (host.isEmpty()) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = state.tvDisplayName?.takeIf { it.isNotBlank() } ?: host,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (state.androidTvPhase) {
                    AndroidTvConnectionPhase.REMOTE_CONNECTED -> {
                        Text(
                            text = stringResource(R.string.dialog_remote_connected_use_pad),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    AndroidTvConnectionPhase.PAIRING_NEED_PIN -> {
                        Text(
                            text = stringResource(R.string.dialog_enter_pin),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        OutlinedTextField(
                            value = state.androidTvPin,
                            onValueChange = viewModel::setAndroidTvPin,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.field_code_hex)) },
                            singleLine = true,
                            enabled = !state.isBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KeyPurple,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.35f)
                            )
                        )
                        RemoteKeyButton(
                            label = stringResource(R.string.btn_confirm_code),
                            onClick = viewModel::androidTvFinishPairing,
                            enabled = !state.isBusy && state.androidTvPin.length == 6
                        )
                    }
                    AndroidTvConnectionPhase.PAIRED -> {
                        Text(
                            text = stringResource(R.string.dialog_pair_ok_connect_remote),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        RemoteKeyButton(
                            label = stringResource(R.string.btn_connect_remote),
                            onClick = {
                                viewModel.androidTvConnectRemote()
                            },
                            enabled = !state.isBusy
                        )
                    }
                    AndroidTvConnectionPhase.ERROR -> {
                        Text(
                            text = state.lastMessage ?: stringResource(R.string.error_connection_default),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        RemoteKeyButton(
                            label = stringResource(R.string.btn_retry_pairing),
                            onClick = viewModel::androidTvStartPairing,
                            enabled = !state.isBusy
                        )
                    }
                    AndroidTvConnectionPhase.IDLE -> {
                        Text(
                            text = stringResource(R.string.dialog_start_pairing_on_tv),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        RemoteKeyButton(
                            label = stringResource(R.string.btn_start_pairing),
                            onClick = viewModel::androidTvStartPairing,
                            enabled = !state.isBusy
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close), color = KeyPurplePressed)
            }
        }
    )
}

@Composable
private fun DeviceListOnly(
    modifier: Modifier = Modifier,
    viewModel: RemoteViewModel,
    state: RemoteUiState,
    onSearchNew: () -> Unit,
    onNavigateToConnectTab: () -> Unit,
    onShowPairingDialog: () -> Unit
) {
    var forgetTarget by remember { mutableStateOf<KnownDeviceInfo?>(null) }

    forgetTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { forgetTarget = null },
            title = { Text(stringResource(R.string.forget_tv_title)) },
            text = {
                Text(
                    stringResource(R.string.forget_tv_message, target.title),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.forgetKnownDevice(target)
                        forgetTarget = null
                    }
                ) {
                    Text(stringResource(R.string.forget), color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { forgetTarget = null }) {
                    Text(stringResource(R.string.cancel), color = TextMuted)
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.tvs_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSearchNew) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_tv),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Text(
            text = stringResource(R.string.reorder_hint),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToConnectTab() }
        )

        if (state.knownDevices.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_devices_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.knownDevices.forEachIndexed { index, device ->
                    KnownDeviceRow(
                        device = device,
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth(),
                        listIndex = index,
                        listSize = state.knownDevices.size,
                        onMoveUp = {
                            if (index > 0) viewModel.moveKnownDevice(index, index - 1)
                        },
                        onMoveDown = {
                            if (index < state.knownDevices.size - 1) {
                                viewModel.moveKnownDevice(index, index + 1)
                            }
                        },
                        onForgetRequest = { forgetTarget = device },
                        onShowPairingDialog = onShowPairingDialog
                    )
                }
            }
        }

        SearchNewGradientButton(
            text = stringResource(R.string.search_new_device),
            onClick = onSearchNew,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun KnownDeviceRow(
    device: KnownDeviceInfo,
    state: RemoteUiState,
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier,
    listIndex: Int,
    listSize: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onForgetRequest: () -> Unit,
    onShowPairingDialog: () -> Unit
) {
    val isThis = state.tvIp.trim() == device.host.trim() && state.backend == device.backend
    val connected = when (device.backend) {
        TvBackend.ANDROID_TV ->
            isThis && state.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED
        TvBackend.SAMSUNG -> isThis
    }
    val hasAtvCreds = device.backend == TvBackend.ANDROID_TV &&
        viewModel.hasAndroidTvCredentials(device.host)

    Card(
        colors = CardDefaults.cardColors(containerColor = DeviceListCard),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = listIndex > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowUpward,
                            contentDescription = stringResource(R.string.cd_move_up),
                            tint = if (listIndex > 0) TextMuted else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = listIndex < listSize - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowDownward,
                            contentDescription = stringResource(R.string.cd_move_down),
                            tint = if (listIndex < listSize - 1) TextMuted else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Icon(
                    Icons.Outlined.Tv,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectKnownDevice(device) }
                ) {
                    Text(
                        text = device.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = device.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF3A3A3C))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            connected -> stringResource(R.string.badge_connected)
                            device.backend == TvBackend.SAMSUNG && isThis -> stringResource(R.string.badge_active)
                            else -> stringResource(R.string.badge_list)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (connected || (device.backend == TvBackend.SAMSUNG && isThis)) {
                            MandoOnlineGreen
                        } else {
                            TextMuted
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (device.backend) {
                    TvBackend.ANDROID_TV -> {
                        if (!connected) {
                            if (hasAtvCreds) {
                                TextButton(
                                    onClick = { viewModel.connectKnownDevice(device) },
                                    enabled = !state.isBusy
                                ) {
                                    Text(stringResource(R.string.btn_connect), color = KeyPurplePressed, maxLines = 1)
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        viewModel.selectKnownDevice(device)
                                        onShowPairingDialog()
                                    },
                                    enabled = !state.isBusy
                                ) {
                                    Text(stringResource(R.string.btn_pair), color = KeyPurplePressed, maxLines = 1)
                                }
                            }
                        } else {
                            TextButton(
                                onClick = { viewModel.disconnectKnownDevice(device) },
                                enabled = !state.isBusy
                            ) {
                                Text(stringResource(R.string.btn_disconnect), color = MandoOnlineGreen, maxLines = 1)
                            }
                        }
                    }
                    TvBackend.SAMSUNG -> {
                        if (!isThis) {
                            TextButton(
                                onClick = { viewModel.connectKnownDevice(device) },
                                enabled = !state.isBusy
                            ) {
                                Text(stringResource(R.string.btn_use), color = KeyPurplePressed, maxLines = 1)
                            }
                        } else {
                            TextButton(
                                onClick = { viewModel.disconnectKnownDevice(device) },
                                enabled = !state.isBusy
                            ) {
                                Text(stringResource(R.string.btn_deactivate), color = TextMuted, maxLines = 1)
                            }
                        }
                    }
                }

                val atvBlocksForget = device.backend == TvBackend.ANDROID_TV && isThis &&
                    (state.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED ||
                        state.androidTvPhase == AndroidTvConnectionPhase.PAIRING_NEED_PIN)
                val canForget = when (device.backend) {
                    TvBackend.ANDROID_TV -> !atvBlocksForget
                    TvBackend.SAMSUNG -> !(isThis && state.backend == TvBackend.SAMSUNG)
                }

                IconButton(
                    onClick = onForgetRequest,
                    enabled = canForget,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.cd_forget_tv),
                        tint = if (canForget) TextMuted else TextMuted.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchNewGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(MandoGradientStart, MandoGradientMid, MandoGradientEnd)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun DeviceAddForm(
    viewModel: RemoteViewModel,
    state: RemoteUiState,
    onCloseAddMode: () -> Unit,
    onShowConnectDialog: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCloseAddMode) {
                Text(stringResource(R.string.back_my_tvs), color = KeyPurplePressed)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = DeviceListCard),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_tv_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.add_tv_android_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = stringResource(R.string.tv_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RemoteSegmentButton(
                        label = stringResource(R.string.label_android_tv),
                        selected = state.backend == TvBackend.ANDROID_TV,
                        onClick = { viewModel.setBackend(TvBackend.ANDROID_TV) },
                        modifier = Modifier.weight(1f)
                    )
                    RemoteSegmentButton(
                        label = stringResource(R.string.label_samsung),
                        selected = state.backend == TvBackend.SAMSUNG,
                        onClick = { viewModel.setBackend(TvBackend.SAMSUNG) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = stringResource(R.string.network),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
                OutlinedTextField(
                    value = state.tvIp,
                    onValueChange = viewModel::setTvIp,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.tv_ip)) },
                    placeholder = { Text(stringResource(R.string.tv_ip_placeholder)) },
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
                                label = stringResource(R.string.search_android_tv),
                                onClick = { viewModel.startAndroidTvLanDiscovery() },
                                enabled = !state.isBusy && !state.isScanningLan,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )
                        }
                        TvBackend.SAMSUNG -> {
                            RemoteKeyButton(
                                label = stringResource(R.string.search_samsung),
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
                        Text(stringResource(R.string.clear), color = TextMuted)
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
                        text = stringResource(R.string.found),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            state.discoveredDevices.forEach { d: DiscoveredTv ->
                                DiscoveredTvRow(
                                    tv = d,
                                    backend = state.backend,
                                    onPick = {
                                        viewModel.selectDiscoveredDevice(d)
                                        if (state.backend == TvBackend.ANDROID_TV) {
                                            onShowConnectDialog()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (state.backend == TvBackend.ANDROID_TV && state.tvIp.isNotBlank()) {
                    RemoteKeyButton(
                        label = stringResource(R.string.connect_pair_with_ip),
                        onClick = onShowConnectDialog,
                        enabled = !state.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
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
        }
    }
}

@Composable
private fun DiscoveredTvRow(
    tv: DiscoveredTv,
    backend: TvBackend,
    onPick: () -> Unit
) {
    TextButton(
        onClick = onPick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = tv.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Text(
                text = "${tv.host}:${tv.port}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            if (backend == TvBackend.ANDROID_TV) {
                Text(
                    text = stringResource(R.string.tap_to_connect_pair),
                    style = MaterialTheme.typography.labelSmall,
                    color = KeyPurplePressed,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
