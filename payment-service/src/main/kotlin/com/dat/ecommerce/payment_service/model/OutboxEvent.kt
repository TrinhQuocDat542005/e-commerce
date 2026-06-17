package com.dat.ecommerce.payment_service.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "outbox_events")
data class OutboxEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val aggregateType: String = "",

    @Column(nullable = false)
    val aggregateId: String = "",

    @Column(nullable = false)
    val eventType: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String = "",

    @Column(nullable = false)
    var status: String = "PENDING",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    var processedAt: LocalDateTime? = null
)
