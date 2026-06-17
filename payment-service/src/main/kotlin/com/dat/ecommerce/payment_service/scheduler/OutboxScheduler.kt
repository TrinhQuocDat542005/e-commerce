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
import io.micrometer.tracing.Tracer

@Component
class OutboxScheduler(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer
) {
    private val log = LoggerFactory.getLogger(OutboxScheduler::class.java)

    private fun <T> runInTraceContext(traceHeadersJson: String?, spanName: String, block: () -> T): T {
        val headersMap = if (!traceHeadersJson.isNullOrBlank()) {
            try {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(traceHeadersJson, Map::class.java) as? Map<String, String>
            } catch (e: Exception) {
                log.error("Failed to parse trace headers", e)
                null
            }
        } else null

        if (headersMap != null) {
            val context = tracer.propagation().extractor { carrier: Map<String, String>, key: String ->
                carrier[key]
            }.extract(headersMap)
            val span = tracer.nextSpan(context).name(spanName).start()
            try {
                return tracer.withSpan(span).use {
                    block()
                }
            } finally {
                span.end()
            }
        } else {
            return block()
        }
    }

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
                    runInTraceContext(event.traceHeaders, "relay-payment-response") {
                        kafkaTemplate.send("payment-response-topic", paymentResponseEvent.orderNumber, paymentResponseEvent).get()
                    }
                    
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
