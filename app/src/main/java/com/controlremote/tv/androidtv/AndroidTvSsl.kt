package com.controlremote.tv.androidtv

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.PEMParser
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal object AndroidTvSsl {

    fun createClientContext(certPem: String, keyPem: String): SSLContext {
        val certFactory = CertificateFactory.getInstance("X.509")
        val cert = certFactory.generateCertificate(
            ByteArrayInputStream(certPem.trim().toByteArray(Charsets.UTF_8))
        ) as X509Certificate
        val privateKey = loadPrivateKeyFromPem(keyPem)
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        ks.load(null, null)
        ks.setKeyEntry("atv", privateKey, CharArray(0), arrayOf(cert))
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, CharArray(0))
        val ssl = SSLContext.getInstance("TLS")
        ssl.init(kmf.keyManagers, trustAll(), null)
        return ssl
    }

    private fun loadPrivateKeyFromPem(pem: String): PrivateKey {
        PEMParser(InputStreamReader(ByteArrayInputStream(pem.toByteArray(Charsets.UTF_8)))).use { parser ->
            val obj = parser.readObject() ?: error("PEM vacío")
            val converter = JcaPEMKeyConverter()
            return when (obj) {
                is PEMKeyPair -> converter.getKeyPair(obj).private
                is PrivateKeyInfo -> converter.getPrivateKey(obj)
                else -> error("Tipo PEM no soportado: ${obj.javaClass.name}")
            }
        }
    }

    private fun trustAll(): Array<TrustManager> {
        return arrayOf(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
            }
        )
    }
}
