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

package com.ritense.valtimoplugins.xential.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.ritense.resource.service.TemporaryResourceStorageService
import com.ritense.valtimoplugins.xential.BaseTest
import com.ritense.valtimoplugins.xential.autoconfiguration.XentialCallbackProperties
import com.ritense.valtimoplugins.xential.domain.DocumentCallbackOutcome
import com.ritense.valtimoplugins.xential.domain.DocumentCreatedMessage
import com.ritense.valtimoplugins.xential.domain.FileFormat
import com.ritense.valtimoplugins.xential.domain.XentialDocumentProperties
import com.ritense.valtimoplugins.xential.domain.XentialToken
import com.ritense.valtimoplugins.xential.repository.XentialTokenRepository
import com.rotterdam.esb.xential.api.DefaultApi
import com.rotterdam.esb.xential.model.DocumentCreatieResultaat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.operaton.bpm.engine.RuntimeService
import org.operaton.bpm.engine.runtime.MessageCorrelationBuilder
import org.operaton.bpm.engine.runtime.MessageCorrelationResult
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class DocumentGenerationServiceTest : BaseTest() {
    private val xentialTokenRepository: XentialTokenRepository = mock()
    private val temporaryResourceStorageService: TemporaryResourceStorageService = mock()
    private val runtimeService: RuntimeService = mock()
    private val callbackVerificationService: XentialCallbackVerificationService = mock()
    private val callbackRateLimiter: XentialCallbackRateLimiter = mock()

    private val now: Instant = Instant.parse("2026-01-01T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val localNow: LocalDateTime = LocalDateTime.ofInstant(now, ZoneOffset.UTC)

    private val sessionId = UUID.randomUUID()
    private val processId = UUID.randomUUID()

    @Test
    fun shouldGenerateDocument() {
        val defaultApi: DefaultApi = mock()
        val creatieResultaat =
            DocumentCreatieResultaat(
                documentCreatieSessieId = sessionId.toString(),
                status = DocumentCreatieResultaat.Status.VOLTOOID,
                resumeUrl = null,
            )
        whenever(defaultApi.creeerDocument(any(), any(), any())).thenReturn(creatieResultaat)

        service().generateDocument(
            api = defaultApi,
            processId = processId,
            xentialGebruikersId = "xentialGebruikersId",
            sjabloonId = UUID.randomUUID().toString(),
            xentialDocumentProperties =
                XentialDocumentProperties(
                    xentialTemplateGroupId = UUID.randomUUID(),
                    fileFormat = FileFormat.PDF,
                    documentId = "mijn-kenmerk",
                    messageName = MESSAGE_NAME,
                    content = "voorbeeld data",
                    xentialTemplateName = "xentialTemplateName",
                ),
        )

        argumentCaptor<XentialToken>().apply {
            verify(xentialTokenRepository).save(capture())
            assertEquals(sessionId, firstValue.token)
            assertNotNull(firstValue.expiresOn)
            assertEquals(localNow.plusDays(TOKEN_TTL_DAYS), firstValue.expiresOn)
        }
    }

    @Test
    fun `should process a callback with an allowed signature`() {
        givenCallbackAllowed()
        givenLiveToken()
        givenCorrelationMatches(1)

        val outcome = service().onDocumentGenerated(message(), SIGNATURE)

        assertEquals(DocumentCallbackOutcome.PROCESSED, outcome)
        verify(temporaryResourceStorageService).store(any(), any())
    }

    @Test
    fun `should reject a callback whose signature is not allowed`() {
        givenCallbackRateLimitAvailable()
        whenever(callbackVerificationService.isCallbackAllowed(any(), any())).thenReturn(false)

        val outcome = service().onDocumentGenerated(message(), "wrong")

        assertEquals(DocumentCallbackOutcome.INVALID_SIGNATURE, outcome)
        // The signature is checked before the session is looked up, so a forged callback never reaches the
        // database and cannot be used to probe which session ids exist.
        verify(xentialTokenRepository, never()).findById(any())
        verify(temporaryResourceStorageService, never()).store(any(), any())
    }

    @Test
    fun `should reject a callback when the rate limit is exhausted`() {
        whenever(callbackRateLimiter.tryAcquire()).thenReturn(false)

        val outcome = service().onDocumentGenerated(message(), SIGNATURE)

        assertEquals(DocumentCallbackOutcome.RATE_LIMITED, outcome)
        verify(callbackVerificationService, never()).isCallbackAllowed(any(), any())
        verify(xentialTokenRepository, never()).findById(any())
    }

    @Test
    fun `should return the same outcome for malformed unknown and expired session ids`() {
        givenCallbackAllowed()

        val malformed = service().onDocumentGenerated(message(documentCreatieSessieId = "not-a-uuid"), SIGNATURE)

        whenever(xentialTokenRepository.findById(sessionId)).thenReturn(Optional.empty())
        val unknown = service().onDocumentGenerated(message(), SIGNATURE)

        whenever(xentialTokenRepository.findById(sessionId))
            .thenReturn(Optional.of(token(expiresOn = localNow.minusDays(1))))
        val expired = service().onDocumentGenerated(message(), SIGNATURE)

        // An unauthenticated caller must not be able to tell these three apart - that is what would turn the
        // endpoint into a session-id validity oracle.
        assertEquals(DocumentCallbackOutcome.INVALID_REQUEST, malformed)
        assertEquals(DocumentCallbackOutcome.INVALID_REQUEST, unknown)
        assertEquals(DocumentCallbackOutcome.INVALID_REQUEST, expired)
    }

    @Test
    fun `should reject an undecodable payload with the same outcome`() {
        givenCallbackAllowed()

        val outcome = service().onDocumentGenerated(message(data = "not-base64!!"), SIGNATURE)

        assertEquals(DocumentCallbackOutcome.INVALID_REQUEST, outcome)
        verify(temporaryResourceStorageService, never()).store(any(), any())
    }

    @Test
    fun `should not parse a malformed session id by throwing out of the handler`() {
        givenCallbackAllowed()

        // Previously UUID.fromString threw straight out of the handler, surfacing as a server error.
        val outcome = service().onDocumentGenerated(message(documentCreatieSessieId = "not-a-uuid"), SIGNATURE)

        assertEquals(DocumentCallbackOutcome.INVALID_REQUEST, outcome)
    }

    @Test
    fun `should delete an expired token`() {
        givenCallbackAllowed()
        val expiredToken = token(expiresOn = localNow.minusSeconds(1))
        whenever(xentialTokenRepository.findById(sessionId)).thenReturn(Optional.of(expiredToken))

        service().onDocumentGenerated(message(), SIGNATURE)

        verify(xentialTokenRepository).delete(expiredToken)
        verify(temporaryResourceStorageService, never()).store(any(), any())
    }

    @Test
    fun `should treat a token without an expiry as still valid`() {
        givenCallbackAllowed()
        whenever(xentialTokenRepository.findById(sessionId)).thenReturn(Optional.of(token(expiresOn = null)))
        givenCorrelationMatches(1)

        assertEquals(DocumentCallbackOutcome.PROCESSED, service().onDocumentGenerated(message(), SIGNATURE))
    }

    @Test
    fun `should delete the token after a successful correlation`() {
        givenCallbackAllowed()
        val liveToken = givenLiveToken()
        givenCorrelationMatches(1)

        service().onDocumentGenerated(message(), SIGNATURE)

        verify(xentialTokenRepository).delete(liveToken)
    }

    @Test
    fun `should delete the token even when the correlation matches nothing`() {
        givenCallbackAllowed()
        val liveToken = givenLiveToken()
        givenCorrelationMatches(0)

        val outcome = service().onDocumentGenerated(message(), SIGNATURE)

        // A correlation that matched nothing is still a terminal outcome; leaving the token behind would keep
        // it replayable indefinitely.
        assertEquals(DocumentCallbackOutcome.PROCESSED, outcome)
        verify(xentialTokenRepository).delete(liveToken)
    }

    @Test
    fun `should not log the session token at info level or above`() {
        givenCallbackAllowed()
        givenLiveToken()
        givenCorrelationMatches(1)

        val appender = ListAppender<ILoggingEvent>().apply { start() }
        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.addAppender(appender)
        try {
            service().onDocumentGenerated(message(), SIGNATURE)
        } finally {
            rootLogger.detachAppender(appender)
            appender.stop()
        }

        val loggedAtInfoOrAbove =
            appender.list
                .filter { it.level.isGreaterOrEqual(Level.INFO) }
                .joinToString("\n") { it.formattedMessage }

        assertFalse(
            loggedAtInfoOrAbove.contains(sessionId.toString()),
            "The document creation session token is bearer-equivalent and must not be logged. Found in:\n" +
                loggedAtInfoOrAbove,
        )
    }

    private fun service() =
        DocumentGenerationService(
            xentialTokenRepository,
            temporaryResourceStorageService,
            runtimeService,
            callbackVerificationService,
            callbackRateLimiter,
            XentialCallbackProperties(tokenTimeToLive = Duration.ofDays(TOKEN_TTL_DAYS)),
            clock,
        )

    private fun givenCallbackRateLimitAvailable() {
        whenever(callbackRateLimiter.tryAcquire()).thenReturn(true)
    }

    private fun givenCallbackAllowed() {
        givenCallbackRateLimitAvailable()
        whenever(callbackVerificationService.isCallbackAllowed(any(), any())).thenReturn(true)
    }

    private fun givenLiveToken(): XentialToken =
        token(expiresOn = localNow.plusDays(1)).also {
            whenever(xentialTokenRepository.findById(sessionId)).thenReturn(Optional.of(it))
        }

    private fun givenCorrelationMatches(matches: Int) {
        val correlationBuilder: MessageCorrelationBuilder = mock()
        whenever(runtimeService.createMessageCorrelation(eq(MESSAGE_NAME))).thenReturn(correlationBuilder)
        whenever(correlationBuilder.processInstanceId(any())).thenReturn(correlationBuilder)
        whenever(correlationBuilder.setVariable(any(), any())).thenReturn(correlationBuilder)
        whenever(correlationBuilder.correlateAllWithResult())
            .thenReturn(List(matches) { mock<MessageCorrelationResult>() })
        whenever(temporaryResourceStorageService.store(any(), any())).thenReturn("resource-id")
    }

    private fun token(expiresOn: LocalDateTime?) =
        XentialToken(
            token = sessionId,
            processId = processId,
            messageName = MESSAGE_NAME,
            resumeUrl = null,
            createdOn = localNow.minusHours(1),
            expiresOn = expiresOn,
        )

    private fun message(
        documentCreatieSessieId: String = sessionId.toString(),
        data: String = "cGF5bG9hZA==",
    ) = DocumentCreatedMessage(
        taakapplicatie = "valtimo",
        gebruiker = "gebruiker",
        documentCreatieSessieId = documentCreatieSessieId,
        formaat = FileFormat.PDF,
        documentkenmerk = "kenmerk",
        data = data,
    )

    private companion object {
        const val MESSAGE_NAME = "messageName"
        const val SIGNATURE = "a-signature"
        const val TOKEN_TTL_DAYS = 7L
    }
}
