package com.controlremote.tv.ui

import android.content.Intent
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.controlremote.tv.AppLocale
import com.controlremote.tv.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.controlremote.tv.AndroidTvConnectionPhase
import com.controlremote.tv.RemoteUiState
import com.controlremote.tv.RemoteViewModel
import com.controlremote.tv.TvBackend
import com.controlremote.tv.ui.theme.BackgroundDark
import com.controlremote.tv.ui.theme.KeyPurple
import com.controlremote.tv.ui.theme.KeyPurplePressed
import com.controlremote.tv.ui.theme.SurfaceCard
import com.controlremote.tv.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onLocaleSelected: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val postNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPostNotificationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.requestPostNotificationPermission.collect {
            postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(state.backend) {
        selectedTab = 0
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { languageMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = stringResource(R.string.language),
                                tint = KeyPurplePressed
                            )
                        }
                        DropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = { languageMenuExpanded = false }
                        ) {
                            val current = AppLocale.currentTag(context)
                            fun select(tag: String) {
                                languageMenuExpanded = false
                                if (tag != current) onLocaleSelected(tag)
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language_system)) },
                                onClick = { select(AppLocale.SYSTEM) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language_english)) },
                                onClick = { select(AppLocale.EN) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language_spanish)) },
                                onClick = { select(AppLocale.ES) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language_french)) },
                                onClick = { select(AppLocale.FR) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language_portuguese)) },
                                onClick = { select(AppLocale.PT) }
                            )
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
                    text = { Text(stringResource(R.string.tab_device)) },
                    selectedContentColor = KeyPurplePressed,
                    unselectedContentColor = TextMuted
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_status)) },
                    selectedContentColor = KeyPurplePressed,
                    unselectedContentColor = TextMuted
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.tab_remote)) },
                    selectedContentColor = KeyPurplePressed,
                    unselectedContentColor = TextMuted
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark)
            ) {
                AdsBanner()
            }

            when (selectedTab) {
                2 -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        PadTabContent(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel,
                            state = state,
                            onOpenDeviceTab = { selectedTab = 0 }
                        )
                    }
                }
                0 -> {
                    // Un solo scroll aquí: si el padre también usa verticalScroll y el formulario
                    // «añadir» tiene otro scroll, Compose puede crashear al abrir «Buscar nuevo dispositivo».
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp, bottom = 12.dp)
                    ) {
                        DeviceTabSection(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel,
                            state = state,
                            onNavigateToConnectTab = { selectedTab = 1 }
                        )
                    }
                }
                1 -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ConnectTabSection(state = state)
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(BackgroundDark)
            ) {
                AdsBanner()
                TextButton(
                    onClick = {
                        val url = context.getString(R.string.privacy_policy_url)
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.legal_privacy),
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun PadTabContent(
    modifier: Modifier = Modifier,
    viewModel: RemoteViewModel,
    state: RemoteUiState,
    onOpenDeviceTab: () -> Unit
) {
    when (state.backend) {
        TvBackend.ANDROID_TV -> {
            if (state.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED) {
                ModernRemotePad(
                    modifier = modifier,
                    backend = TvBackend.ANDROID_TV,
                    controlsEnabled = !state.isBusy,
                    tvIpLabel = state.tvDisplayName?.takeIf { it.isNotBlank() } ?: state.tvIp,
                    onAndroidKey = { viewModel.sendKeyAndroidTv(it) },
                    onSamsungKey = { },
                    onOpenDeviceTab = onOpenDeviceTab
                )
            } else {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pad_connect_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
        TvBackend.SAMSUNG -> {
            ModernRemotePad(
                modifier = modifier,
                backend = TvBackend.SAMSUNG,
                controlsEnabled = state.tvIp.isNotBlank() && !state.isBusy,
                tvIpLabel = state.tvDisplayName?.takeIf { it.isNotBlank() } ?: state.tvIp,
                onAndroidKey = { },
                onSamsungKey = { viewModel.sendKeySamsung(it) },
                onOpenDeviceTab = onOpenDeviceTab
            )
        }
    }
}

@Composable
internal fun RemoteSegmentButton(
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
internal fun RemoteKeyButton(
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
