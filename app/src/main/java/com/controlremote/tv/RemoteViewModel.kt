package com.controlremote.tv

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlremote.androidtv.proto.RemoteKeyCode
import com.controlremote.tv.androidtv.AndroidTvMdnsDiscovery
import com.controlremote.tv.androidtv.AndroidTvSsl
import com.controlremote.tv.androidtv.DiscoveredTv
import com.controlremote.tv.androidtv.PairingSession
import com.controlremote.tv.androidtv.RemoteSessionHolder
import com.controlremote.tv.androidtv.RemoteSessionState
import com.controlremote.tv.androidtv.SamsungPortScanner
import com.controlremote.tv.androidtv.SelfSignedCertGenerator
import com.controlremote.tv.androidtv.TvRemoteForegroundService
import com.controlremote.tv.androidtv.DeviceOrderStore
import com.controlremote.tv.androidtv.SamsungKnownStore
import com.controlremote.tv.androidtv.TvCredentialsStore
import com.controlremote.tv.remote.SamsungKey
import com.controlremote.tv.remote.SamsungRemoteClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TvBackend { SAMSUNG, ANDROID_TV }

enum class AndroidTvConnectionPhase {
    IDLE,
    PAIRING_NEED_PIN,
    PAIRED,
    REMOTE_CONNECTED,
    ERROR
}

data class KnownDeviceInfo(
    val host: String,
    val backend: TvBackend,
    val title: String,
    /** IP u otro identificador secundario */
    val subtitle: String
)

data class RemoteUiState(
    val backend: TvBackend = TvBackend.ANDROID_TV,
    val tvIp: String = "",
    /** Nombre mostrado (mDNS, protocolo remoto o lista); si es null se usa [tvIp]. */
    val tvDisplayName: String? = null,
    val lastMessage: String? = null,
    val isBusy: Boolean = false,
    val androidTvPhase: AndroidTvConnectionPhase = AndroidTvConnectionPhase.IDLE,
    val androidTvPin: String = "",
    val discoveredDevices: List<DiscoveredTv> = emptyList(),
    val isScanningLan: Boolean = false,
    val knownDevices: List<KnownDeviceInfo> = emptyList()
)

