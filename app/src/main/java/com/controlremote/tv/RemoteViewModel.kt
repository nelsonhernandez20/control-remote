package com.controlremote.tv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlremote.androidtv.proto.RemoteKeyCode
import com.controlremote.tv.androidtv.AndroidTvMdnsDiscovery
import com.controlremote.tv.androidtv.AndroidTvSsl
import com.controlremote.tv.androidtv.DiscoveredTv
import com.controlremote.tv.androidtv.PairingSession
import com.controlremote.tv.androidtv.RemoteSession
import com.controlremote.tv.androidtv.SamsungPortScanner
import com.controlremote.tv.androidtv.SelfSignedCertGenerator
import com.controlremote.tv.androidtv.TvCredentialsStore
import com.controlremote.tv.remote.SamsungKey
import com.controlremote.tv.remote.SamsungRemoteClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

data class RemoteUiState(
    val backend: TvBackend = TvBackend.ANDROID_TV,
    val tvIp: String = "",
    val lastMessage: String? = null,
    val isBusy: Boolean = false,
    val androidTvPhase: AndroidTvConnectionPhase = AndroidTvConnectionPhase.IDLE,
    val androidTvPin: String = "",
    val discoveredDevices: List<DiscoveredTv> = emptyList(),
    val isScanningLan: Boolean = false
)

