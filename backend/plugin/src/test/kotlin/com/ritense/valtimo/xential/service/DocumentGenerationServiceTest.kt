package com.ritense.valtimo.xential.service

import com.ritense.valtimoplugins.xential.domain.GenerateDocumentProperties
import com.ritense.valtimoplugins.xential.domain.HttpClientProperties
import com.ritense.valtimoplugins.xential.domain.XentialToken
import com.ritense.valtimoplugins.xential.repository.XentialTokenRepository
import com.ritense.valtimoplugins.xential.service.DocumentGenerationService
import com.rotterdam.xential.api.DefaultApi
import com.rotterdam.xential.model.DocumentCreatieResultaat
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI
import java.util.UUID

class DocumentGenerationServiceTest {

    @Mock
    lateinit var execution: DelegateExecution

    @Mock
    lateinit var defaultApi: DefaultApi

    @Mock
    lateinit var xentialTokenRepository: XentialTokenRepository

    @InjectMocks
    lateinit var documentGenerationService: DocumentGenerationService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun shouldGenerateDocument() {
        val executionId = UUID.randomUUID()

        val generateDocumentProperties = GenerateDocumentProperties(
            templateId = UUID.randomUUID(),
            fileFormat = com.ritense.valtimoplugins.xential.domain.FileFormat.PDF,
            documentId = "mijn-kenmerk",
            messageName = "messageName",
            templateData = emptyArray()
        )


        val creatieResultaat = DocumentCreatieResultaat(
            documentCreatieSessieId = UUID.randomUUID().toString(),
            status = DocumentCreatieResultaat.Status.VOLTOOID,
            resumeUrl = null
        )
        whenever(defaultApi.creeerDocument(any(), any(), any())).thenReturn(creatieResultaat)
        val file = kotlin.io.path.createTempFile().toFile()
        val httpClientProperties = HttpClientProperties(
            "applicationName",
            "applicationPassword",
            URI("baseUrl"),
            file,
            null,
            null
        )

//        generateDocumentProperties: GenerateDocumentProperties,

        documentGenerationService.generateDocument(
            httpClientProperties,
            UUID.randomUUID(),
            generateDocumentProperties,
            execution,
        )
/*
    val token: UUID = UUID.randomUUID(),
    val externalToken: String = "",
    val processId: UUID = UUID.randomUUID(),
    val messageName: String = "",
    val resumeUrl: String = ""
 */
        verify(xentialTokenRepository).save(
            XentialToken(
                token = UUID.randomUUID(),
                externalToken = creatieResultaat.documentCreatieSessieId,
                processId = executionId,
                messageName = "messageName",
                resumeUrl = null
            )
        )
    }

}
