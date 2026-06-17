package com.dat.ecommerce.inventory_service.kafka 

import com.ecommerce.common.event.OrderPlacedEvent
import com.ecommerce.common.event.OrderCancelledEvent
import com.ecommerce.common.event.PaymentResponseEvent
import com.dat.ecommerce.inventory_service.service.InventoryService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class InventoryConsumer(private val inventoryService: InventoryService) { // Đổi tên biến cho chuẩn Service

    private val log = LoggerFactory.getLogger(InventoryConsumer::class.java)

    @KafkaListener(topics = ["order-placed-topic"], groupId = "inventory-group")
    fun consumeOrderPlacedEvent(event: OrderPlacedEvent) {
        log.info("📩 [Inventory Service] Nhận được tín hiệu đặt hàng từ Kafka!")
        log.info("📦 Chi tiết đơn hàng: Mã Đơn = ${event.orderNumber}, SKU = ${event.skuCode}, Số lượng = ${event.quantity}, Giá = ${event.price}")
        
        // Gọi service xuống DB trừ kho luôn!
        inventoryService.decreaseStock(event.orderNumber, event.skuCode, event.quantity, event.price)
    }

    @KafkaListener(topics = ["order-cancelled-topic"], groupId = "inventory-group")
    fun consumeOrderCancelledEvent(event: OrderCancelledEvent) {
        log.info("📩 [Inventory Service] Nhận được tín hiệu HỦY ĐƠN HÀNG từ Kafka!")
        log.info("📦 Chi tiết hoàn hàng: Mã Đơn = ${event.orderNumber}, SKU = ${event.skuCode}, Số lượng = ${event.quantity}")
        
        // Gọi service xuống DB cộng trả kho!
        inventoryService.increaseStock(event.orderNumber, event.skuCode, event.quantity)
    }

    @KafkaListener(topics = ["payment-response-topic"], groupId = "inventory-group")
    fun consumePaymentResponseEvent(event: PaymentResponseEvent) {
        log.info("📩 [Inventory Service] Nhận được kết quả thanh toán cho Đơn hàng: ${event.orderNumber}")
        if (!event.isSuccess) {
            log.info("🔄 [Inventory Service] Thanh toán thất bại cho Đơn hàng ${event.orderNumber}. Kích hoạt compensating transaction để hoàn kho!")
            val skuCode = event.skuCode
            val quantity = event.quantity
            if (skuCode != null && quantity != null) {
                inventoryService.increaseStock(event.orderNumber, skuCode, quantity)
            } else {
                log.error("❌ [Inventory Service] Dữ liệu PaymentResponseEvent thiếu thông tin skuCode hoặc quantity để hoàn kho!")
            }
        } else {
            log.info("✅ [Inventory Service] Thanh toán thành công cho Đơn hàng ${event.orderNumber}. Xác nhận kho hoàn tất.")
        }
    }
}