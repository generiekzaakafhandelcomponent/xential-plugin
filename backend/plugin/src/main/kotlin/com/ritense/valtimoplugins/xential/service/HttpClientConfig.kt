package com.ritense.valtimoplugins.xential.service

import com.ritense.valtimoplugins.xential.domain.HttpClientProperties
import com.rotterdam.xential.api.DefaultApi
import mu.KotlinLogging
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.net.InetAddress
import java.net.UnknownHostException
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
        logger.info { "trustManagerFactory: Certificate file: $certFile" }
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
            logger.info { "keyManagerFactory: clientCert file: $clientCertFile" }
            val clientCert = certificateFactory.generateCertificate(FileInputStream(clientCertFile))

            logger.info { "keyManagerFactory: privateKey file: ---$privateKeyFile---" }
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
        val trustManagerFactory = trustManagerFactory(properties.clientCertFile!!)
        val keyManagerFactory = keyManagerFactory(
            properties.clientPrivateKeyFilename,
            properties.serverCertificateFilename
        )

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory?.keyManagers, trustManagerFactory.trustManagers, null)
        }

        val credentials = Credentials.basic(properties.applicationName, properties.applicationPassword)
        val customDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                logger.info { "Resolving hostname: $hostname" }
                return try {
                    InetAddress.getAllByName("127.0.0.1").toList()
                } catch (e: UnknownHostException) {
                    logger.error("Failed to resolve hostname: $hostname")
                    throw e
                }
            }
        }
        val customClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", credentials)
                    .build()
                chain.proceed(request)
            }
            .sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
            .dns(customDns)
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

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