class RemoteViewModel(
    application: Application,
    private val samsungClient: SamsungRemoteClient = SamsungRemoteClient()
) : AndroidViewModel(application) {

    private val credentials = TvCredentialsStore(application)
    private var pairing: PairingSession? = null
    private var remote: RemoteSession? = null
    private var mdnsDiscovery: AndroidTvMdnsDiscovery? = null
    private var mdnsAutoStopJob: Job? = null
    private var samsungScanJob: Job? = null

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    fun setBackend(backend: TvBackend) {
        _uiState.update {
            it.copy(
                backend = backend,
                lastMessage = null,
                discoveredDevices = emptyList()
            )
        }
        stopAllLanScans()
    }

    fun setTvIp(value: String) {
        _uiState.update { it.copy(tvIp = value, lastMessage = null) }
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
                lastMessage = "Buscando Android TV / Google TV en la red…"
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
                val suffix = "Búsqueda Android TV finalizada."
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
                it.copy(lastMessage = "No se detectó IPv4 en Wi‑Fi. Prueba con IP manual.")
            }
            return
        }
        AppLog.d("Escaneando subred $prefix.0/24 …")
        _uiState.update {
            it.copy(
                discoveredDevices = emptyList(),
                isScanningLan = true,
                lastMessage = "Escaneando red local (puerto 8001)… Puede tardar ~1 min."
            )
        }
        samsungScanJob = viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { SamsungPortScanner.scan() }
            AppLog.d("Escaneo Samsung terminado: ${found.size} host(s) con :8001")
            _uiState.update {
                it.copy(
                    isScanningLan = false,
                    discoveredDevices = found.sortedBy { it.host },
                    lastMessage = when {
                        found.isEmpty() -> "No se encontró ningún dispositivo con el puerto 8001 abierto."
                        else -> "Encontrados: ${found.size}. Toca uno para usar su IP."
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
        _uiState.update {
            it.copy(
                tvIp = tv.host,
                lastMessage = "Seleccionado: ${tv.name} (${tv.host})"
            )
        }
    }

    fun clearDiscoveredList() {
        _uiState.update { it.copy(discoveredDevices = emptyList()) }
    }

    fun sendKeySamsung(key: SamsungKey) {
        val ip = _uiState.value.tvIp.trim()
        if (ip.isEmpty()) {
            _uiState.update { it.copy(lastMessage = "Busca en la red o escribe la IP de la TV") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastMessage = null) }
            val result = samsungClient.sendKey(ip, key)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    lastMessage = result.fold(
                        onSuccess = { "Enviado (Samsung): ${key.name}" },
                        onFailure = { e -> "Error: ${e.message ?: "desconocido"}" }
                    )
                )
            }
        }
    }

    fun sendKeyAndroidTv(key: RemoteKeyCode) {
        val session = remote
        if (session == null) {
            _uiState.update { it.copy(lastMessage = "Conecta primero el control Android TV") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastMessage = null) }
            val result = runCatching { withContext(Dispatchers.IO) { session.sendKey(key) } }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    lastMessage = result.fold(
                        onSuccess = { "Enviado (Android TV)" },
                        onFailure = { e -> "Error: ${e.message ?: "desconocido"}" }
                    )
                )
            }
        }
    }

    /**
     * Android TV: envía texto por **IME** ([RemoteSession.sendImeText]), necesario para buscadores.
     * Samsung: solo dígitos 0-9 por HTTP.
     */
    fun sendKeyboardText(text: String) {
        if (text.isEmpty()) {
            _uiState.update { it.copy(lastMessage = "Escribe texto para enviar") }
            return
        }
        when (_uiState.value.backend) {
            TvBackend.ANDROID_TV -> {
                val session = remote
                if (session == null) {
                    _uiState.update { it.copy(lastMessage = "Conecta primero el control Android TV") }
                    return
                }
                viewModelScope.launch {
                    _uiState.update { it.copy(isBusy = true, lastMessage = null) }
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            session.sendImeText(text)
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            lastMessage = result.fold(
                                onSuccess = { "Texto enviado a la TV (IME)" },
                                onFailure = { e -> "Error: ${e.message ?: "desconocido"}" }
                            )
                        )
                    }
                }
            }
            TvBackend.SAMSUNG -> {
                val ip = _uiState.value.tvIp.trim()
                if (ip.isEmpty()) {
                    _uiState.update { it.copy(lastMessage = "Indica la IP de la TV") }
                    return
                }
                val digits = text.filter { it.isDigit() }
                if (digits.isEmpty()) {
                    _uiState.update {
                        it.copy(lastMessage = "En Samsung el teclado solo envía números (0-9)") }
                    return
                }
                viewModelScope.launch {
                    _uiState.update { it.copy(isBusy = true, lastMessage = null) }
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            for (ch in digits) {
                                val key = digitToSamsungKey(ch) ?: continue
                                samsungClient.sendKey(ip, key).getOrThrow()
                                delay(120)
                            }
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            lastMessage = result.fold(
                                onSuccess = { "Teclado Samsung: enviados ${digits.length} dígitos" },
                                onFailure = { e -> "Error: ${e.message ?: "desconocido"}" }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun digitToSamsungKey(c: Char): SamsungKey? = when (c) {
        '0' -> SamsungKey.DIGIT_0
        '1' -> SamsungKey.DIGIT_1
        '2' -> SamsungKey.DIGIT_2
        '3' -> SamsungKey.DIGIT_3
        '4' -> SamsungKey.DIGIT_4
        '5' -> SamsungKey.DIGIT_5
        '6' -> SamsungKey.DIGIT_6
        '7' -> SamsungKey.DIGIT_7
        '8' -> SamsungKey.DIGIT_8
        '9' -> SamsungKey.DIGIT_9
        else -> null
    }

    fun androidTvStartPairing() {
        val ip = _uiState.value.tvIp.trim()
        if (ip.isEmpty()) {
            _uiState.update { it.copy(lastMessage = "Busca en la red o escribe la IP del Chromecast / Android TV") }
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
                        ?: "Introduce el código de 6 caracteres que muestra la TV"
                )
            }
        }
    }

    fun androidTvFinishPairing() {
        val pin = _uiState.value.androidTvPin
        if (pin.length != 6) {
            _uiState.update { it.copy(lastMessage = "El código debe tener 6 caracteres hex (0-9, a-f)") }
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
                        ?: "Emparejamiento correcto. Pulsa «Conectar control»."
                )
            }
        }
    }

    fun androidTvConnectRemote() {
        val ip = _uiState.value.tvIp.trim()
        if (ip.isEmpty()) {
            _uiState.update { it.copy(lastMessage = "Busca en la red o escribe la IP") }
            return
        }
        if (!credentials.hasCredentials(ip)) {
            _uiState.update { it.copy(lastMessage = "Empareja primero con la TV") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastMessage = null) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    remote?.close()
                    val ssl = AndroidTvSsl.createClientContext(
                        credentials.getCertPem(ip)!!,
                        credentials.getKeyPem(ip)!!
                    )
                    val s = RemoteSession(ip, ssl)
                    s.connect()
                    remote = s
                }
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    androidTvPhase = if (result.isSuccess) {
                        AndroidTvConnectionPhase.REMOTE_CONNECTED
                    } else {
                        AndroidTvConnectionPhase.ERROR
                    },
                    lastMessage = result.exceptionOrNull()?.message
                        ?: "Conectado a Android TV / Google TV"
                )
            }
        }
    }

    fun androidTvDisconnect() {
        remote?.close()
        remote = null
        pairing?.close()
        pairing = null
        val ip = _uiState.value.tvIp.trim()
        val next = if (ip.isNotEmpty() && credentials.hasCredentials(ip)) {
            AndroidTvConnectionPhase.PAIRED
        } else {
            AndroidTvConnectionPhase.IDLE
        }
        _uiState.update {
            it.copy(androidTvPhase = next, lastMessage = "Sesión remota cerrada")
        }
    }

    fun androidTvForgetDevice() {
        val ip = _uiState.value.tvIp.trim()
        if (ip.isNotEmpty()) credentials.clear(ip)
        remote?.close()
        remote = null
        pairing?.close()
        pairing = null
        _uiState.update {
            it.copy(
                androidTvPhase = AndroidTvConnectionPhase.IDLE,
                androidTvPin = "",
                lastMessage = "Credenciales borradas para esta IP"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAllLanScans()
        remote?.close()
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
