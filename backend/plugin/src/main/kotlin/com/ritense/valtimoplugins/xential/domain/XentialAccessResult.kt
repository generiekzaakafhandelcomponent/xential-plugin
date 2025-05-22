package com.ritense.valtimoplugins.xential.domain

import org.springframework.http.HttpStatusCode

data class XentialAccessResult(
    val statusCode: HttpStatusCode,
    val statusMessage: String
)
