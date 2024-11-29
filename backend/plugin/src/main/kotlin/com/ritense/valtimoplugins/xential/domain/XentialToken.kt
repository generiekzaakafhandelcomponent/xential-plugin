package com.ritense.valtimoplugins.xential.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.net.URI
import java.util.UUID

@Entity
@Table(name = "xential_tokens")
class XentialToken (

//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    @Column(name = "token")
//    val token: UUID = UUID.randomUUID(),
//
//    @Column(name = "external_token")
//    val externalToken: String = "",
//
//    @Column(name = "process_id")
//    val processId: UUID = UUID.randomUUID(),
//
//    @Column(name = "message_name")
//    val messageName: String = "",
//
//    @Column(name = "resume_url")
//    val resumeUrl: String = ""

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
