package com.controlremote.tv.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.controlremote.tv.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdsBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val unitId = context.getString(R.string.admob_banner_unit_id)
    AdViewBanner(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        adSize = AdSize.BANNER,
        adUnitId = unitId
    )
}

/**
 * Rectángulo mediano para incrustar en tarjetas (p. ej. pestaña Estado).
 */
@Composable
fun AdsMediumRectangle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val unitId = context.getString(R.string.admob_medium_rect_unit_id)
    AdViewBanner(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        adSize = AdSize.MEDIUM_RECTANGLE,
        adUnitId = unitId
    )
}

@Composable
private fun AdViewBanner(
    modifier: Modifier,
    adSize: AdSize,
    adUnitId: String
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
