package com.ritense.valtimoplugins.xential.service

import com.ritense.documentenapi.client.CreateDocumentRequest
import com.ritense.documentenapi.client.DocumentStatusType
import com.ritense.documentenapi.client.DocumentenApiClient
import com.ritense.documentenapi.event.DocumentCreated
import com.ritense.plugin.service.PluginService
import com.ritense.valtimo.contract.authentication.UserManagementService
import com.ritense.valtimoplugins.xential.domain.DocumentCreatedMessage
import com.ritense.valtimoplugins.xential.domain.GenerateDocumentProperties
import com.ritense.valtimoplugins.xential.domain.HttpClientProperties
import com.ritense.valtimoplugins.xential.domain.XentialToken
import com.ritense.valtimoplugins.xential.plugin.TemplateDataEntry
import com.ritense.valtimoplugins.xential.plugin.XentialPlugin
import com.ritense.valtimoplugins.xential.repository.XentialTokenRepository
import com.ritense.valueresolver.ValueResolverService
import com.ritense.zakenapi.ZaakUrlProvider
import com.ritense.zakenapi.client.LinkDocumentRequest
import com.ritense.zakenapi.client.ZakenApiClient
import com.rotterdam.xential.api.DefaultApi
import com.rotterdam.xential.model.Sjabloondata
import mu.KotlinLogging
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.context.ApplicationEventPublisher
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDate
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class DocumentGenerationService(
    private val xentialTokenRepository: XentialTokenRepository,
    private val pluginService: PluginService,
    private val documentenApiClient: DocumentenApiClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val zaakUrlProvider: ZaakUrlProvider,
    private val zakenApiClient: ZakenApiClient,
    private val runtimeService: RuntimeService,
    private val valueResolverService: ValueResolverService,
    private val userManagementService: UserManagementService
) {

    fun generateDocument(
        httpClientProperties: HttpClientProperties,
        processId: UUID,
        generateDocumentProperties: GenerateDocumentProperties,
        execution: DelegateExecution,
    ) {
        logger.info { "current userid: ${userManagementService.currentUserId}" }

        val api = configureClient(httpClientProperties)
        val resolvedMap = resolveTemplateData(generateDocumentProperties.templateData, execution)
        val sjabloonVulData = resolvedMap.map { "<${it.key}>${it.value}</${it.key}>" }.joinToString()
        val result = api.creeerDocument(
            gebruikersId = userManagementService.currentUserId,
            accepteerOnbekend = false,
            sjabloondata = Sjabloondata(
                sjabloonId = generateDocumentProperties.templateId.toString(),
                bestandsFormaat = Sjabloondata.BestandsFormaat.valueOf(generateDocumentProperties.fileFormat.name),
                documentkenmerk = generateDocumentProperties.documentId,
                sjabloonVulData = "<root>$sjabloonVulData</root>"
            )
        )
        logger.info { "found something: $result" }
        val xentialToken = XentialToken(
            token = UUID.fromString(result.documentCreatieSessieId),
//            externalToken = result.documentCreatieSessieId,
            processId = processId,
            messageName = generateDocumentProperties.messageName,
            resumeUrl = result.resumeUrl
        )
        logger.info { "token: ${xentialToken.token}" }
        xentialTokenRepository.save(xentialToken)
        logger.info { "ready" }
    }

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

    private fun configureClient(properties: HttpClientProperties): DefaultApi {
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

    fun onDocumentGenerated(message: DocumentCreatedMessage) {
        val plugin = getXentialPlugin(message)
        val documentenApiPlugin = plugin.documentenApiPluginConfiguration
        val zakenApiPlugin = plugin.zakenApiPluginConfiguration

        val bytes = Base64.getDecoder().decode(message.data)

        val xentialToken = xentialTokenRepository.findById(UUID.fromString(message.documentCreatieSessieId))
            .orElseThrow { NoSuchElementException("Could not find Xential Token ${message.documentCreatieSessieId}") }

        ByteArrayInputStream(bytes).use { inputStream ->
            val createDocumentRequest = CreateDocumentRequest(
                bronorganisatie = documentenApiPlugin.bronorganisatie,
                creatiedatum = LocalDate.now(),
                titel = message.documentkenmerk,
                vertrouwelijkheidaanduiding = null,
                auteur = message.gebruiker,
                status = DocumentStatusType.DEFINITIEF,
                taal = "nld",
                bestandsnaam = message.documentkenmerk,
                bestandsomvang = bytes.size.toLong(),
                inhoud = inputStream,
                beschrijving = "",
                ontvangstdatum = LocalDate.now(),
                verzenddatum = LocalDate.now(),
                informatieobjecttype = null,
                formaat = message.formaat.name,
            )

            val documentCreateResult = documentenApiClient.storeDocument(
                documentenApiPlugin.authenticationPluginConfiguration,
                documentenApiPlugin.url,
                createDocumentRequest
            )
            val event = DocumentCreated(
                documentCreateResult.url,
                documentCreateResult.auteur,
                documentCreateResult.bestandsnaam,
                documentCreateResult.bestandsomvang,
                documentCreateResult.beginRegistratie
            )
            applicationEventPublisher.publishEvent(event)

            val zaakUrl = zaakUrlProvider.getZaakUrl(xentialToken.processId)
            zakenApiClient.linkDocument(
                zakenApiPlugin.authenticationPluginConfiguration,
                zakenApiPlugin.url,
                LinkDocumentRequest(
                    documentCreateResult.url,
                    zaakUrl.toString(),
                    message.documentkenmerk,
                    ""
                )
            )

            runtimeService.createMessageCorrelation(xentialToken.messageName)
                .processInstanceId(xentialToken.processId.toString())
                .correlate()
        }
    }

    private fun resolveTemplateData(
        templateData: Array<TemplateDataEntry>,
        execution: DelegateExecution
    ): Map<String, Any?> {
        val placeHolderValueMap = valueResolverService.resolveValues(
            execution.processInstanceId,
            execution,
            templateData.map { it.value }.toList()
        )
        return templateData.associate { it.key to placeHolderValueMap.getOrDefault(it.value, null) }
    }

    private fun getXentialPlugin(message: DocumentCreatedMessage): XentialPlugin {
        //FIXME needs a way of determining the right plugin
        val pluginConfig = pluginService.findPluginConfiguration(XentialPlugin.PLUGIN_KEY) { _ -> true }
            ?: throw NoSuchElementException("Could not find Xential plugin")
        return pluginService.createInstance(pluginConfig) as XentialPlugin
    }

    companion object {
        private val logger = KotlinLogging.logger {  }
    }
}
