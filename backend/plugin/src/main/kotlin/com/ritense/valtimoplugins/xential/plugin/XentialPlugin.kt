/*
 * Copyright 2015-2026 Ritense BV, the Netherlands.
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

package com.ritense.valtimoplugins.xential.plugin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.ritense.plugin.annotation.Plugin
import com.ritense.plugin.annotation.PluginAction
import com.ritense.plugin.annotation.PluginActionProperty
import com.ritense.plugin.annotation.PluginProperty
import com.ritense.processlink.domain.ActivityTypeWithEventName
import com.ritense.valtimoplugins.mtlssslcontext.MTlsSslContext
import com.ritense.valtimoplugins.xential.domain.FileFormat
import com.ritense.valtimoplugins.xential.domain.XentialAccessResult
import com.ritense.valtimoplugins.xential.domain.XentialDocumentProperties
import com.ritense.valtimoplugins.xential.plugin.XentialPlugin.Companion.PLUGIN_KEY
import com.ritense.valtimoplugins.xential.service.DocumentGenerationService
import com.ritense.valtimoplugins.xential.service.OpentunnelEsbClient
import com.ritense.valtimoplugins.xential.service.XentialSjablonenService
import com.ritense.valueresolver.ValueResolverService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.operaton.bpm.engine.delegate.DelegateExecution
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.net.URI
import java.util.UUID

@Plugin(
    key = PLUGIN_KEY,
    title = "Xential Plugin",
    description = "Handle Xential requests",
)
@Suppress("UNUSED")
class XentialPlugin(
    private val esbClient: OpentunnelEsbClient,
    private val documentGenerationService: DocumentGenerationService,
    private val valueResolverService: ValueResolverService,
    private val xentialSjablonenService: XentialSjablonenService,
    private val objectMapper: ObjectMapper,
    private val environment: Environment,
) {
    @PluginProperty(key = "applicationName", secret = false, required = true)
    lateinit var applicationName: String

    @PluginProperty(key = "applicationPassword", secret = true, required = true)
    lateinit var applicationPassword: String

    @PluginProperty(key = "baseUrl", secret = false, required = true)
    lateinit var baseUrl: URI

    @PluginProperty(key = "mTlsSslContextAutoConfigurationId", secret = false, required = true)
    private lateinit var mTlsSslContextAutoConfigurationId: MTlsSslContext

    @PluginAction(
        key = "generate-document",
        title = "Generate document",
        description = "Generate a document using xential.",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun generateDocument(
        @PluginActionProperty xentialDocumentPropertiesVariableName: String,
        @PluginActionProperty xentialData: String,
        @PluginActionProperty xentialSjabloonId: String,
        @PluginActionProperty xentialGebruikersId: String,
        @PluginActionProperty fileFormat: FileFormat,
        execution: DelegateExecution,
    ) {
        val originalProps = getXentialDocumentProperties(execution, xentialDocumentPropertiesVariableName)
        logger.info { "Generating document from template: $xentialSjabloonId for user: $xentialGebruikersId" }
        logger.debug { "> XentialDocumentProperties: $originalProps" }
        logger.debug { "> XentialDate: $xentialData" }

        val xentialSjabloon =
            xentialSjablonenService
                .getTemplateList(
                    gebruikersId = xentialGebruikersId,
                    sjabloongroepId = originalProps.xentialTemplateGroupId.toString(),
                ).sjablonen
                .single { it.id == xentialSjabloonId }
        logger.debug { "> Template: $xentialSjabloon" }

        val resolvedValues = resolveValuesFor(execution, mapOf("content" to xentialData))
        val modifiedProps =
            originalProps.copy(
                xentialTemplateName = xentialSjabloon.naam,
                fileFormat = fileFormat,
                content = resolvedValues["content"] as String,
            )
        storeXentialDocumentProperties(execution, xentialDocumentPropertiesVariableName, modifiedProps)

        documentGenerationService
            .generateDocument(
                api = esbClient.documentApi(restClient(mTlsSslContextAutoConfigurationId)),
                processId = UUID.fromString(execution.processInstanceId),
                xentialGebruikersId = xentialGebruikersId,
                sjabloonId = xentialSjabloonId,
                xentialDocumentProperties = modifiedProps,
            ).let { result ->
                execution.setVariable("xentialStatus", result.status)
                result.resumeUrl?.let {
                    execution.setVariable("xentialResumeUrl", it)
                }
            }
    }

    @PluginAction(
        key = "generate-document-bb",
        title = "Generate document with bb",
        description = "Generate a document using xential with bb.",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun generateDocumentBB(
        @PluginActionProperty textContent: String,
        @PluginActionProperty sjabloonId: String,
        @PluginActionProperty xentialGebruikersId: String,
        @PluginActionProperty fileFormat: FileFormat,
        @PluginActionProperty sjabloonGroepId: String,
        @PluginActionProperty messageName: String,
        execution: DelegateExecution,
    ) {
        logger.info { "Generating document from template: $sjabloonId for user: $xentialGebruikersId" }
        logger.debug { "> XentialDate: $textContent" }

        val xentialSjabloon =
            xentialSjablonenService
                .getTemplateList(
                    gebruikersId = xentialGebruikersId,
                    sjabloongroepId = sjabloonGroepId,
                ).sjablonen
                .single { it.id == sjabloonId }
        logger.debug { "> Template: $xentialSjabloon" }

        val xentialDocumentProperties = XentialDocumentProperties(
            xentialTemplateGroupId = UUID.fromString(sjabloonGroepId),
            xentialTemplateName = xentialSjabloon.naam,
            fileFormat = fileFormat,
            documentId = "documentId",
            messageName = messageName,
            content = textContent,
        )

        documentGenerationService
            .generateDocument(
                api = esbClient.documentApi(restClient(mTlsSslContextAutoConfigurationId)),
                processId = UUID.fromString(execution.processInstanceId),
                xentialGebruikersId = xentialGebruikersId,
                sjabloonId = sjabloonId,
                xentialDocumentProperties = xentialDocumentProperties,
            ).let { result ->
                execution.setVariable("xentialStatus", result.status)
                result.resumeUrl?.let {
                    execution.setVariable("xentialResumeUrl", it)
                }
            }
    }

    @PluginAction(
        key = "validate-xential-toegang",
        title = "Valideer xential toegang",
        description = "Valideer toegang tot xential gebasseerd op configuratie proceskoppeling.",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun validateAccess(
        @PluginActionProperty toegangResultaatId: String,
        @PluginActionProperty xentialGebruikersId: String,
        @PluginActionProperty xentialDocumentPropertiesVariableName: String,
        execution: DelegateExecution,
    ) {
        val props = getXentialDocumentProperties(execution, xentialDocumentPropertiesVariableName)
        logger.info {
            "Validate access for user: $xentialGebruikersId on template group: ${props.xentialTemplateGroupId}"
        }
        xentialSjablonenService
            .testAccessToSjabloonGroep(
                gebruikersId = xentialGebruikersId,
                sjabloonGroepId = props.xentialTemplateGroupId.toString(),
            ).let { accessResult ->
                execution.processInstance.setVariable(
                    toegangResultaatId,
                    objectMapper.convertValue(accessResult),
                )
            }
    }

    @PluginAction(
        key = "set-sjabloon-group-id",
        title = "Set sjabloon group id",
        description = "Zet sjabloon groep id op basis van zaaptype naam en valideer toegang tot xential.",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun setSjabloonGroepId(
        @PluginActionProperty toegangResultaatId: String,
        @PluginActionProperty xentialGebruikersId: String,
        @PluginActionProperty sjabloonGroepNaam: String,
        execution: DelegateExecution,
    ) {
        fun storeResult(result: XentialAccessResult) =
            execution.processInstance.setVariable(toegangResultaatId, objectMapper.convertValue(result))

        try {
            val sjabloonGroupId = sjabloonGroepUuid(xentialGebruikersId, sjabloonGroepNaam)
                ?: run {
                    logger.debug { "No sjabloongroep found with name: $sjabloonGroepNaam for user: $xentialGebruikersId" }
                    storeResult(
                        XentialAccessResult(
                            statusCode = "404",
                            statusMessage = "No sjabloon group found with name: $sjabloonGroepNaam",
                        ),
                    )
                    return
                }

            val accessResult = xentialSjablonenService.testAccessToSjabloonGroep(
                gebruikersId = xentialGebruikersId,
                sjabloonGroepId = sjabloonGroupId,
            )
            accessResult.sjabloonGroepId = sjabloonGroupId
            storeResult(accessResult)
        } catch (e: RestClientException) {
            logger.error(e) {
                "Xential request failed while setting sjabloon group id for name: $sjabloonGroepNaam"
            }
            storeResult(
                XentialAccessResult(
                    statusCode = (e as? RestClientResponseException)?.statusCode?.value()?.toString() ?: "503",
                    statusMessage = (e as? RestClientResponseException)?.statusText ?: e.message ?: "Could not reach Xential",
                ),
            )
        }
    }

    private fun sjabloonGroepUuid(xentialGebruikersId: String, caseType: String): String? {
        val sjabloongroepen = xentialSjablonenService
            .getTemplateList(xentialGebruikersId, null)
            .sjabloongroepen
        return sjabloongroepen
            .firstOrNull { it.naam == caseType }
            ?.id
            ?: if (environment.acceptsProfiles(Profiles.of("dev"))) {
                logger.debug { "Dev profile active, falling back to first sjabloongroep for: $caseType" }
                sjabloongroepen.firstOrNull()?.id
            } else {
                null
            }
    }

    @PluginAction(
        key = "prepare-content",
        title = "Prepare content",
        description = "Prepare content for xential with template.",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun prepareContent(
        @PluginActionProperty xentialDocumentPropertiesVariableName: String,
        @PluginActionProperty firstTemplateGroupId: UUID,
        @PluginActionProperty secondTemplateGroupId: UUID?,
        @PluginActionProperty thirdTemplateGroupId: UUID?,
        @PluginActionProperty eventMessageName: String,
        @PluginActionProperty test: String?,
        execution: DelegateExecution,
    ) {
        try {
            val xentialDocumentProperties =
                XentialDocumentProperties(
                    xentialTemplateGroupId = thirdTemplateGroupId ?: secondTemplateGroupId ?: firstTemplateGroupId,
                    fileFormat = null,
                    documentId = "documentId",
                    messageName = eventMessageName,
                    content = null,
                    xentialTemplateName = null,
                )
            storeXentialDocumentProperties(execution, xentialDocumentPropertiesVariableName, xentialDocumentProperties)
        } catch (e: Exception) {
            logger.error { "Exiting scope due to nested error. $e" }
            return
        }
    }

    private fun getXentialDocumentProperties(
        execution: DelegateExecution,
        variableName: String,
    ): XentialDocumentProperties = objectMapper.convertValue(execution.getVariable(variableName))

    private fun storeXentialDocumentProperties(
        execution: DelegateExecution,
        variableName: String,
        properties: XentialDocumentProperties,
    ) {
        execution.setVariable(variableName, objectMapper.convertValue<Map<String, Any>>(properties))
    }

    private fun isResolvableValue(value: String): Boolean =
        value.isNotBlank() && (
            value.startsWith("case:") ||
                value.startsWith("doc:") ||
                value.startsWith("template:") ||
                value.startsWith("pv:")
        )

    private fun resolveValuesFor(
        execution: DelegateExecution,
        params: Map<String, Any?>,
    ): Map<String, Any?> {
        val resolvedValues =
            params
                .filter {
                    if (it.value is String) {
                        isResolvableValue(it.value as String)
                    } else {
                        false
                    }
                }.let { filteredParams ->
                    logger.debug { "Trying to resolve values for: $filteredParams" }
                    valueResolverService
                        .resolveValues(
                            execution.processInstanceId,
                            execution,
                            filteredParams.map { it.value as String },
                        ).let { resolvedValues ->
                            logger.debug { "Resolved values: $resolvedValues" }
                            filteredParams.toMutableMap().apply {
                                this.entries.forEach { (key, value) ->
                                    this[key] = resolvedValues[value]
                                }
                            }
                        }
                }
        return params
            .toMutableMap()
            .apply {
                this.putAll(resolvedValues)
            }.toMap()
    }

    private fun restClient(mTlsSslContextAutoConfiguration: MTlsSslContext?): RestClient =
        esbClient.createRestClient(
            baseUrl = baseUrl.toString(),
            applicationName = applicationName,
            applicationPassword = applicationPassword,
            sslContext = mTlsSslContextAutoConfiguration?.createSslContext(),
        )

    companion object {
        private val logger = KotlinLogging.logger { }
        const val PLUGIN_KEY = "xential"
    }
}
