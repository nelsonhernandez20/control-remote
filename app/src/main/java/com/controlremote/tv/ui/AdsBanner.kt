package com.controlremote.tv.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * IDs de prueba oficiales AdMob (desarrollo). En producción, crea unidades por formato en AdMob.
 * Banner estándar (320×50 aprox.)
 */
private const val AD_UNIT_BANNER = "ca-app-pub-3940256099942544/6300978111"

/**
 * Rectángulo mediano 300×250 (MREC). Misma app de demo de Google; en producción usa una unidad MREC.
 */
private const val AD_UNIT_MEDIUM_RECT = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun AdsBanner(modifier: Modifier = Modifier) {
    AdViewBanner(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        adSize = AdSize.BANNER,
        adUnitId = AD_UNIT_BANNER
    )
}

/**
 * Rectángulo mediano para incrustar en tarjetas (p. ej. pestaña Estado).
 */
@Composable
fun AdsMediumRectangle(modifier: Modifier = Modifier) {
    AdViewBanner(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        adSize = AdSize.MEDIUM_RECTANGLE,
        adUnitId = AD_UNIT_MEDIUM_RECT
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
