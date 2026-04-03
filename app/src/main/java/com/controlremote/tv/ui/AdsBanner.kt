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
 * Banner inferior. En producción sustituye [AD_UNIT_ID] por tu unidad en AdMob.
 * IDs de prueba oficiales de Google (solo desarrollo).
 */
private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun AdsBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
