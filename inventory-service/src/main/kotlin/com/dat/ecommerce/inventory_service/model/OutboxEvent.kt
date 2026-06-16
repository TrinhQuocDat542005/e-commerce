package com.dat.ecommerce.inventory_service.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "outbox_events")
class OutboxEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var aggregateType: String = "",

    @Column(nullable = false)
    var aggregateId: String = "",

    @Column(nullable = false)
    var eventType: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var payload: String = "",

    @Column(nullable = false)
    var status: String = "PENDING",

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    var processedAt: LocalDateTime? = null
)
