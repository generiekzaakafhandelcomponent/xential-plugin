package com.ritense.valtimoplugins.xential.service

import com.ritense.valtimoplugins.xential.domain.HttpClientProperties
import com.rotterdam.xential.api.DefaultApi
import okhttp3.Credentials
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class HttpClientConfig {

    private fun trustManagerFactory(certFile: File): TrustManagerFactory {

        // Load the server certificate
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val serverCert = certificateFactory.generateCertificate(FileInputStream(certFile))

        // Create a KeyStore with the server certificate
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("server", serverCert)
        }

        // Configure the TrustManager
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)
        return trustManagerFactory
    }

    private fun keyManagerFactory(privateKeyFile: File?, clientCertFile: File?): KeyManagerFactory? {
        return if (privateKeyFile != null && clientCertFile != null) {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val clientCert = certificateFactory.generateCertificate(FileInputStream(clientCertFile))
            val privateKey = loadPrivateKey(privateKeyFile)

            // Create a KeyStore with the client certificate and private key
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setKeyEntry("client", privateKey, null, arrayOf(clientCert))
            }

            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore, null)
            }
        } else null
    }

    fun configureClient(properties: HttpClientProperties): DefaultApi {
        val trustManagerFactory = trustManagerFactory(properties.serverCertificateFilename)
        val keyManagerFactory = keyManagerFactory(
            properties.clientPrivateKeyFilename,
            properties.clientCertFile
        )

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory?.keyManagers, trustManagerFactory.trustManagers, null)
        }

        val credentials = Credentials.basic(properties.applicationName, properties.applicationPassword)
        val customClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", credentials)
                    .build()
                chain.proceed(request)
            }
            .sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
            .build()

        return DefaultApi(properties.baseUrl.toString(), customClient)
    }

    private fun loadPrivateKey(file: File): java.security.PrivateKey {
        val keyBytes = file.readText()
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .let { Base64.getDecoder().decode(it) }

        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

}
