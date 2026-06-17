package com.dat.ecommerce.payment_service.kafka

import com.ecommerce.common.event.InventoryResponseEvent
import com.dat.ecommerce.payment_service.service.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class PaymentConsumer(private val paymentService: PaymentService) {

    private val log = LoggerFactory.getLogger(PaymentConsumer::class.java)

    @KafkaListener(topics = ["inventory-response-topic"], groupId = "payment-group")
    fun consumeInventoryResponse(event: InventoryResponseEvent) {
        log.info("📩 [Payment Service] Nhận được kết quả xử lý kho cho Đơn hàng: ${event.orderNumber}")
        
        if (event.isSuccess) {
            val items = event.items
            if (items.isNotEmpty()) {
                val totalPrice = items.fold(BigDecimal.ZERO) { acc, item ->
                    acc.add(BigDecimal.valueOf(item.price).multiply(BigDecimal.valueOf(item.quantity.toLong())))
                }
                // Luôn thanh toán cho tài khoản testuser
                paymentService.deductBalance(event.orderNumber, "testuser", totalPrice, items)
            } else {
                log.error("❌ [Payment Service] Danh sách sản phẩm trong InventoryResponseEvent bị trống!")
            }
        } else {
            log.info("ℹ️ [Payment Service] Kho hàng báo thất bại cho Đơn hàng ${event.orderNumber}. Không cần xử lý thanh toán.")
        }
    }
}
