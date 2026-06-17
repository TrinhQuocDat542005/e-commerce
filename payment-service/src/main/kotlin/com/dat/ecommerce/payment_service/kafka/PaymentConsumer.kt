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
            val skuCode = event.skuCode
            val quantity = event.quantity
            val price = event.price
            
            if (skuCode != null && quantity != null && price != null) {
                val totalPrice = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity.toLong()))
                // Luôn thanh toán cho tài khoản testuser
                paymentService.deductBalance(event.orderNumber, "testuser", totalPrice, skuCode, quantity)
            } else {
                log.error("❌ [Payment Service] Thiếu thông tin skuCode, quantity hoặc price trong InventoryResponseEvent để tiến hành thanh toán!")
            }
        } else {
            log.info("ℹ️ [Payment Service] Kho hàng báo thất bại cho Đơn hàng ${event.orderNumber}. Không cần xử lý thanh toán.")
        }
    }
}
