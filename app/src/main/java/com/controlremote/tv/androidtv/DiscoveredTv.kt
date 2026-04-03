package com.controlremote.tv.androidtv

data class DiscoveredTv(
    val name: String,
    val host: String,
    val port: Int,
    val source: DiscoverySource
)

enum class DiscoverySource {
    ANDROID_TV_MDNS,
    SAMSUNG_HTTP_SCAN
}
