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

import com.ritense.resource.domain.MetadataType
import com.ritense.resource.service.TemporaryResourceStorageService
import com.ritense.smartdocuments.domain.DocumentFormatOption
import com.ritense.valtimoplugins.xential.autoconfiguration.XentialCallbackProperties
import com.ritense.valtimoplugins.xential.domain.DocumentCallbackOutcome
import com.ritense.valtimoplugins.xential.domain.DocumentCreatedMessage
import com.ritense.valtimoplugins.xential.domain.FileFormat
import com.ritense.valtimoplugins.xential.domain.GenerateDocumentResult
import com.ritense.valtimoplugins.xential.domain.XentialDocumentProperties
import com.ritense.valtimoplugins.xential.domain.XentialToken
import com.ritense.valtimoplugins.xential.repository.XentialTokenRepository
import com.rotterdam.esb.xential.api.DefaultApi
import com.rotterdam.esb.xential.model.Sjabloondata
import io.github.oshai.kotlinlogging.KotlinLogging
import org.operaton.bpm.engine.RuntimeService
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID

class DocumentGenerationService(
    private val xentialTokenRepository: XentialTokenRepository,
    private val temporaryResourceStorageService: TemporaryResourceStorageService,
    private val runtimeService: RuntimeService,
    private val callbackVerificationService: XentialCallbackVerificationService,
    private val callbackRateLimiter: XentialCallbackRateLimiter,
    private val callbackProperties: XentialCallbackProperties,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun generateDocument(
        api: DefaultApi,
        processId: UUID,
        xentialGebruikersId: String,
        sjabloonId: String,
        xentialDocumentProperties: XentialDocumentProperties,
    ): GenerateDocumentResult {
        logger.info { "Generating xential document" }
        requireNotNull(xentialDocumentProperties.fileFormat) {
            "fileFormat is required"
        }
        requireNotNull(xentialDocumentProperties.content) {
            "content is required"
        }
        val result =
            api.creeerDocument(
                gebruikersId = xentialGebruikersId,
                accepteerOnbekend = false,
                sjabloondata =
                    Sjabloondata(
                        sjabloonId = sjabloonId,
                        bestandsFormaat =
                            Sjabloondata.BestandsFormaat.valueOf(
                                xentialDocumentProperties.fileFormat.name,
                            ),
                        documentkenmerk = xentialDocumentProperties.documentId,
                        sjabloonVulData = xentialDocumentProperties.content,
                    ),
            )
        logger.debug { "xential creeer document response: $result" }

        val now = LocalDateTime.now(clock)
        val xentialToken =
            XentialToken(
                token = UUID.fromString(result.documentCreatieSessieId),
                processId = processId,
                messageName = xentialDocumentProperties.messageName,
                resumeUrl = result.resumeUrl?.toString(),
                createdOn = now,
                expiresOn = now.plus(callbackProperties.tokenTimeToLive),
            )
        xentialTokenRepository.save(xentialToken)
        logger.debug { "persisted document creation session for process ${xentialToken.processId}" }
        logger.info { "ready" }

        return GenerateDocumentResult(
            status = result.status.value,
            resumeUrl = result.resumeUrl?.toString(),
        )
    }

    private fun setMimeType(format: FileFormat): String {
        val mime =
            when (format.toString()) {
                FileFormat.PDF.toString() -> DocumentFormatOption.PDF
                FileFormat.WORD.toString() -> DocumentFormatOption.DOCX
                else -> null
            }

        return mime?.mediaType?.toString() ?: ""
    }

    /**
     * Handles a callback from Xential carrying the content of a generated document.
     *
     * This endpoint cannot be authenticated at the HTTP level, so the checks below are what stand between a
     * genuine callback and a forged one. They are deliberately ordered cheapest-first, and every way of failing
     * to identify a live session collapses into a single [DocumentCallbackOutcome.INVALID_REQUEST] so that the
     * endpoint does not reveal which session ids exist.
     */
    fun onDocumentGenerated(
        message: DocumentCreatedMessage,
        signature: String? = null,
    ): DocumentCallbackOutcome {
        if (!callbackRateLimiter.tryAcquire()) {
            return DocumentCallbackOutcome.RATE_LIMITED
        }
        if (!callbackVerificationService.isCallbackAllowed(message, signature)) {
            return DocumentCallbackOutcome.INVALID_SIGNATURE
        }

        val sessionId =
            parseSessionId(message.documentCreatieSessieId)
                ?: return rejectInvalidRequest("the document creation session id is not a valid UUID")
        val bytes =
            decodePayload(message.data)
                ?: return rejectInvalidRequest("the payload is not valid base64")
        val xentialToken =
            xentialTokenRepository
                .findById(sessionId)
                .orElse(null)
                ?: return rejectInvalidRequest("no document creation session exists for the supplied id")

        if (xentialToken.isExpired(LocalDateTime.now(clock))) {
            // Terminal outcome: consume the session so that an expired id cannot be retried.
            xentialTokenRepository.delete(xentialToken)
            return rejectInvalidRequest("the document creation session has expired")
        }

        logger.info { "Retrieved content from Xential callback, type: ${message.formaat}" }

        ByteArrayInputStream(bytes).use { inputStream ->
            val metadata =
                mapOf(
                    MetadataType.FILE_NAME.key to "${xentialToken.processId}-${xentialToken.messageName}.tmp",
                    MetadataType.CONTENT_TYPE.key to setMimeType(message.formaat),
                )
            temporaryResourceStorageService.store(inputStream, metadata).let { resourceId ->
                logger.info { "Stored temporary resource with id: $resourceId" }
                runtimeService
                    .createMessageCorrelation(xentialToken.messageName)
                    .processInstanceId(xentialToken.processId.toString())
                    .setVariable("xentialResourceId", resourceId)
                    .correlateAllWithResult()
                    .also { correlationResults ->
                        logger.info {
                            "Correlated message '${xentialToken.messageName}' to ${correlationResults.size} execution(s)"
                        }
                    }
            }
        }

        // The callback for this session has now been handled, so the session is spent whether or not the
        // correlation matched anything. Deleting only on a match would leave a token replayable indefinitely.
        xentialTokenRepository.delete(xentialToken)
        logger.debug { "Deleted document creation session for process ${xentialToken.processId}" }

        return DocumentCallbackOutcome.PROCESSED
    }

    /**
     * Logs the real reason at DEBUG and returns the single outcome shared by every invalid request, so that the
     * caller cannot tell a malformed id from an unknown or expired one.
     */
    private fun rejectInvalidRequest(reason: String): DocumentCallbackOutcome {
        logger.debug { "Rejected Xential callback: $reason" }
        return DocumentCallbackOutcome.INVALID_REQUEST
    }

    private fun parseSessionId(documentCreatieSessieId: String?): UUID? =
        documentCreatieSessieId?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun decodePayload(data: String?): ByteArray? =
        data?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() }

//    private fun resolveTemplateData(
//        templateData: Array<TemplateDataEntry>,
//        execution: DelegateExecution
//    ): Map<String, Any?> {
//        val placeHolderValueMap = valueResolverService.resolveValues(
//            execution.processInstanceId,
//            execution,
//            templateData.map { it.value }.toList()
//        )
//        return templateData.associate { it.key to placeHolderValueMap.getOrDefault(it.value, null) }
//    }
//
    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
