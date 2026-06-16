package com.dat.ecommerce.order_service.kafka

import com.ecommerce.common.event.InventoryResponseEvent
import com.dat.ecommerce.order_service.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderConsumer(private val orderRepository: OrderRepository) {

    private val log = LoggerFactory.getLogger(OrderConsumer::class.java)

    @KafkaListener(topics = ["inventory-response-topic"], groupId = "order-group")
    @Transactional
    fun consumeInventoryResponse(event: InventoryResponseEvent) {
        log.info("📩 [Order Service] Nhận được kết quả xử lý kho cho Đơn hàng: ${event.orderNumber}")
        
        val orderOptional = orderRepository.findByOrderNumber(event.orderNumber)
        if (orderOptional.isPresent) {
            val order = orderOptional.get()
            if (event.isSuccess) {
                order.status = "CONFIRMED"
                log.info("✅ [Order Service] Đơn hàng ${event.orderNumber} đã chuyển trạng thái thành CONFIRMED")
            } else {
                order.status = "CANCELLED"
                log.error("❌ [Order Service] Đơn hàng ${event.orderNumber} bị hủy vì lý do: ${event.reason}")
            }
            orderRepository.save(order)
        } else {
            log.error("❌ [Order Service] Không tìm thấy đơn hàng: ${event.orderNumber} để cập nhật trạng thái!")
        }
    }
}
