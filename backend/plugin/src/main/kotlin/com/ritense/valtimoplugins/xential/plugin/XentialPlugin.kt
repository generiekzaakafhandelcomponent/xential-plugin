/*
 * Copyright 2015-2022 Ritense BV, the Netherlands.
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

import com.ritense.plugin.annotation.Plugin
import com.ritense.plugin.annotation.PluginAction
import com.ritense.plugin.annotation.PluginActionProperty
import com.ritense.plugin.annotation.PluginProperty
import com.ritense.processlink.domain.ActivityTypeWithEventName
import com.ritense.valtimoplugins.xential.domain.HttpClientProperties
import com.ritense.valtimoplugins.xential.domain.FileFormat
import com.ritense.valtimoplugins.xential.domain.GenerateDocumentProperties
import com.ritense.valtimoplugins.xential.plugin.XentialPlugin.Companion.PLUGIN_KEY
import com.ritense.valtimoplugins.xential.service.DocumentGenerationService
import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateExecution
import java.io.File
import java.net.URI
import java.util.UUID

@Plugin(
    key = PLUGIN_KEY,
    title = "Xential Plugin",
    description = "handle xentail requests"
)
@Suppress("UNUSED")
class XentialPlugin(
    private val documentGenerationService: DocumentGenerationService
) {

    @PluginProperty(key = "applicationName", secret = false, required = true)
    private lateinit var applicationName: String

    @PluginProperty(key = "applicationPassword", secret = true, required = true)
    private lateinit var applicationPassword: String

    @PluginProperty(key = "baseUrl", secret = false, required = true)
    lateinit var baseUrl: URI

    @PluginProperty(key = "serverCertificateFilename", secret = false, required = true)
    private lateinit var serverCertificateFilename: String

    @PluginProperty(key = "clientPrivateKeyFilename", secret = false, required = false)
    var clientPrivateKeyFilename: String? = null

    @PluginProperty(key = "clientCertificateFilename", secret = false, required = false)
    var clientCertificateFilename: String? = null

    @PluginAction(
        key = "generate-document",
        title = "Generate document",
        description = "Generate a document using xential.",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START]
    )
    fun generateDocument(
        @PluginActionProperty templateId: UUID,
        @PluginActionProperty fileFormat: FileFormat,
        @PluginActionProperty documentId: String,
        @PluginActionProperty messageName: String,
        @PluginActionProperty contentProcessVariable: String,
        execution: DelegateExecution
    ) {
        val generateDocumentProperties = GenerateDocumentProperties(
            templateId,
            fileFormat,
            documentId,
            messageName
        )

        val httpClientProperties = HttpClientProperties(
            applicationName,
            applicationPassword,
            baseUrl,
            File(serverCertificateFilename),
            clientPrivateKeyFilename?.let{File(it)},
            clientCertificateFilename?.let {File(it)}
        )

        documentGenerationService.generateDocument(
            httpClientProperties,
            UUID.fromString(execution.processInstanceId),
            generateDocumentProperties,
            contentProcessVariable,
            execution
        )
    }

    @PluginAction(
        key = "prepare-content",
        title = "Prepare content",
        description = "Prepare content for xential.",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START]
    )
    fun prepareContent(
        @PluginActionProperty resultProcessVariableName: String,
        @PluginActionProperty verzendAdresData: Array<TemplateDataEntry>,
        @PluginActionProperty colofonData: Array<TemplateDataEntry>,
        @PluginActionProperty creatieData: Array<TemplateDataEntry>,
        execution: DelegateExecution
    ) {
        try {
            documentGenerationService.generateContent(
                creatieData,
                colofonData,
                verzendAdresData,
                execution
            ).let{
                execution.processInstance.setVariable(
                    resultProcessVariableName, it
                )
            }
        } catch (e: Exception) {
            logger.info("Exiting scope due to nested error.", e)
            return
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        const val PLUGIN_KEY = "xential"
    }

}
