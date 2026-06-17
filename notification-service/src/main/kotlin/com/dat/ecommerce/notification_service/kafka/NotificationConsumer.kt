package com.dat.ecommerce.notification_service.kafka

import com.ecommerce.common.event.OrderPlacedEvent
import com.ecommerce.common.event.InventoryResponseEvent
import com.ecommerce.common.event.PaymentResponseEvent
import com.ecommerce.common.event.OrderCancelledEvent
import com.dat.ecommerce.notification_service.controller.NotificationController
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import io.micrometer.tracing.Tracer

@Component
class NotificationConsumer(
    private val notificationController: NotificationController,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer
) {
    private val log = LoggerFactory.getLogger(NotificationConsumer::class.java)

    private fun send(message: String, type: String) {
        try {
            val traceId = tracer.currentSpan()?.context()?.traceId()
            val payload = mapOf(
                "message" to message,
                "type" to type,
                "traceId" to traceId
            )
            val json = objectMapper.writeValueAsString(payload)
            notificationController.dispatchNotification(json)
        } catch (e: Exception) {
            log.error("Failed to serialize notification payload to JSON", e)
        }
    }

    @KafkaListener(topics = ["order-placed-topic"], groupId = "notification-group")
    fun consumeOrderPlaced(event: OrderPlacedEvent) {
        log.info("🔔 Consumed OrderPlacedEvent for order: ${event.orderNumber}")
        send("🔔 Đơn hàng #${event.orderNumber.substring(0, 8)}... đã được khởi tạo, đang xử lý giữ kho...", "info")
    }

    @KafkaListener(topics = ["inventory-response-topic"], groupId = "notification-group")
    fun consumeInventoryResponse(event: InventoryResponseEvent) {
        log.info("📦 Consumed InventoryResponseEvent for order: ${event.orderNumber}, success: ${event.isSuccess}")
        if (event.isSuccess) {
            send("📦 Kho hàng đã giữ chỗ sản phẩm thành công cho Đơn hàng #${event.orderNumber.substring(0, 8)}...", "info")
        } else {
            send("❌ Đơn hàng #${event.orderNumber.substring(0, 8)}... bị hủy do sản phẩm trong kho không khả dụng!", "error")
        }
    }

    @KafkaListener(topics = ["payment-response-topic"], groupId = "notification-group")
    fun consumePaymentResponse(event: PaymentResponseEvent) {
        log.info("💳 Consumed PaymentResponseEvent for order: ${event.orderNumber}, success: ${event.isSuccess}")
        if (event.isSuccess) {
            send("💳 Thanh toán thành công! Đơn hàng #${event.orderNumber.substring(0, 8)}... đã được xác nhận.", "success")
        } else {
            send("❌ Thanh toán thất bại cho Đơn hàng #${event.orderNumber.substring(0, 8)}... do số dư ví không đủ!", "error")
            send("🔄 Đang kích hoạt compensating transaction để hoàn trả sản phẩm vào kho...", "info")
        }
    }

    @KafkaListener(topics = ["order-cancelled-topic"], groupId = "notification-group")
    fun consumeOrderCancelled(event: OrderCancelledEvent) {
        log.info("🔄 Consumed OrderCancelledEvent for order: ${event.orderNumber}")
        send("🔄 Đơn hàng #${event.orderNumber.substring(0, 8)}... đã được hủy. Sản phẩm đã hoàn trả kho.", "info")
    }
}
