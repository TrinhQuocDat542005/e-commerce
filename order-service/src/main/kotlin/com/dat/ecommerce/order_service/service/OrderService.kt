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

        // 1. Lưu đơn hàng vào DB trước (không cần hỏi kho nữa)
        orderRepository.save(order)
        log.info("✅ [Order Service] Đơn hàng ${order.orderNumber} đã lưu DB!")

        // 2. Bắn sự kiện ra Kafka cho Inventory tự xử lý
        val firstItem = order.orderLineItemsList.firstOrNull()

        val orderPlacedEvent = OrderPlacedEvent(
            orderNumber = order.orderNumber,
            skuCode = firstItem?.skuCode ?: "UNKNOWN", // Nếu trống thì để mặc định
            quantity = firstItem?.quantity ?: 0
        )

        kafkaTemplate.send("order-placed-topic", orderPlacedEvent)
        log.info("🚀 [Order Service] Đã bắn sự kiện lên Kafka!")
        log.info("📢 Đang chuẩn bị bắn tin vào topic: order-placed-topic với nội dung: {}", orderPlacedEvent)
kafkaTemplate.send("order-placed-topic", orderPlacedEvent)
    }
}