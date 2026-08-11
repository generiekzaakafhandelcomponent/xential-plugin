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

import com.ritense.plugin.service.PluginService
import com.ritense.valtimoplugins.xential.autoconfiguration.XentialCallbackProperties
import com.ritense.valtimoplugins.xential.domain.CallbackVerificationMode
import com.ritense.valtimoplugins.xential.domain.DocumentCreatedMessage
import com.ritense.valtimoplugins.xential.plugin.XentialPlugin
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Verifies that an incoming Xential document callback was produced by a party holding the shared secret.
 *
 * The callback endpoint is necessarily unauthenticated at the HTTP level - it is called by the Xential ESB, not
 * by a logged-in user - so the signature is what distinguishes a genuine callback from a forged one.
 *
 * The expected signature is `HMAC-SHA256(callbackSecret, documentCreatieSessieId || data)`, hex encoded, sent in
 * the [SIGNATURE_HEADER] header.
 */
class XentialCallbackVerificationService(
    private val pluginService: PluginService,
    private val callbackProperties: XentialCallbackProperties,
) {
    /**
     * Verifies [signature] against [message].
     *
     * @return `true` when the callback may be processed. In [CallbackVerificationMode.LOG_ONLY] this is always
     * `true` - the outcome is only logged - which is what makes it safe to deploy ahead of the sending side.
     */
    fun isCallbackAllowed(
        message: DocumentCreatedMessage,
        signature: String?,
    ): Boolean {
        val verified = verify(message, signature)
        if (verified) {
            return true
        }
        return when (callbackProperties.verificationMode) {
            CallbackVerificationMode.LOG_ONLY -> {
                logger.warn {
                    "Xential callback signature could not be verified. The callback is still being processed " +
                        "because valtimo.xential.callback.verification-mode is LOG_ONLY. Configure the sending " +
                        "side to sign its callbacks and then switch to ENFORCE."
                }
                true
            }

            CallbackVerificationMode.ENFORCE -> {
                logger.warn { "Rejected Xential callback: signature missing or invalid." }
                false
            }
        }
    }

    private fun verify(
        message: DocumentCreatedMessage,
        signature: String?,
    ): Boolean {
        val secret = callbackSecret()
        if (secret.isNullOrBlank()) {
            logger.warn {
                "No callbackSecret is configured on the Xential plugin, so incoming callbacks cannot be " +
                    "verified. Configure it on the plugin configuration and on the sending side."
            }
            return false
        }
        if (signature.isNullOrBlank()) {
            return false
        }
        val providedSignature = decodeHex(signature) ?: return false
        val expectedSignature = sign(secret, message.documentCreatieSessieId, message.data)

        // Constant-time comparison: a length- or content-dependent comparison would leak the expected
        // signature one byte at a time.
        return MessageDigest.isEqual(expectedSignature, providedSignature)
    }

    private fun callbackSecret(): String? =
        runCatching {
            pluginService.createInstance(XentialPlugin::class.java) { true }?.callbackSecret
        }.getOrElse { exception ->
            logger.warn(exception) { "Could not resolve the Xential plugin configuration to verify a callback." }
            null
        }

    companion object {
        const val SIGNATURE_HEADER = "X-Xential-Signature"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val HEX_RADIX = 16
        private val logger = KotlinLogging.logger {}

        /**
         * Computes the signature the sending side is expected to send.
         *
         * The session id is folded into the signed material alongside the payload so that a signature captured
         * for one document creation session cannot be replayed against another.
         */
        fun sign(
            secret: String,
            documentCreatieSessieId: String,
            data: String,
        ): ByteArray =
            Mac.getInstance(HMAC_ALGORITHM).let { mac ->
                mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), HMAC_ALGORITHM))
                mac.doFinal((documentCreatieSessieId + data).toByteArray(StandardCharsets.UTF_8))
            }

        /** Renders [signature] the way it is expected to appear in the [SIGNATURE_HEADER] header. */
        fun encodeHex(signature: ByteArray): String = signature.joinToString("") { "%02x".format(it) }

        /** Decodes a hex encoded signature, returning `null` when it is not valid hex. */
        private fun decodeHex(value: String): ByteArray? {
            val trimmed = value.trim()
            if (trimmed.length % 2 != 0 || trimmed.isEmpty()) {
                return null
            }
            return runCatching {
                ByteArray(trimmed.length / 2) { index ->
                    trimmed.substring(index * 2, index * 2 + 2).toInt(HEX_RADIX).toByte()
                }
            }.getOrNull()
        }
    }
}
