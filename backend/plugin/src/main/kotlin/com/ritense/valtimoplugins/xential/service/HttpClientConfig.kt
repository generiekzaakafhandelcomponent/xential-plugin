/*
 * Copyright 2015-2025 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
