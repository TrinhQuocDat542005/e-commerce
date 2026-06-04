package com.dat.ecommerce.inventory_service.kafka

import com.dat.ecommerce.common_library.event.OrderPlacedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class InventoryConsumer {

    private val log = LoggerFactory.getLogger(InventoryConsumer::class.java)

    @KafkaListener(topics = ["order-placed-topic"], groupId = "inventory-group")
    fun consumeOrderPlacedEvent(event: OrderPlacedEvent) {
        log.info("📩 [Inventory Service] Nhận được tín hiệu đặt hàng từ Kafka!")
        log.info("📦 Chi tiết đơn hàng: Mã Đơn = ${event.orderId}, Số lượng mặt hàng = ${event.items.size}")
        
        // TODO: Gọi xuống InventoryService để thực hiện logic kiểm tra và trừ kho (DB) ở đây
    }
}