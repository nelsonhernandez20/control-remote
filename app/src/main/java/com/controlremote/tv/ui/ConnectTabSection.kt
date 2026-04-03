package com.controlremote.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.controlremote.tv.AndroidTvConnectionPhase
import com.controlremote.tv.R
import com.controlremote.tv.RemoteUiState
import com.controlremote.tv.TvBackend
import com.controlremote.tv.ui.theme.DeviceListCard
import com.controlremote.tv.ui.theme.MandoOnlineGreen
import com.controlremote.tv.ui.theme.SurfaceCard
import com.controlremote.tv.ui.theme.TextMuted

/**
 * Pestaña Estado: resumen en tarjeta gris, «Conectado» en verde; nota Samsung y ayuda Android TV.
 */
@Composable
fun ConnectTabSection(state: RemoteUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConnectionStatusBanner(state)
        when (state.backend) {
            TvBackend.ANDROID_TV -> AndroidTvConnectHintCard()
            TvBackend.SAMSUNG -> SamsungConnectInfoCard(state)
        }
        StatusTabAdCard()
    }
}

@Composable
private fun StatusTabAdCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AdsMediumRectangle(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@Composable
private fun ConnectionStatusBanner(state: RemoteUiState) {
    val emDash = stringResource(R.string.em_dash)
    val (statusLabel, statusGreen, detail) = when (state.backend) {
        TvBackend.SAMSUNG -> {
            val ready = state.tvIp.isNotBlank()
            Triple(
                if (ready) stringResource(R.string.status_samsung_ready) else stringResource(R.string.status_samsung_no_ip),
                ready,
                state.tvDisplayName?.takeIf { it.isNotBlank() } ?: state.tvIp.ifBlank { emDash }
            )
        }
        TvBackend.ANDROID_TV -> {
            val label = when (state.androidTvPhase) {
                AndroidTvConnectionPhase.REMOTE_CONNECTED -> stringResource(R.string.status_atv_connected)
                AndroidTvConnectionPhase.PAIRING_NEED_PIN -> stringResource(R.string.status_atv_waiting_pin)
                AndroidTvConnectionPhase.PAIRED -> stringResource(R.string.status_atv_paired_no_session)
                AndroidTvConnectionPhase.IDLE -> stringResource(R.string.status_atv_disconnected)
                AndroidTvConnectionPhase.ERROR -> stringResource(R.string.status_atv_error)
            }
            val green = state.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED
            val sub = state.tvDisplayName?.takeIf { it.isNotBlank() } ?: state.tvIp.ifBlank { emDash }
            Triple(label, green, sub)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DeviceListCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.status_section_title),
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (statusGreen) MandoOnlineGreen else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SamsungConnectInfoCard(state: RemoteUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeviceListCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.samsung_hint_no_tls),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun AndroidTvConnectHintCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeviceListCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.atv_hint_pairing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )
    }
}
