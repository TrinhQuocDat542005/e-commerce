package com.dat.ecommerce.order_service.service

import org.springframework.kafka.core.KafkaTemplate
import com.ecommerce.common.event.OrderPlacedEvent // Đảm bảo import đúng gói này
import com.dat.ecommerce.order_service.dto.OrderRequest
import com.dat.ecommerce.order_service.model.Order
import com.dat.ecommerce.order_service.model.OrderLineItems
import com.dat.ecommerce.order_service.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val kafkaTemplate: KafkaTemplate<String, OrderPlacedEvent> // Chỉ giữ lại KafkaTemplate
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    fun placeOrder(orderRequest: OrderRequest) {
        val order = Order()
        order.orderNumber = UUID.randomUUID().toString()

        val orderLineItems = orderRequest.orderLineItemsDtoList.map { dto ->
            OrderLineItems(
                skuCode = dto.skuCode,
                price = dto.price,
                quantity = dto.quantity
            )
        }
        order.orderLineItemsList = orderLineItems

    // 1. Lưu đơn hàng vào DB trước
        orderRepository.save(order)
        log.info("✅ [Order Service] Đơn hàng ${order.orderNumber} đã lưu DB!")

    // 2. Bóc trực tiếp từ request payload đầu vào để né lỗi Null Entity ngầm
        val firstRequestDto = orderRequest.orderLineItemsDtoList.firstOrNull()

        val orderPlacedEvent = OrderPlacedEvent(
            orderNumber = order.orderNumber,
            skuCode = firstRequestDto?.skuCode ?: "UNKNOWN", 
            quantity = firstRequestDto?.quantity ?: 0
        )

    // Bắn duy nhất 1 lần lên Kafka (Tôi thấy code cũ của ông bị trùng lặp gõ 2 dòng send)
        kafkaTemplate.send("order-placed-topic", orderPlacedEvent)
        log.info("🚀 [Order Service] Đã bắn sự kiện lên Kafka!")
        log.info("📢 Nội dung sự kiện bay đi: OrderPlacedEvent(orderNumber=${orderPlacedEvent.orderNumber}, skuCode=${orderPlacedEvent.skuCode}, quantity=${orderPlacedEvent.quantity})")
    }
}