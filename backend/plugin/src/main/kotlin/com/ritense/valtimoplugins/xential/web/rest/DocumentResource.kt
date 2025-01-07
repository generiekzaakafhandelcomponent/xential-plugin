package com.ritense.valtimoplugins.xential.web.rest

import com.ritense.valtimo.contract.annotation.SkipComponentScan
import com.ritense.valtimo.contract.domain.ValtimoMediaType
import com.ritense.valtimoplugins.xential.domain.DocumentCreatedMessage
import com.ritense.valtimoplugins.xential.service.DocumentGenerationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@SkipComponentScan
@RequestMapping("/api", produces = [ValtimoMediaType.APPLICATION_JSON_UTF8_VALUE])
class DocumentResource(
    val documentGenerationService: DocumentGenerationService
) {

    @PostMapping("/v1/xential/document")
    fun handleSubmission(
        @RequestBody message: DocumentCreatedMessage
    ) {
        documentGenerationService.onDocumentGenerated(message)
    }
}
