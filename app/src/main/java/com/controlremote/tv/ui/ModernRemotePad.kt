package com.controlremote.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.controlremote.tv.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlremote.androidtv.proto.RemoteKeyCode
import com.controlremote.tv.TvBackend
import com.controlremote.tv.remote.SamsungKey
import com.controlremote.tv.ui.theme.MandoBackground
import com.controlremote.tv.ui.theme.MandoGradientEnd
import com.controlremote.tv.ui.theme.MandoGradientMid
import com.controlremote.tv.ui.theme.MandoGradientStart
import com.controlremote.tv.ui.theme.MandoIconWhite
import com.controlremote.tv.ui.theme.MandoOnlineGreen
import com.controlremote.tv.ui.theme.MandoPowerRed
import com.controlremote.tv.ui.theme.MandoSurface

/**
 * Mando estilo referencia: cabecera, D‑pad con anillo en degradado, puntos de página,
 * columna VOL, rejilla 3×2 y canales. Sin accesos directos a apps de streaming.
 */
@Composable
fun ModernRemotePad(
    modifier: Modifier = Modifier,
    backend: TvBackend,
    controlsEnabled: Boolean,
    tvIpLabel: String,
    onAndroidKey: (RemoteKeyCode) -> Unit,
    onSamsungKey: (SamsungKey) -> Unit,
    onOpenDeviceTab: () -> Unit
) {
    var padPage by remember { mutableIntStateOf(0) }
    var roomMenu by remember { mutableStateOf(false) }

    val ringBrush = remember {
        Brush.linearGradient(
            colors = listOf(MandoGradientStart, MandoGradientMid, MandoGradientEnd)
        )
    }

    val defaultRoom = stringResource(R.string.room_default_name)
    val roomText = when {
        tvIpLabel.isNotBlank() -> tvIpLabel
        else -> defaultRoom
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MandoBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onOpenDeviceTab) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.cd_device_settings),
                    tint = MandoIconWhite
                )
            }
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { roomMenu = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (controlsEnabled) MandoOnlineGreen else Color(0xFF555555))
                    )
                    Text(
                        text = roomText,
                        color = MandoIconWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MandoIconWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = roomMenu, onDismissRequest = { roomMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_change_tv)) },
                        onClick = {
                            roomMenu = false
                            onOpenDeviceTab()
                        }
                    )
                }
            }
            IconButton(
                onClick = {
                    when (backend) {
                        TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_POWER)
                        TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.POWER)
                    }
                },
                enabled = controlsEnabled
            ) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = stringResource(R.string.cd_power),
                    tint = MandoPowerRed
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (padPage) {
                0 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GradientDpad(
                        ringBrush = ringBrush,
                        controlsEnabled = controlsEnabled,
                        backend = backend,
                        onAndroidKey = onAndroidKey,
                        onSamsungKey = onSamsungKey
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PageDots(padPage = padPage, onSelect = { padPage = it })
                }
                1 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NumericKeypadPage(
                        backend = backend,
                        controlsEnabled = controlsEnabled,
                        onAndroidKey = onAndroidKey,
                        onSamsungKey = onSamsungKey
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PageDots(padPage = padPage, onSelect = { padPage = it })
                }
            }
        }

        BottomControlRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            backend = backend,
            controlsEnabled = controlsEnabled,
            onAndroidKey = onAndroidKey,
            onSamsungKey = onSamsungKey,
            onOpenNumpadPage = { padPage = 1 }
        )
    }
}

@Composable
private fun GradientDpad(
    ringBrush: Brush,
    controlsEnabled: Boolean,
    backend: TvBackend,
    onAndroidKey: (RemoteKeyCode) -> Unit,
    onSamsungKey: (SamsungKey) -> Unit
) {
    val inner = 68.dp
    Box(
        modifier = Modifier.size(248.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(BorderStroke(3.dp, ringBrush), CircleShape)
        )
        ArrowAt(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
            enabled = controlsEnabled,
            vector = Icons.Filled.KeyboardArrowUp
        ) {
            when (backend) {
                TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_DPAD_UP)
                TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.UP)
            }
        }
        ArrowAt(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            enabled = controlsEnabled,
            vector = Icons.Filled.KeyboardArrowDown
        ) {
            when (backend) {
                TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_DPAD_DOWN)
                TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.DOWN)
            }
        }
        ArrowAt(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp),
            enabled = controlsEnabled,
            vector = Icons.AutoMirrored.Filled.KeyboardArrowLeft
        ) {
            when (backend) {
                TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_DPAD_LEFT)
                TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.LEFT)
            }
        }
        ArrowAt(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
            enabled = controlsEnabled,
            vector = Icons.AutoMirrored.Filled.KeyboardArrowRight
        ) {
            when (backend) {
                TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_DPAD_RIGHT)
                TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.RIGHT)
            }
        }
        Box(
            modifier = Modifier
                .size(inner)
                .border(BorderStroke(3.dp, ringBrush), CircleShape)
                .clickable(
                    enabled = controlsEnabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    when (backend) {
                        TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_DPAD_CENTER)
                        TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.ENTER)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.RadioButtonUnchecked,
                contentDescription = stringResource(R.string.cd_ok),
                tint = MandoIconWhite.copy(alpha = 0.28f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ArrowAt(
    modifier: Modifier,
    enabled: Boolean,
    vector: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(vector, contentDescription = null, tint = MandoIconWhite, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun PageDots(
    padPage: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (padPage == 0) MandoIconWhite else Color(0xFF444444))
                .clickable { onSelect(0) }
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (padPage == 1) MandoIconWhite else Color(0xFF444444))
                .clickable { onSelect(1) }
        )
    }
}

