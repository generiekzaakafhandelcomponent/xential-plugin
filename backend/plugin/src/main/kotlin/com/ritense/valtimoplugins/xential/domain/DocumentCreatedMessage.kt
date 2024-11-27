package com.ritense.valtimoplugins.xential.domain

data class DocumentCreatedMessage(
    val taakapplicatie: String,
    val gebruiker: String,
    val documentCreatieSessieId: String,
    val formaat: FileFormat,
    val documentkenmerk: String,
    val data: String,
)
