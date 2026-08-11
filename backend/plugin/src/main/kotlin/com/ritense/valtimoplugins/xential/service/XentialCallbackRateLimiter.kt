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

import com.ritense.valtimoplugins.xential.autoconfiguration.XentialCallbackProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock

/**
 * A fixed window rate limiter for the Xential document callback endpoint.
 *
 * The limit is applied to the endpoint as a whole rather than per caller. The endpoint serves a single ESB
 * integration, so a global ceiling is both sufficient and harder to evade than a per-address limit, which an
 * attacker could sidestep by rotating source addresses and which would need care to read a forwarded-for header
 * correctly behind an ingress.
 *
 * This is a per-instance limiter; it is a guard against an unauthenticated caller exhausting resources or
 * grinding through session ids, not a distributed quota.
 */
class XentialCallbackRateLimiter(
    private val callbackProperties: XentialCallbackProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    private var windowStart: Long? = null
    private var callsInWindow = 0

    /**
     * Records a call and reports whether it is within the configured limit.
     *
     * @return `true` when the call may proceed, `false` when the limit for the current window is exhausted.
     */
    @Synchronized
    fun tryAcquire(): Boolean {
        val windowLength = callbackProperties.rateLimitWindow.toMillis()
        if (windowLength <= 0 || callbackProperties.rateLimit <= 0) {
            return true
        }
        val now = clock.millis()
        val currentWindowStart = windowStart
        if (currentWindowStart == null || now - currentWindowStart >= windowLength) {
            windowStart = now
            callsInWindow = 0
        }
        if (callsInWindow >= callbackProperties.rateLimit) {
            logger.warn {
                "Rejected Xential callback: more than ${callbackProperties.rateLimit} callbacks within " +
                    "${callbackProperties.rateLimitWindow}."
            }
            return false
        }
        callsInWindow++
        return true
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
