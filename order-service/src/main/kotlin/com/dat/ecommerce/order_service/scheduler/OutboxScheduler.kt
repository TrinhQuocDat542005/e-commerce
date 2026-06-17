package com.dat.ecommerce.order_service.scheduler

import com.ecommerce.common.event.OrderPlacedEvent
import com.ecommerce.common.event.OrderCancelledEvent
import com.dat.ecommerce.order_service.model.OutboxEvent
import com.dat.ecommerce.order_service.repository.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class OutboxScheduler(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(OutboxScheduler::class.java)

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun processOutboxEvents() {
        val pendingEvents = outboxRepository.findByStatus("PENDING")
        if (pendingEvents.isEmpty()) return

        log.info("Found ${pendingEvents.size} pending outbox events. Relaying to Kafka...")

        for (event in pendingEvents) {
            try {
                if (event.eventType == "OrderPlaced") {
                    val orderPlacedEvent = objectMapper.readValue(event.payload, OrderPlacedEvent::class.java)
                    
                    // Publish to Kafka synchronously
                    kafkaTemplate.send("order-placed-topic", orderPlacedEvent.orderNumber, orderPlacedEvent).get()
                    
                    // Mark as processed
                    event.status = "PROCESSED"
                    event.processedAt = LocalDateTime.now()
                    outboxRepository.save(event)
                    
                    log.info("✅ Successfully relayed Outbox Event ID: ${event.id} for Order: ${event.aggregateId}")
                } else if (event.eventType == "OrderCancelled") {
                    val orderCancelledEvent = objectMapper.readValue(event.payload, OrderCancelledEvent::class.java)
                    
                    // Publish to Kafka synchronously
                    kafkaTemplate.send("order-cancelled-topic", orderCancelledEvent.orderNumber, orderCancelledEvent).get()
                    
                    // Mark as processed
                    event.status = "PROCESSED"
                    event.processedAt = LocalDateTime.now()
                    outboxRepository.save(event)
                    
                    log.info("✅ Successfully relayed OrderCancelled Event ID: ${event.id} for Order: ${event.aggregateId}")
                }
            } catch (e: Exception) {
                log.error("❌ Failed to relay Outbox Event ID: ${event.id} to Kafka. Reason: ${e.message}", e)
            }
        }
    }
}
