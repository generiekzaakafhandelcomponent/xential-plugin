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
import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.io.File
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

    private fun base64ToInputStream(base64String: String): ByteArrayInputStream {
        // Decode the base64 encoded string to bytes
        val decodedBytes = Base64.getDecoder().decode(base64String)

        // Convert the decoded bytes array to ByteArrayInputStream
        return ByteArrayInputStream(decodedBytes)
    }

    private fun trustManagerFactory(serverCertificate: String): TrustManagerFactory {

        // Load the server certificate
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val clientCertificateDecoded = certificateFactory.generateCertificate(base64ToInputStream(serverCertificate))

        // Create a KeyStore with the server certificate
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("server", clientCertificateDecoded)
        }

        // Configure the TrustManager
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)
        return trustManagerFactory
    }

    private fun keyManagerFactory(clientPrivateKey: String?, clientCertificate: String?): KeyManagerFactory? {
        return if (clientPrivateKey != null && clientCertificate != null) {
            val certificateFactory = CertificateFactory.getInstance("X.509")

            val clientCert = certificateFactory.generateCertificate(base64ToInputStream(clientCertificate))

            val privateKey = loadPrivateKeyFromString(clientPrivateKey)

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
        // clientCertFile = fullchain
        val trustManagerFactory = trustManagerFactory(properties.serverCertificate)

        val keyManagerFactory = keyManagerFactory(
            properties.clientPrivateKey,
            properties.clientCertificate
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

    private fun loadPrivateKeyFromString(input: String): java.security.PrivateKey {
        val decodedBytes = Base64.getDecoder().decode(input)

        val keyBytes = String(decodedBytes)
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .let { Base64.getDecoder().decode(it) }

        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
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
