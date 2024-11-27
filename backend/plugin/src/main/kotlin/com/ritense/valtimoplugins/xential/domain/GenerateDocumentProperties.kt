package com.ritense.valtimoplugins.xential.domain

import com.ritense.valtimoplugins.xential.plugin.TemplateDataEntry
import java.util.UUID

data class GenerateDocumentProperties(
    val templateId: UUID,
    val fileFormat: FileFormat,
    val documentId: String,
    val messageName: String,
    val templateData: Array<TemplateDataEntry>
)
