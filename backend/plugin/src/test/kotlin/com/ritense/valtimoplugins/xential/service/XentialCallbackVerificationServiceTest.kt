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

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.plugin.service.PluginService
import com.ritense.valtimoplugins.xential.BaseTest
import com.ritense.valtimoplugins.xential.autoconfiguration.XentialCallbackProperties
import com.ritense.valtimoplugins.xential.domain.CallbackVerificationMode
import com.ritense.valtimoplugins.xential.domain.DocumentCreatedMessage
import com.ritense.valtimoplugins.xential.domain.FileFormat
import com.ritense.valtimoplugins.xential.plugin.XentialPlugin
import com.ritense.valtimoplugins.xential.service.XentialCallbackVerificationService.Companion.encodeHex
import com.ritense.valtimoplugins.xential.service.XentialCallbackVerificationService.Companion.sign
import com.ritense.valueresolver.ValueResolverService
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.MessageDigest
import java.util.UUID

class XentialCallbackVerificationServiceTest : BaseTest() {
    private val pluginService: PluginService = mock()
    private val sessionId = UUID.randomUUID().toString()
    private val payload = "cGF5bG9hZA=="

    @BeforeEach
    fun setUp() {
        configureSecret(SECRET)
    }

    @Test
    fun `should accept a correctly signed callback`() {
        val service = service(CallbackVerificationMode.ENFORCE)

        assertTrue(service.isCallbackAllowed(message(), validSignature()))
    }

    @Test
    fun `should reject a callback without a signature when enforcing`() {
        val service = service(CallbackVerificationMode.ENFORCE)

        assertFalse(service.isCallbackAllowed(message(), null))
        assertFalse(service.isCallbackAllowed(message(), ""))
    }

    @Test
    fun `should reject a callback with a wrong signature when enforcing`() {
        val service = service(CallbackVerificationMode.ENFORCE)
        val wrongSignature = encodeHex(sign("another-secret", sessionId, payload))

        assertFalse(service.isCallbackAllowed(message(), wrongSignature))
    }

    @Test
    fun `should reject a signature that is not valid hex when enforcing`() {
        val service = service(CallbackVerificationMode.ENFORCE)

        assertFalse(service.isCallbackAllowed(message(), "not-hex"))
        assertFalse(service.isCallbackAllowed(message(), "abc"))
    }

    @Test
    fun `should accept a signature regardless of hex casing`() {
        val service = service(CallbackVerificationMode.ENFORCE)

        assertTrue(service.isCallbackAllowed(message(), validSignature().uppercase()))
    }

    @Test
    fun `should reject a signature captured for a different session when enforcing`() {
        val service = service(CallbackVerificationMode.ENFORCE)
        val signatureForOtherSession = encodeHex(sign(SECRET, UUID.randomUUID().toString(), payload))

        assertFalse(service.isCallbackAllowed(message(), signatureForOtherSession))
    }

    @Test
    fun `should reject a signature captured for a different payload when enforcing`() {
        val service = service(CallbackVerificationMode.ENFORCE)
        val signatureForOtherPayload = encodeHex(sign(SECRET, sessionId, "b3RoZXI="))

        assertFalse(service.isCallbackAllowed(message(), signatureForOtherPayload))
    }

    @Test
    fun `should reject every callback when enforcing without a configured secret`() {
        configureSecret(null)
        val service = service(CallbackVerificationMode.ENFORCE)

        assertFalse(service.isCallbackAllowed(message(), validSignature()))
    }

    @Test
    fun `should allow an unverifiable callback when only logging`() {
        val service = service(CallbackVerificationMode.LOG_ONLY)

        assertTrue(service.isCallbackAllowed(message(), null))
        assertTrue(service.isCallbackAllowed(message(), "deadbeef"))
    }

    @Test
    fun `should allow a callback when the plugin configuration cannot be resolved`() {
        whenever(pluginService.createInstance(eq(XentialPlugin::class.java), any<(Any) -> Boolean>()))
            .thenThrow(IllegalStateException("no configuration"))
        val service = service(CallbackVerificationMode.LOG_ONLY)

        assertTrue(service.isCallbackAllowed(message(), validSignature()))
    }

    /**
     * Asserts on the implementation rather than on timing, which would be inherently flaky.
     *
     * A content-dependent comparison would leak the expected signature one byte at a time, so the check that
     * matters is that [MessageDigest.isEqual] is genuinely the comparison being used. This is asserted against
     * the compiled class, because no black-box behaviour can distinguish a constant-time comparison from a
     * short-circuiting one that happens to return the same answers.
     */
    @Test
    fun `should compare signatures in constant time`() {
        val compiledClass =
            checkNotNull(
                XentialCallbackVerificationService::class.java
                    .getResourceAsStream("/${SERVICE_CLASS_RESOURCE}"),
            ) { "Could not read the compiled class to verify the comparison used" }
        val bytecode = compiledClass.use { it.readBytes() }.toString(Charsets.ISO_8859_1)

        assertTrue(
            bytecode.contains("java/security/MessageDigest") && bytecode.contains("isEqual"),
            "Signature comparison must use MessageDigest.isEqual; a naive equality check would reintroduce a " +
                "timing oracle on the signature.",
        )
    }

    private fun service(mode: CallbackVerificationMode) =
        XentialCallbackVerificationService(
            pluginService,
            XentialCallbackProperties(verificationMode = mode),
        )

    private fun configureSecret(secret: String?) {
        val plugin =
            XentialPlugin(
                documentGenerationService = mock(),
                esbClient = mock(),
                objectMapper = ObjectMapper(),
                valueResolverService = mock<ValueResolverService>(),
                xentialSjablonenService = mock(),
            ).apply { callbackSecret = secret }
        whenever(pluginService.createInstance(eq(XentialPlugin::class.java), any<(Any) -> Boolean>()))
            .thenReturn(plugin)
    }

    private fun validSignature() = encodeHex(sign(SECRET, sessionId, payload))

    private fun message() =
        DocumentCreatedMessage(
            taakapplicatie = "valtimo",
            gebruiker = "gebruiker",
            documentCreatieSessieId = sessionId,
            formaat = FileFormat.PDF,
            documentkenmerk = "kenmerk",
            data = payload,
        )

    private companion object {
        const val SECRET = "a-shared-secret"
        const val SERVICE_CLASS_RESOURCE =
            "com/ritense/valtimoplugins/xential/service/XentialCallbackVerificationService.class"
    }
}
