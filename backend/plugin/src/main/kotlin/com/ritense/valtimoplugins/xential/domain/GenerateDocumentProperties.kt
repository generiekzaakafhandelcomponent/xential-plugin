package com.ritense.valtimoplugins.xential.domain

import java.util.UUID

data class GenerateDocumentProperties(
    val templateId: UUID,
    val fileFormat: FileFormat,
    val documentId: String,
    val messageName: String
)
