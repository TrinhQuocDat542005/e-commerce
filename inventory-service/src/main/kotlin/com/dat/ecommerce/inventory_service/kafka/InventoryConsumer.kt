package com.dat.ecommerce.inventory_service.kafka 

import com.ecommerce.common.event.OrderPlacedEvent
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
        log.info("📦 Chi tiết đơn hàng: Mã Đơn = ${event.orderNumber}, SKU = ${event.skuCode}, Số lượng = ${event.quantity}")
        
        // Gọi service xuống DB trừ kho luôn!
        inventoryService.decreaseStock(event.skuCode, event.quantity)
    }
}