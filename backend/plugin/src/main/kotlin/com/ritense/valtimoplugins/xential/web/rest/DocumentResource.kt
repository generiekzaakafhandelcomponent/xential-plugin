package com.ritense.valtimoplugins.xential.web.rest

import com.fasterxml.jackson.databind.JsonNode
import com.ritense.valtimo.contract.annotation.SkipComponentScan
import com.ritense.valtimo.contract.domain.ValtimoMediaType
import com.ritense.valtimoplugins.xential.domain.DocumentCreatedMessage
import com.ritense.valtimoplugins.xential.service.DocumentGenerationService
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@SkipComponentScan
//@SkipComponentScan
@RequestMapping("/api", produces = [ValtimoMediaType.APPLICATION_JSON_UTF8_VALUE])
class DocumentResource(
    val documentGenerationService: DocumentGenerationService
) {

    @PostMapping("/xential/v1/document")
    fun handleSubmission(
        @RequestBody message: DocumentCreatedMessage
    ) {
        documentGenerationService.onDocumentGenerated(message)
    }
}
