package com.ritense.valtimoplugins.xential.domain

import java.util.UUID

data class XentialDocumentProperties(
    val templateId: UUID,
    val fileFormat: FileFormat,
    val documentId: String,
    val messageName: String,
    val content: MutableMap<String, Any>
)
