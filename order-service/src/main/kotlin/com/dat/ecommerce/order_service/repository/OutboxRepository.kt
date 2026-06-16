package com.dat.ecommerce.order_service.repository

import com.dat.ecommerce.order_service.model.OutboxEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OutboxRepository : JpaRepository<OutboxEvent, Long> {
    fun findByStatus(status: String): List<OutboxEvent>
}
