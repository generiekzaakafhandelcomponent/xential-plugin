package com.ritense.valtimoplugins.xential.service

import com.rotterdam.esb.xential.api.DefaultApi
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Credentials
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpHeaders

class OpentunnelEsbClient {

    fun createRestClient(
        baseUrl: String,
        applicationName: String,
        applicationPassword: String,
        authenticationEnabled: Boolean,
        base64PrivateKey: String? = null,
        base64ClientCert: String? = null,
        base64ServerCert: String? = null
    ): RestClient {
        logger.debug { "Creating ESB client" }
        val credentials = Credentials.basic(applicationName, applicationPassword)

        return when {
            authenticationEnabled -> {
                require(base64PrivateKey != null)
                require(base64ClientCert != null)
                require(base64ServerCert != null)
                HttpClientHelper.createSecureHttpClient(
                    base64PrivateKey = base64PrivateKey,
                    base64ClientCert = base64ClientCert,
                    base64CaCert = base64ServerCert
                ).also {
                    logger.debug { "Using secure HttpClient with Client Certificate authentication" }
                }
            }
            else ->
                HttpClientHelper.createDefaultHttpClient().also {
                    logger.debug { "Using default HttpClient" }
                }
        }.let { httpClient ->
            RestClient.builder()
                .defaultHeader("Authorization", credentials)
                .baseUrl(baseUrl)
                .requestFactory(HttpComponentsClientHttpRequestFactory(httpClient))
                .build().also {
                    logger.debug { "Created ESB client using RestClient" }
                }
        }
    }

    fun documentApi(restClient: RestClient) = DefaultApi(restClient)

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
