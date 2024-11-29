package com.ritense.valtimoplugins.xential.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "xential_tokens")
class XentialToken (

    @Id
    @Column(name = "token", nullable = false, updatable = false)
    val token: UUID,
    @Column(name = "process_id", nullable = false, updatable = false)
    val processId: UUID,
    @Column(name = "message_name", nullable = false, updatable = false)
    val messageName: String,
    @Column(name = "resume_url", nullable = true, updatable = false)
    val resumeUrl: String?
)
