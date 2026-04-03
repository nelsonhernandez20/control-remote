package com.controlremote.tv.androidtv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.controlremote.tv.AppLog
/**
 * Descubre Android TV / Google TV vía DNS-SD (mDNS): [_androidtvremote2._tcp] y [_androidtvremote._tcp].
 */
class AndroidTvMdnsDiscovery(
    private val context: Context,
    private val onDevice: (DiscoveredTv) -> Unit
) {
    private val appCtx = context.applicationContext
    private val nsdManager = appCtx.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val listeners = mutableMapOf<String, NsdManager.DiscoveryListener>()

    fun start() {
        stop()
        val types = listOf("_androidtvremote2._tcp", "_androidtvremote._tcp")
        AppLog.d("mDNS: buscando ${types.joinToString()}")
        for (type in types) {
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String?) {
                    AppLog.d("mDNS descubrimiento iniciado: $serviceType")
                }
                override fun onDiscoveryStopped(serviceType: String?) {}
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    AppLog.e("mDNS fallo al iniciar tipo=$serviceType code=$errorCode")
                }
                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    AppLog.d("mDNS servicio visto: ${serviceInfo.serviceName} (${serviceInfo.serviceType})")
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                            AppLog.e("mDNS resolve fallo code=$errorCode svc=${serviceInfo?.serviceName}")
                        }
                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            val host = resolved.host ?: return
                            val ip = host.hostAddress ?: return
                            if (ip.isBlank()) return
                            AppLog.d("mDNS resuelto: ${resolved.serviceName} -> $ip:${resolved.port}")
                            onDevice(
                                DiscoveredTv(
                                    name = resolved.serviceName?.takeIf { it.isNotBlank() } ?: ip,
                                    host = ip,
                                    port = resolved.port,
                                    source = DiscoverySource.ANDROID_TV_MDNS
                                )
                            )
                        }
                    })
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
            }
            listeners[type] = listener
            runCatching {
                nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            }
        }
    }

    fun stop() {
        val toStop = listeners.values.toList()
        listeners.clear()
        for (listener in toStop) {
            NsdHelper.stopDiscovery(nsdManager, listener)
        }
    }
}
