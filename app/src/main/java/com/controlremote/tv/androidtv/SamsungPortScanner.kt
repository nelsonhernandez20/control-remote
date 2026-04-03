package com.controlremote.tv.androidtv

import android.content.Context
import com.controlremote.tv.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Busca TVs Samsung que expongan el puerto HTTP 8001 en el mismo /24 que el Wi‑Fi del teléfono.
 */
object SamsungPortScanner {

    private const val SAMSUNG_PORT = 8001
    private const val TIMEOUT_MS = 280
    private const val PARALLEL = 36

    fun localSubnetPrefix(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces().toList()
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            for (addr in intf.inetAddresses) {
                if (addr !is Inet4Address || addr.isLoopbackAddress) continue
                val host = addr.hostAddress ?: continue
                val parts = host.split('.')
                if (parts.size != 4) continue
                return "${parts[0]}.${parts[1]}.${parts[2]}"
            }
        }
        return null
    }

    suspend fun scan(context: Context): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        val prefix = localSubnetPrefix() ?: return@withContext emptyList()
        val app = context.applicationContext
        coroutineScope {
            val sem = Semaphore(PARALLEL)
            (1..254).map { last ->
                async {
                    sem.withPermit {
                        val ip = "$prefix.$last"
                        if (portOpen(ip)) {
                            DiscoveredTv(
                                name = app.getString(R.string.discovered_samsung_http, ip),
                                host = ip,
                                port = SAMSUNG_PORT,
                                source = DiscoverySource.SAMSUNG_HTTP_SCAN
                            )
                        } else {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun portOpen(host: String): Boolean {
        val s = Socket()
        return try {
            s.connect(InetSocketAddress(host, SAMSUNG_PORT), TIMEOUT_MS)
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { s.close() }
        }
    }
}