class RemoteViewModel(
    application: Application,
    private val samsungClient: SamsungRemoteClient = SamsungRemoteClient()
) : AndroidViewModel(application) {

    private fun str(@StringRes id: Int) = getApplication<Application>().getString(id)
    private fun str(@StringRes id: Int, vararg args: Any) =
        getApplication<Application>().getString(id, *args)

    private val credentials = TvCredentialsStore(application)
    private val samsungKnown = SamsungKnownStore(application)
    private val deviceOrder = DeviceOrderStore(application)
    private var pairing: PairingSession? = null
    private var mdnsDiscovery: AndroidTvMdnsDiscovery? = null
    private var mdnsAutoStopJob: Job? = null
    private var samsungScanJob: Job? = null

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private val _requestPostNotificationPermission = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestPostNotificationPermission: SharedFlow<Unit> = _requestPostNotificationPermission.asSharedFlow()

    init {
        refreshKnownDevices()
        viewModelScope.launch {
            RemoteSessionHolder.state.collect { st ->
                when (st) {
                    RemoteSessionState.Disconnected -> {
                        _uiState.update { current ->
                            if (current.backend != TvBackend.ANDROID_TV) {
                                return@update current.copy(isBusy = false)
                            }
                            val ip = current.tvIp.trim()
                            val next = when {
                                ip.isEmpty() -> AndroidTvConnectionPhase.IDLE
                                credentials.hasCredentials(ip) -> AndroidTvConnectionPhase.PAIRED
                                else -> AndroidTvConnectionPhase.IDLE
                            }
                            current.copy(androidTvPhase = next, isBusy = false)
                        }
                    }
                    is RemoteSessionState.Connecting -> {
                        _uiState.update {
                            it.copy(
                                isBusy = true,
                                tvIp = st.host,
                                backend = TvBackend.ANDROID_TV
                            )
                        }
                    }
                    is RemoteSessionState.Connected -> {
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                tvIp = st.host,
                                backend = TvBackend.ANDROID_TV,
                                androidTvPhase = AndroidTvConnectionPhase.REMOTE_CONNECTED,
                                lastMessage = str(R.string.msg_connected_android_tv)
                            )
                        }
                        syncTvDisplayName()
                        refreshKnownDevices()
                    }
                    is RemoteSessionState.Error -> {
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                androidTvPhase = AndroidTvConnectionPhase.ERROR,
                                lastMessage = st.message
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            RemoteSessionHolder.refreshNeeded.collect {
                refreshKnownDevices()
                syncTvDisplayName()
            }
        }
    }

    private fun deviceKeyFor(info: KnownDeviceInfo): String = when (info.backend) {
        TvBackend.ANDROID_TV -> DeviceOrderStore.keyAndroid(info.host)
        TvBackend.SAMSUNG -> DeviceOrderStore.keySamsung(info.host)
    }

    /** TVs emparejadas (Android TV) o usadas antes (Samsung), orden persistido. */
    fun refreshKnownDevices() {
        val androidHosts = credentials.listPairedHosts()
        val samsungHosts = samsungKnown.listHosts()
        val map = LinkedHashMap<String, KnownDeviceInfo>()
        for (h in androidHosts) {
            val title = credentials.getDisplayName(h) ?: h
            map[DeviceOrderStore.keyAndroid(h)] =
                KnownDeviceInfo(h, TvBackend.ANDROID_TV, title, h)
        }
        for (h in samsungHosts) {
            if (androidHosts.contains(h)) continue
            val title = samsungKnown.getDisplayName(h) ?: h
            map[DeviceOrderStore.keySamsung(h)] =
                KnownDeviceInfo(h, TvBackend.SAMSUNG, title, h)
        }
        val order = deviceOrder.getOrder()
        val ordered = mutableListOf<KnownDeviceInfo>()
        val used = mutableSetOf<String>()
        for (key in order) {
            map[key]?.let {
                ordered.add(it)
                used.add(key)
            }
        }
        val leftovers = map.filterKeys { it !in used }.values.sortedWith(
            compareBy({ it.backend.ordinal }, { it.title.lowercase() })
        )
        ordered.addAll(leftovers)
        for (info in ordered) {
            deviceOrder.appendIfMissing(deviceKeyFor(info))
        }
        _uiState.update { it.copy(knownDevices = ordered) }
        syncTvDisplayName()
    }

    fun moveKnownDevice(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.knownDevices.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        deviceOrder.setOrder(list.map { deviceKeyFor(it) })
        _uiState.update { it.copy(knownDevices = list) }
    }

    fun hasAndroidTvCredentials(host: String): Boolean =
        credentials.hasCredentials(host.trim())

    private fun syncTvDisplayName() {
        val ip = _uiState.value.tvIp.trim()
        if (ip.isEmpty()) {
            _uiState.update { it.copy(tvDisplayName = null) }
            return
        }
        val name = when (_uiState.value.backend) {
            TvBackend.ANDROID_TV -> credentials.getDisplayName(ip)
            TvBackend.SAMSUNG -> samsungKnown.getDisplayName(ip)
        }
        _uiState.update { it.copy(tvDisplayName = name) }
    }

    fun selectKnownDevice(device: KnownDeviceInfo) {
        AppLog.d("Dispositivo conocido: ${device.host} (${device.backend})")
        _uiState.update {
            it.copy(
                backend = device.backend,
                tvIp = device.host,
                tvDisplayName = device.title,
                lastMessage = str(R.string.msg_selected, device.title)
            )
        }
    }

    /**
     * Conecta el control a una TV ya conocida (botón «Conectar»).
     */
    fun connectKnownDevice(device: KnownDeviceInfo) {
        if (device.backend == TvBackend.ANDROID_TV) {
            if (_uiState.value.androidTvPhase == AndroidTvConnectionPhase.PAIRING_NEED_PIN) {
                selectKnownDevice(device)
                return
            }
            val st = _uiState.value
            if (st.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED &&
                st.tvIp.trim() == device.host.trim()
            ) {
                return
            }
            if (st.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED &&
                st.tvIp.trim() != device.host.trim()
            ) {
                androidTvDisconnect()
            }
        }
        selectKnownDevice(device)
        if (device.backend == TvBackend.SAMSUNG) return
        if (!credentials.hasCredentials(device.host)) return
        val st2 = _uiState.value
        if (st2.isBusy) return
        if (st2.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED &&
            st2.tvIp.trim() == device.host.trim()
        ) {
            return
        }
        androidTvConnectRemote()
    }

    fun disconnectKnownDevice(device: KnownDeviceInfo) {
        when (device.backend) {
            TvBackend.ANDROID_TV -> {
                if (_uiState.value.tvIp.trim() != device.host.trim()) return
                if (_uiState.value.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED) {
                    androidTvDisconnect()
                }
            }
            TvBackend.SAMSUNG -> {
                if (_uiState.value.tvIp.trim() == device.host.trim() &&
                    _uiState.value.backend == TvBackend.SAMSUNG
                ) {
                    _uiState.update {
                        it.copy(tvIp = "", tvDisplayName = null, lastMessage = str(R.string.msg_tv_deselected))
                    }
                }
            }
        }
    }

    fun forgetKnownDevice(device: KnownDeviceInfo) {
        when (device.backend) {
            TvBackend.ANDROID_TV -> androidTvForgetForHost(device.host)
            TvBackend.SAMSUNG -> forgetSamsungHost(device.host)
        }
    }

    private fun forgetSamsungHost(host: String) {
        val h = host.trim()
        if (h.isEmpty()) return
        samsungKnown.removeHost(h)
        deviceOrder.removeKey(DeviceOrderStore.keySamsung(h))
        if (_uiState.value.tvIp.trim() == h) {
            _uiState.update {
                it.copy(tvIp = "", tvDisplayName = null, lastMessage = str(R.string.msg_tv_removed))
            }
        }
        refreshKnownDevices()
    }

    /**
     * Cierra la sesión de emparejamiento (PIN) sin borrar credenciales, p. ej. al cerrar el modal.
     */
    fun cancelAndroidTvPairing() {
        pairing?.close()
        pairing = null
        val st = _uiState.value
        if (st.androidTvPhase == AndroidTvConnectionPhase.REMOTE_CONNECTED) {
            _uiState.update { it.copy(androidTvPin = "") }
            return
        }
        val ip = st.tvIp.trim()
        val next = when {
            ip.isEmpty() -> AndroidTvConnectionPhase.IDLE
            credentials.hasCredentials(ip) -> AndroidTvConnectionPhase.PAIRED
            else -> AndroidTvConnectionPhase.IDLE
        }
        _uiState.update { it.copy(androidTvPhase = next, androidTvPin = "") }
    }

    fun setBackend(backend: TvBackend) {
        val previous = _uiState.value.backend
        if (backend == TvBackend.SAMSUNG && previous == TvBackend.ANDROID_TV) {
            TvRemoteForegroundService.disconnect(getApplication())
        }
        _uiState.update {
            it.copy(
                backend = backend,
                lastMessage = null,
                discoveredDevices = emptyList()
            )
        }
        stopAllLanScans()
        syncTvDisplayName()
    }

    fun setTvIp(value: String) {
        _uiState.update { it.copy(tvIp = value, lastMessage = null) }
        syncTvDisplayName()
    }

    fun setAndroidTvPin(value: String) {
        val filtered = value.filter { c ->
            c.isDigit() || c in 'a'..'f' || c in 'A'..'F'
        }.take(6).lowercase()
        _uiState.update { it.copy(androidTvPin = filtered) }
    }

    /** mDNS: _androidtvremote2._tcp y _androidtvremote._tcp (≈20 s). */
    fun startAndroidTvLanDiscovery() {
        AppLog.d("Buscar Android TV (mDNS)…")
        stopAllLanScans()
        _uiState.update {
            it.copy(
                discoveredDevices = emptyList(),
                isScanningLan = true,
                lastMessage = str(R.string.msg_scanning_android_tv)
            )
        }
        val disc = AndroidTvMdnsDiscovery(getApplication()) { tv ->
            addDiscoveredUnique(tv)
        }
        mdnsDiscovery = disc
        disc.start()
        mdnsAutoStopJob = viewModelScope.launch {
            delay(22_000)
            stopAndroidTvMdnsOnly()
            _uiState.update { s ->
                val base = s.lastMessage?.trim().orEmpty()
                val suffix = str(R.string.msg_scan_android_tv_finished)
                s.copy(
                    lastMessage = if (base.isEmpty()) suffix else "$base · $suffix"
                )
            }
        }
    }

    private fun addDiscoveredUnique(tv: DiscoveredTv) {
        AppLog.d("Dispositivo: ${tv.name} ${tv.host}:${tv.port} (${tv.source})")
        _uiState.update { state ->
            if (state.discoveredDevices.any { it.host == tv.host && it.source == tv.source }) {
                state
            } else {
                state.copy(
                    discoveredDevices = (state.discoveredDevices + tv).sortedBy { it.name }
                )
            }
        }
    }

    private fun stopAndroidTvMdnsOnly() {
        mdnsAutoStopJob?.cancel()
        mdnsAutoStopJob = null
        mdnsDiscovery?.stop()
        mdnsDiscovery = null
        _uiState.update { it.copy(isScanningLan = samsungScanJob?.isActive == true) }
    }

    /** Escaneo del /24 actual en busca del puerto 8001 (Samsung). */
    fun startSamsungLanScan() {
        AppLog.d("Buscar Samsung (puerto 8001)…")
        stopAllLanScans()
        val prefix = SamsungPortScanner.localSubnetPrefix()
        if (prefix == null) {
            AppLog.e("Sin prefijo de subred IPv4 (¿Wi‑Fi desactivado?)")
            _uiState.update {
                it.copy(lastMessage = str(R.string.msg_no_ipv4))
            }
            return
        }
        AppLog.d("Escaneando subred $prefix.0/24 …")
        _uiState.update {
            it.copy(
                discoveredDevices = emptyList(),
                isScanningLan = true,
                lastMessage = str(R.string.msg_scanning_samsung)
            )
        }
        samsungScanJob = viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { SamsungPortScanner.scan(getApplication()) }
            AppLog.d("Escaneo Samsung terminado: ${found.size} host(s) con :8001")
            _uiState.update {
                it.copy(
                    isScanningLan = false,
                    discoveredDevices = found.sortedBy { it.host },
                    lastMessage = when {
                        found.isEmpty() -> str(R.string.msg_samsung_no_devices)
                        else -> str(R.string.msg_samsung_found_count, found.size)
                    }
                )
            }
        }
    }

    fun stopAllLanScans() {
        mdnsAutoStopJob?.cancel()
        mdnsAutoStopJob = null
        samsungScanJob?.cancel()
        samsungScanJob = null
        mdnsDiscovery?.stop()
        mdnsDiscovery = null
        _uiState.update { it.copy(isScanningLan = false) }
    }

    fun selectDiscoveredDevice(tv: DiscoveredTv) {
        AppLog.d("IP seleccionada: ${tv.host} (${tv.name})")
        when (_uiState.value.backend) {
            TvBackend.ANDROID_TV -> {
                credentials.setDisplayName(tv.host, tv.name)
            }
            TvBackend.SAMSUNG -> {
                samsungKnown.rememberHost(tv.host)
                samsungKnown.setDisplayName(tv.host, tv.name)
                deviceOrder.appendIfMissing(DeviceOrderStore.keySamsung(tv.host))
            }
        }
        _uiState.update {
            it.copy(
                tvIp = tv.host,
                lastMessage = str(R.string.msg_selected_with_ip, tv.name, tv.host)
            )
        }
        refreshKnownDevices()
    }

    fun clearDiscoveredList() {
        _uiState.update { it.copy(discoveredDevices = emptyList()) }
    }

    fun sendKeySamsung(key: SamsungKey) {
        val ip = _uiState.value.tvIp.trim()
        if (ip.isEmpty()) {
            _uiState.update { it.copy(lastMessage = str(R.string.msg_enter_ip_samsung)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastMessage = null) }
            val result = samsungClient.sendKey(ip, key)
            if (result.isSuccess) {
                samsungKnown.rememberHost(ip)
                if (samsungKnown.getDisplayName(ip) == null) {
                    samsungKnown.setDisplayName(ip, str(R.string.samsung_tv_default_name))
                }
                refreshKnownDevices()
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    lastMessage = result.fold(
                        onSuccess = { str(R.string.msg_samsung_sent, key.name) },
                        onFailure = { e ->
                            str(R.string.msg_error_with_reason, e.message ?: str(R.string.msg_error_unknown))
                        }
                    )
                )
            }
        }
    }

    fun sendKeyAndroidTv(key: RemoteKeyCode) {
        val session = RemoteSessionHolder.getSession()
        if (session == null) {
            _uiState.update { it.copy(lastMessage = str(R.string.msg_connect_android_first)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastMessage = null) }
            val result = runCatching { withContext(Dispatchers.IO) { session.sendKey(key) } }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    lastMessage = result.fold(
                        onSuccess = { str(R.string.msg_sent_android_tv) },
                        onFailure = { e ->
                            str(R.string.msg_error_with_reason, e.message ?: str(R.string.msg_error_unknown))
                        }
                    )
                )
            }
        }
    }

    fun androidTvStartPairing() {
        val ip = _uiState.value.tvIp.trim()
        if (ip.isEmpty()) {
            _uiState.update { it.copy(lastMessage = str(R.string.msg_enter_ip_pairing)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastMessage = null) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (!credentials.hasCredentials(ip)) {
                        val (cert, key) = SelfSignedCertGenerator.generatePem("control-remote-mobile")
                        credentials.savePem(ip, cert, key)
                    }
                    val ssl = AndroidTvSsl.createClientContext(
                        credentials.getCertPem(ip)!!,
                        credentials.getKeyPem(ip)!!
                    )
                    pairing?.close()
                    val p = PairingSession(ip, ssl, "Control remoto TV")
                    pairing = p
                    p.connect()
                    p.startPairing()
                }.onFailure {
                    pairing?.close()
                    pairing = null
                }
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    androidTvPhase = if (result.isSuccess) {
                        AndroidTvConnectionPhase.PAIRING_NEED_PIN
                    } else {
                        AndroidTvConnectionPhase.ERROR
                    },
                    lastMessage = result.exceptionOrNull()?.message
                        ?: str(R.string.msg_pair_pin_hint)
                )
            }
            if (result.isSuccess) refreshKnownDevices()
        }
    }

    fun androidTvFinishPairing() {
        val pin = _uiState.value.androidTvPin
        if (pin.length != 6) {
            _uiState.update { it.copy(lastMessage = str(R.string.msg_pin_invalid)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastMessage = null) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val p = pairing ?: error("Emparejamiento no iniciado")
                    p.finishPairing(pin)
                    p.close()
                    pairing = null
                }.onFailure {
                    pairing?.close()
                    pairing = null
                }
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    androidTvPhase = if (result.isSuccess) {
                        AndroidTvConnectionPhase.PAIRED
                    } else {
                        AndroidTvConnectionPhase.ERROR
                    },
                    lastMessage = result.exceptionOrNull()?.message
                        ?: str(R.string.msg_pairing_ok_connect)
                )
            }
            if (result.isSuccess) refreshKnownDevices()
        }
    }

    fun androidTvConnectRemote() {
        val ip = _uiState.value.tvIp.trim()
        if (ip.isEmpty()) {
            _uiState.update { it.copy(lastMessage = str(R.string.msg_enter_ip)) }
            return
        }
        if (!credentials.hasCredentials(ip)) {
            _uiState.update { it.copy(lastMessage = str(R.string.msg_pair_first)) }
            return
        }
        if (RemoteSessionHolder.getConnectedHost() == ip) {
            return
        }
        if (needsPostNotificationPermission()) {
            _requestPostNotificationPermission.tryEmit(Unit)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastMessage = null) }
            RemoteSessionHolder.setConnecting(ip)
            TvRemoteForegroundService.connect(getApplication(), ip)
        }
    }

    fun onPostNotificationPermissionResult(granted: Boolean) {
        if (granted) {
            androidTvConnectRemote()
        } else {
            _uiState.update {
                it.copy(lastMessage = str(R.string.msg_notification_denied), isBusy = false)
            }
        }
    }

    private fun needsPostNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun androidTvDisconnect() {
        TvRemoteForegroundService.disconnect(getApplication())
        pairing?.close()
        pairing = null
        val ip = _uiState.value.tvIp.trim()
        val next = if (ip.isNotEmpty() && credentials.hasCredentials(ip)) {
            AndroidTvConnectionPhase.PAIRED
        } else {
            AndroidTvConnectionPhase.IDLE
        }
        _uiState.update {
            it.copy(androidTvPhase = next, lastMessage = str(R.string.msg_remote_closed))
        }
    }

    fun androidTvForgetDevice() {
        androidTvForgetForHost(_uiState.value.tvIp.trim())
    }

    fun androidTvForgetForHost(host: String) {
        val h = host.trim()
        if (h.isEmpty()) return
        if (_uiState.value.tvIp.trim() == h) {
            TvRemoteForegroundService.disconnect(getApplication())
            pairing?.close()
            pairing = null
        }
        credentials.clear(h)
        deviceOrder.removeKey(DeviceOrderStore.keyAndroid(h))
        _uiState.update {
            val active = it.tvIp.trim() == h
            if (active) {
                it.copy(
                    tvIp = "",
                    androidTvPhase = AndroidTvConnectionPhase.IDLE,
                    androidTvPin = "",
                    tvDisplayName = null,
                    lastMessage = str(R.string.msg_tv_removed)
                )
            } else {
                it.copy(lastMessage = str(R.string.msg_tv_removed))
            }
        }
        refreshKnownDevices()
    }

    override fun onCleared() {
        super.onCleared()
        stopAllLanScans()
        pairing?.close()
    }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RemoteViewModel(application) as T
            }
        }
    }
}
