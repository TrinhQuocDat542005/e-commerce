package com.dat.ecommerce.payment_service.scheduler

import com.ecommerce.common.event.PaymentResponseEvent
import com.dat.ecommerce.payment_service.model.OutboxEvent
import com.dat.ecommerce.payment_service.repository.OutboxRepository
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
                if (event.eventType == "PaymentResponse") {
                    val paymentResponseEvent = objectMapper.readValue(event.payload, PaymentResponseEvent::class.java)
                    
                    // Publish to Kafka synchronously
                    kafkaTemplate.send("payment-response-topic", paymentResponseEvent.orderNumber, paymentResponseEvent).get()
                    
                    // Mark as processed
                    event.status = "PROCESSED"
                    event.processedAt = LocalDateTime.now()
                    outboxRepository.save(event)
                    
                    log.info("✅ Successfully relayed PaymentResponse Event ID: ${event.id} for Order: ${event.aggregateId}")
                }
            } catch (e: Exception) {
                log.error("❌ Failed to relay Outbox Event ID: ${event.id} to Kafka. Reason: ${e.message}", e)
            }
        }
    }
}
