package com.dat.ecommerce.payment_service.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "outbox_events")
data class OutboxEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val aggregateType: String = "",
    val aggregateId: String = "",
    val eventType: String = "",
    val payload: String = "",
    var status: String = "PENDING",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var processedAt: LocalDateTime? = null
)
