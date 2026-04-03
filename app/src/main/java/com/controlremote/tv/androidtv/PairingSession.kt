package com.controlremote.tv.androidtv

import com.google.polo.wire.protobuf.PoloProto
import com.google.protobuf.ByteString
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/**
 * Emparejamiento TLS en el puerto 6467 (protocolo Polo / Android TV Remote v2).
 */
class PairingSession(
    private val host: String,
    private val sslContext: SSLContext,
    private val clientName: String
) {
    private var socket: SSLSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    fun connect() {
        val sock = sslContext.socketFactory.createSocket(host, PAIR_PORT) as SSLSocket
        sock.soTimeout = 30_000
        sock.startHandshake()
        socket = sock
        input = sock.getInputStream()
        output = sock.getOutputStream()
    }

    /** Paso 1: envía PairingRequest y completa el intercambio hasta ConfigurationAck. */
    fun startPairing() {
        val out = output ?: error("Sin conexión")
        val inp = input ?: error("Sin conexión")
        val req = PoloProto.OuterMessage.newBuilder()
            .setProtocolVersion(2)
            .setStatus(PoloProto.OuterMessage.Status.STATUS_OK)
            .setPairingRequest(
                PoloProto.PairingRequest.newBuilder()
                    .setServiceName("atvremote")
                    .setClientName(clientName)
                    .build()
            )
            .build()
        req.writeDelimitedTo(out)
        while (true) {
            val msg = PoloProto.OuterMessage.parseDelimitedFrom(inp)
                ?: error("Conexión cerrada durante el emparejamiento")
            if (msg.status != PoloProto.OuterMessage.Status.STATUS_OK) {
                error("Estado del protocolo: ${msg.status}")
            }
            when {
                msg.hasPairingRequestAck() -> {
                    val resp = PoloProto.OuterMessage.newBuilder()
                        .setProtocolVersion(2)
                        .setStatus(PoloProto.OuterMessage.Status.STATUS_OK)
                        .setOptions(
                            PoloProto.Options.newBuilder()
                                .setPreferredRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
                                .addInputEncodings(
                                    PoloProto.Options.Encoding.newBuilder()
                                        .setType(
                                            PoloProto.Options.Encoding.EncodingType.ENCODING_TYPE_HEXADECIMAL
                                        )
                                        .setSymbolLength(6)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                    resp.writeDelimitedTo(out)
                }
                msg.hasOptions() -> {
                    val resp = PoloProto.OuterMessage.newBuilder()
                        .setProtocolVersion(2)
                        .setStatus(PoloProto.OuterMessage.Status.STATUS_OK)
                        .setConfiguration(
                            PoloProto.Configuration.newBuilder()
                                .setEncoding(
                                    PoloProto.Options.Encoding.newBuilder()
                                        .setType(
                                            PoloProto.Options.Encoding.EncodingType.ENCODING_TYPE_HEXADECIMAL
                                        )
                                        .setSymbolLength(6)
                                        .build()
                                )
                                .setClientRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
                                .build()
                        )
                        .build()
                    resp.writeDelimitedTo(out)
                }
                msg.hasConfigurationAck() -> return
                else -> error("Mensaje de emparejamiento inesperado")
            }
        }
    }

    /**
     * Paso 2: código de 6 caracteres hex mostrado en la TV (ej. "a1b2c3").
     */
    fun finishPairing(hexSix: String) {
        val out = output ?: error("Sin conexión")
        val inp = input ?: error("Sin conexión")
        val code = hexSix.trim().lowercase()
        require(code.length == 6) { "El código debe tener exactamente 6 caracteres hexadecimales" }
        require(code.all { it in '0'..'9' || it in 'a'..'f' }) { "Código no hexadecimal" }
        val sock = socket ?: error("Sin socket")
        val peer = sock.session.peerCertificates[0] as X509Certificate
        val local = sock.session.localCertificates[0] as X509Certificate
        val digest = computePairingDigest(local, peer, code)
        val secretMsg = PoloProto.OuterMessage.newBuilder()
            .setProtocolVersion(2)
            .setStatus(PoloProto.OuterMessage.Status.STATUS_OK)
            .setSecret(
                PoloProto.Secret.newBuilder()
                    .setSecret(ByteString.copyFrom(digest))
                    .build()
            )
            .build()
        secretMsg.writeDelimitedTo(out)
        while (true) {
            val msg = PoloProto.OuterMessage.parseDelimitedFrom(inp)
                ?: error("Conexión cerrada antes de confirmar el emparejamiento")
            if (msg.hasSecretAck()) return
            if (msg.status != PoloProto.OuterMessage.Status.STATUS_OK) {
                error("Emparejamiento rechazado por la TV (${msg.status})")
            }
        }
    }

    fun close() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        input = null
        output = null
    }

    companion object {
        private const val PAIR_PORT = 6467

        internal fun computePairingDigest(
            clientCert: X509Certificate,
            serverCert: X509Certificate,
            pairingCodeHexSix: String
        ): ByteArray {
            val clientPub = clientCert.publicKey as RSAPublicKey
            val serverPub = serverCert.publicKey as RSAPublicKey
            val md = MessageDigest.getInstance("SHA-256")
            md.update(bigIntToHexBytesModulus(clientPub.modulus))
            md.update(bigIntToHexBytesExponent(clientPub.publicExponent))
            md.update(bigIntToHexBytesModulus(serverPub.modulus))
            md.update(bigIntToHexBytesExponent(serverPub.publicExponent))
            md.update(hexToBytes(pairingCodeHexSix.substring(2)))
            val digest = md.digest()
            val expectedFirst = pairingCodeHexSix.substring(0, 2).toInt(16)
            if ((digest[0].toInt() and 0xFF) != expectedFirst) {
                error("El código no coincide con la verificación (revisa el PIN en la TV)")
            }
            return digest
        }

        private fun bigIntToHexBytesModulus(n: BigInteger): ByteArray {
            var h = n.toString(16).uppercase()
            if (h.length % 2 == 1) h = "0$h"
            return hexToBytes(h)
        }

        private fun bigIntToHexBytesExponent(e: BigInteger): ByteArray {
            val h = "0" + e.toString(16).uppercase()
            return hexToBytes(h)
        }

        private fun hexToBytes(hex: String): ByteArray {
            require(hex.length % 2 == 0) { "hex impar" }
            return ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }
    }
}
