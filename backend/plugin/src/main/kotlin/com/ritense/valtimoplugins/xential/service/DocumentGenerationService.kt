package com.ritense.valtimoplugins.xential.service

import com.ritense.plugin.service.PluginService
import com.ritense.resource.domain.MetadataType
import com.ritense.resource.service.TemporaryResourceStorageService
import com.ritense.valtimo.contract.authentication.UserManagementService
import com.ritense.valtimoplugins.xential.domain.HttpClientProperties
import com.ritense.valtimoplugins.xential.domain.DocumentCreatedMessage
import com.ritense.valtimoplugins.xential.domain.GenerateDocumentProperties
import com.ritense.valtimoplugins.xential.domain.XentialToken
import com.ritense.valtimoplugins.xential.plugin.TemplateDataEntry
import com.ritense.valtimoplugins.xential.plugin.XentialPlugin
import com.ritense.valtimoplugins.xential.repository.XentialTokenRepository
import com.ritense.valueresolver.ValueResolverService
import com.rotterdam.xential.model.Sjabloondata
import mu.KotlinLogging
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.delegate.DelegateExecution
import java.io.ByteArrayInputStream
import java.util.*


class DocumentGenerationService(

    private val xentialTokenRepository: XentialTokenRepository,
    private val temporaryResourceStorageService: TemporaryResourceStorageService,
    private val pluginService: PluginService,
    private val runtimeService: RuntimeService,
    private val valueResolverService: ValueResolverService,
    private val userManagementService: UserManagementService,
    private val httpClientConfig: HttpClientConfig
) {

    fun generateDocument(
        httpClientProperties: HttpClientProperties,
        processId: UUID,
        generateDocumentProperties: GenerateDocumentProperties,
        execution: DelegateExecution,
    ) {
        logger.info { "current userid: ${userManagementService.currentUserId}" }

        val api = httpClientConfig.configureClient(httpClientProperties)
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
//            token = UUID.randomUUID(),
            processId = processId,
            messageName = generateDocumentProperties.messageName,
            resumeUrl = result.resumeUrl.toString()
        )


        logger.info { "token: ${xentialToken.token}" }
        xentialTokenRepository.save(xentialToken)

        execution.setVariable("xentialStatus", result.status)

        logger.info { "ready" }
    }

    fun onDocumentGenerated(message: DocumentCreatedMessage) {

        val bytes = Base64.getDecoder().decode(message.data)

        val xentialToken = xentialTokenRepository.findById(UUID.fromString(message.documentCreatieSessieId))
            .orElseThrow { NoSuchElementException("Could not find Xential Token ${message.documentCreatieSessieId}") }

        ByteArrayInputStream(bytes).use { inputStream ->
            val metadata = mapOf(MetadataType.FILE_NAME.key to "${xentialToken.processId}-${xentialToken.messageName}.tmp")
            val resourceId = temporaryResourceStorageService.store(inputStream, metadata)
            val resourceIdMap = mapOf("resourceId" to resourceId)
            runtimeService.createMessageCorrelation(xentialToken.messageName)
                .processInstanceId(xentialToken.processId.toString())
                .setVariables(resourceIdMap)
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
        private val logger = KotlinLogging.logger {}
    }
}
