package com.controlremote.tv.androidtv

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.util.Date
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter

/**
 * Genera par certificado/clave RSA en PEM (compatible con el protocolo de emparejamiento Android TV).
 */
object SelfSignedCertGenerator {

    fun generatePem(commonName: String): Pair<String, String> {
        // RSA con el proveedor por defecto del sistema (Conscrypt en Android).
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        val now = Date()
        val notAfter = Date(now.time + 10L * 365 * 86400000L)
        val issuer = X500Name("CN=$commonName")
        val subject = X500Name("CN=$commonName")
        val certBuilder = JcaX509v3CertificateBuilder(
            issuer,
            BigInteger.valueOf(1000L),
            now,
            notAfter,
            subject,
            keyPair.public
        )
        certBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        certBuilder.addExtension(
            Extension.subjectAlternativeName,
            false,
            GeneralNames(GeneralName(GeneralName.dNSName, commonName))
        )
        // No usar el proveedor "BC" para firmar: en muchos Androids BC no expone
        // SHA256withRSA y falla con "Cannot create Signer ... for provider BC".
        val cert = JcaX509CertificateConverter()
            .getCertificate(
                certBuilder.build(
                    JcaContentSignerBuilder("SHA256withRSA")
                        .build(keyPair.private)
                )
            )

        val certPem = pemEncode("CERTIFICATE", cert.encoded)
        val keyPem = pemEncode("PRIVATE KEY", keyPair.private.encoded)
        return certPem to keyPem
    }

    private fun pemEncode(type: String, der: ByteArray): String {
        val baos = ByteArrayOutputStream()
        PemWriter(OutputStreamWriter(baos, Charsets.UTF_8)).use { writer ->
            writer.writeObject(PemObject(type, der))
        }
        return baos.toString(Charsets.UTF_8.name())
    }
}
