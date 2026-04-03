package com.controlremote.tv.androidtv

import com.controlremote.androidtv.proto.RemoteConfigure
import com.controlremote.androidtv.proto.RemoteDeviceInfo
import com.controlremote.androidtv.proto.RemoteDirection
import com.controlremote.androidtv.proto.RemoteEditInfo
import com.controlremote.androidtv.proto.RemoteImeBatchEdit
import com.controlremote.androidtv.proto.RemoteImeObject
import com.controlremote.androidtv.proto.RemoteKeyCode
import com.controlremote.androidtv.proto.RemoteKeyInject
import com.controlremote.androidtv.proto.RemoteMessage
import com.controlremote.androidtv.proto.RemotePingResponse
import com.controlremote.androidtv.proto.RemoteSetActive
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import kotlin.concurrent.thread

/**
 * Sesión de control remoto TLS en el puerto 6466 (Android TV / Google TV).
 */
class RemoteSession(
    private val host: String,
    private val sslContext: SSLContext,
    enableIme: Boolean = true
) {
    private val requestedFeatures: Int = run {
        var f = 1 or 2 or 32 or 64 or 512
        if (enableIme) f = f or 4
        f
    }
    private val activeFeatures = AtomicInteger(requestedFeatures)

    private var socket: SSLSocket? = null
    private var output: OutputStream? = null
    private val remoteReady = CountDownLatch(1)
    private val closed = AtomicBoolean(false)
    private var reader: Thread? = null

    /** Sincronizados con los mensajes IME que envía la TV (p. ej. al enfocar el buscador). */
    private val imeCounter = AtomicInteger(0)
    private val fieldCounter = AtomicInteger(0)

    fun connect(timeoutMs: Long = 20_000) {
        val sock = sslContext.socketFactory.createSocket(host, REMOTE_PORT) as SSLSocket
        sock.soTimeout = 0
        sock.startHandshake()
        socket = sock
        output = sock.getOutputStream()
        reader = thread(name = "atv-remote-read") {
            try {
                readLoop(sock.getInputStream())
            } catch (_: Exception) {
                if (!closed.get()) {
                    remoteReady.countDown()
                }
            }
        }
        if (!remoteReady.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            close()
            error("Tiempo de espera agotado al iniciar el control remoto")
        }
    }

    private fun readLoop(input: InputStream) {
        while (!closed.get()) {
            val msg = RemoteMessage.parseDelimitedFrom(input) ?: break
            handleIncoming(msg)
        }
    }

    private fun handleIncoming(msg: RemoteMessage) {
        val out = output ?: return
        if (msg.hasRemoteStart()) {
            remoteReady.countDown()
        }
        val reply = RemoteMessage.newBuilder()
        when {
            msg.hasRemoteConfigure() -> {
                val cfg = msg.remoteConfigure
                val merged = activeFeatures.get() and cfg.code1
                activeFeatures.set(merged)
                reply.setRemoteConfigure(
                    RemoteConfigure.newBuilder()
                        .setCode1(merged)
                        .setDeviceInfo(
                            RemoteDeviceInfo.newBuilder()
                                .setUnknown1(1)
                                .setUnknown2("1")
                                .setPackageName("com.controlremote.tv")
                                .setAppVersion("1.0.0")
                                .build()
                        )
                        .build()
                )
            }
            msg.hasRemoteSetActive() -> {
                reply.setRemoteSetActive(
                    RemoteSetActive.newBuilder()
                        .setActive(activeFeatures.get())
                        .build()
                )
            }
            msg.hasRemotePingRequest() -> {
                reply.setRemotePingResponse(
                    RemotePingResponse.newBuilder()
                        .setVal1(msg.remotePingRequest.val1)
                        .build()
                )
            }
            msg.hasRemoteImeBatchEdit() -> {
                val be = msg.remoteImeBatchEdit
                imeCounter.set(be.imeCounter)
                fieldCounter.set(be.fieldCounter)
                return
            }
            msg.hasRemoteImeKeyInject() -> {
                return
            }
            else -> return
        }
        synchronized(out) {
            reply.build().writeDelimitedTo(out)
        }
    }

    fun sendKey(keyCode: RemoteKeyCode) {
        val out = output ?: error("No conectado")
        val payload = RemoteMessage.newBuilder()
            .setRemoteKeyInject(
                RemoteKeyInject.newBuilder()
                    .setKeyCode(keyCode)
                    .setDirection(RemoteDirection.SHORT)
                    .build()
            )
            .build()
        synchronized(out) {
            payload.writeDelimitedTo(out)
        }
    }

    /**
     * Envía texto al campo enfocado (buscador, etc.) vía IME.
     * Debe coincidir con [androidtvremote2](https://github.com/tronikos/androidtvremote2) / Remote v2.
     */
    fun sendImeText(text: String) {
        val out = output ?: error("No conectado")
        if (text.isEmpty()) return
        val endIdx = (text.length - 1).coerceAtLeast(0)
        val imeObject = RemoteImeObject.newBuilder()
            .setStart(endIdx)
            .setEnd(endIdx)
            .setValue(text)
            .build()
        val editInfo = RemoteEditInfo.newBuilder()
            .setInsert(1)
            .setTextFieldStatus(imeObject)
            .build()
        val batch = RemoteImeBatchEdit.newBuilder()
            .setImeCounter(imeCounter.get())
            .setFieldCounter(fieldCounter.get())
            .addEditInfo(editInfo)
            .build()
        val payload = RemoteMessage.newBuilder()
            .setRemoteImeBatchEdit(batch)
            .build()
        synchronized(out) {
            payload.writeDelimitedTo(out)
        }
    }

    fun close() {
        closed.set(true)
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        output = null
    }

    companion object {
        private const val REMOTE_PORT = 6466
    }
}
