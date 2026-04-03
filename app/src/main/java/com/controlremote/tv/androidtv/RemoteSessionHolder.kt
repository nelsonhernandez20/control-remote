package com.controlremote.tv.androidtv

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RemoteSessionState {
    data object Disconnected : RemoteSessionState
    data class Connecting(val host: String) : RemoteSessionState
    data class Connected(val host: String) : RemoteSessionState
    data class Error(val message: String) : RemoteSessionState
}

/**
 * Sesión [RemoteSession] compartida entre la UI y [TvRemoteForegroundService].
 * Permite mantener el socket activo cuando la actividad se destruye.
 */
object RemoteSessionHolder {

    private val lock = Any()
    private var session: RemoteSession? = null

    private val _state = MutableStateFlow<RemoteSessionState>(RemoteSessionState.Disconnected)
    val state: StateFlow<RemoteSessionState> = _state.asStateFlow()

    private val _refreshNeeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshNeeded: SharedFlow<Unit> = _refreshNeeded.asSharedFlow()

    fun getSession(): RemoteSession? = synchronized(lock) { session }

    fun getConnectedHost(): String? = when (val s = _state.value) {
        is RemoteSessionState.Connected -> s.host
        else -> null
    }

    fun setConnecting(host: String) {
        _state.value = RemoteSessionState.Connecting(host)
    }

    fun setConnected(s: RemoteSession, remoteHost: String) {
        synchronized(lock) {
            session?.close()
            session = s
        }
        _state.value = RemoteSessionState.Connected(remoteHost)
    }

    fun setError(message: String) {
        synchronized(lock) {
            session?.close()
            session = null
        }
        _state.value = RemoteSessionState.Error(message)
    }

    fun clear() {
        synchronized(lock) {
            session?.close()
            session = null
        }
        _state.value = RemoteSessionState.Disconnected
    }

    fun notifyCredentialsRefresh() {
        _refreshNeeded.tryEmit(Unit)
    }
}
