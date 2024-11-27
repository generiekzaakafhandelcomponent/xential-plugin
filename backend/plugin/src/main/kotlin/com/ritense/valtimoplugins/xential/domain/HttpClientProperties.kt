package com.ritense.valtimoplugins.xential.domain

import java.io.File
import java.net.URI

data class HttpClientProperties(
    val applicationName: String,
    val applicationPassword: String,
    val baseUrl: URI,
    val serverCertificateFilename: File,
    val clientPrivateKeyFilename: File?,
    val clientCertFile: File?
)