@Composable
private fun BottomControlRow(
    modifier: Modifier = Modifier,
    backend: TvBackend,
    controlsEnabled: Boolean,
    onAndroidKey: (RemoteKeyCode) -> Unit,
    onSamsungKey: (SamsungKey) -> Unit,
    onOpenNumpadPage: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VolumePill(
            enabled = controlsEnabled,
            onVolUp = {
                when (backend) {
                    TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_VOLUME_UP)
                    TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.VOL_UP)
                }
            },
            onVolDown = {
                when (backend) {
                    TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_VOLUME_DOWN)
                    TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.VOL_DOWN)
                }
            }
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundPadButton(
                    icon = Icons.Filled.Home,
                    contentDescription = stringResource(R.string.cd_home),
                    enabled = controlsEnabled,
                    onClick = {
                        when (backend) {
                            TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_HOME)
                            TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.HOME)
                        }
                    }
                )
                RoundPadButton(
                    icon = Icons.Filled.Dialpad,
                    contentDescription = stringResource(R.string.cd_numpad),
                    enabled = controlsEnabled,
                    onClick = onOpenNumpadPage
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundPadButton(
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(R.string.cd_mute),
                    enabled = controlsEnabled,
                    onClick = {
                        when (backend) {
                            TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_VOLUME_MUTE)
                            TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.MUTE)
                        }
                    }
                )
                RoundPadButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    enabled = controlsEnabled,
                    onClick = {
                        when (backend) {
                            TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_BACK)
                            TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.BACK)
                        }
                    }
                )
            }
        }
        ChannelPill(
            enabled = controlsEnabled,
            onChUp = {
                when (backend) {
                    TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_CHANNEL_UP)
                    TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.CH_UP)
                }
            },
            onChDown = {
                when (backend) {
                    TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_CHANNEL_DOWN)
                    TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.CH_DOWN)
                }
            }
        )
    }
}

@Composable
private fun VolumePill(
    enabled: Boolean,
    onVolUp: () -> Unit,
    onVolDown: () -> Unit
) {
    // No mostramos 0–100: el protocolo remoto no devuelve el nivel real de la TV (cada marca escala distinto).
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(168.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MandoSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onVolUp,
                enabled = enabled,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_vol_up), tint = MandoIconWhite)
            }
            Text(
                text = stringResource(R.string.label_vol),
                color = MandoIconWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = onVolDown,
                enabled = enabled,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.cd_vol_down), tint = MandoIconWhite)
            }
        }
    }
}

@Composable
private fun ChannelPill(
    enabled: Boolean,
    onChUp: () -> Unit,
    onChDown: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .height(168.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MandoSurface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onChUp,
            enabled = enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.cd_ch_up), tint = MandoIconWhite)
        }
        Text(
            text = stringResource(R.string.label_ch),
            color = MandoIconWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(
            onClick = onChDown,
            enabled = enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_ch_down), tint = MandoIconWhite)
        }
    }
}

@Composable
private fun RoundPadButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MandoSurface)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MandoIconWhite,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun NumericKeypadPage(
    backend: TvBackend,
    controlsEnabled: Boolean,
    onAndroidKey: (RemoteKeyCode) -> Unit,
    onSamsungKey: (SamsungKey) -> Unit
) {
    val row1 = listOf(
        Triple("1", RemoteKeyCode.KEYCODE_1, SamsungKey.DIGIT_1),
        Triple("2", RemoteKeyCode.KEYCODE_2, SamsungKey.DIGIT_2),
        Triple("3", RemoteKeyCode.KEYCODE_3, SamsungKey.DIGIT_3)
    )
    val row2 = listOf(
        Triple("4", RemoteKeyCode.KEYCODE_4, SamsungKey.DIGIT_4),
        Triple("5", RemoteKeyCode.KEYCODE_5, SamsungKey.DIGIT_5),
        Triple("6", RemoteKeyCode.KEYCODE_6, SamsungKey.DIGIT_6)
    )
    val row3 = listOf(
        Triple("7", RemoteKeyCode.KEYCODE_7, SamsungKey.DIGIT_7),
        Triple("8", RemoteKeyCode.KEYCODE_8, SamsungKey.DIGIT_8),
        Triple("9", RemoteKeyCode.KEYCODE_9, SamsungKey.DIGIT_9)
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(row1, row2, row3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, atv, sam) ->
                    DigitCircleButton(
                        label = label,
                        enabled = controlsEnabled,
                        onClick = {
                            when (backend) {
                                TvBackend.ANDROID_TV -> onAndroidKey(atv)
                                TvBackend.SAMSUNG -> onSamsungKey(sam)
                            }
                        }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(modifier = Modifier.size(52.dp))
            DigitCircleButton(
                label = "0",
                enabled = controlsEnabled,
                onClick = {
                    when (backend) {
                        TvBackend.ANDROID_TV -> onAndroidKey(RemoteKeyCode.KEYCODE_0)
                        TvBackend.SAMSUNG -> onSamsungKey(SamsungKey.DIGIT_0)
                    }
                }
            )
            Spacer(modifier = Modifier.size(52.dp))
        }
    }
}

@Composable
private fun DigitCircleButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MandoSurface)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MandoIconWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
