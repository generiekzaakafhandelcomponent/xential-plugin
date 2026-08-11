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

package com.ritense.valtimoplugins.xential.domain

/**
 * The result of handling an incoming Xential document callback.
 *
 * [INVALID_REQUEST] deliberately covers every way in which a callback can fail to identify a live document
 * creation session - a malformed id, an unknown id, an expired id and an undecodable payload all map onto it,
 * so that the endpoint cannot be used to probe which session ids exist.
 */
enum class DocumentCallbackOutcome {
    /** The document was stored and the BPMN message was correlated. */
    PROCESSED,

    /** The signature was missing or did not match, and verification is being enforced. */
    INVALID_SIGNATURE,

    /** The callback did not resolve to a live document creation session. */
    INVALID_REQUEST,

    /** The endpoint is receiving more callbacks than it is configured to accept. */
    RATE_LIMITED,
}
