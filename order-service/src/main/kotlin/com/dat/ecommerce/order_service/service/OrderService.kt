package com.dat.ecommerce.order_service.service

import com.ecommerce.common.event.OrderPlacedEvent
import com.ecommerce.common.event.OrderCancelledEvent
import com.dat.ecommerce.order_service.dto.OrderRequest
import com.dat.ecommerce.order_service.model.Order
import com.dat.ecommerce.order_service.model.OrderLineItems
import com.dat.ecommerce.order_service.model.OutboxEvent
import com.dat.ecommerce.order_service.repository.OrderRepository
import com.dat.ecommerce.order_service.repository.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    fun placeOrder(orderRequest: OrderRequest) {
        val order = Order()
        order.orderNumber = UUID.randomUUID().toString()
        order.status = "PENDING"

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

        // 2. Tạo nội dung sự kiện OrderPlacedEvent
        val firstRequestDto = orderRequest.orderLineItemsDtoList.firstOrNull()

        val orderPlacedEvent = OrderPlacedEvent(
            orderNumber = order.orderNumber,
            skuCode = firstRequestDto?.skuCode ?: "UNKNOWN", 
            quantity = firstRequestDto?.quantity ?: 0
        )

        // 3. Serialize event sang JSON và lưu vào Outbox table trong cùng transaction
        val payloadJson = objectMapper.writeValueAsString(orderPlacedEvent)
        val outboxEvent = OutboxEvent(
            aggregateType = "ORDER",
            aggregateId = order.orderNumber,
            eventType = "OrderPlaced",
            payload = payloadJson,
            status = "PENDING"
        )
        outboxRepository.save(outboxEvent)
        log.info("💾 [Order Service] Đã ghi nhận OutboxEvent cho đơn hàng ${order.orderNumber}!")
    }

    fun cancelOrder(orderNumber: String) {
        val orderOptional = orderRepository.findByOrderNumber(orderNumber)
        if (orderOptional.isPresent) {
            val order = orderOptional.get()
            if (order.status != "CONFIRMED") {
                throw IllegalArgumentException("Cannot cancel order with status: ${order.status}. Only CONFIRMED orders can be cancelled.")
            }
            order.status = "CANCELLED"
            orderRepository.save(order)
            log.info("✅ [Order Service] Đơn hàng ${order.orderNumber} đã cập nhật trạng thái CANCELLED!")

            // Lưu OutboxEvent cho từng item trong đơn hàng để hoàn trả kho
            for (item in order.orderLineItemsList) {
                val orderCancelledEvent = OrderCancelledEvent(
                    orderNumber = order.orderNumber,
                    skuCode = item.skuCode,
                    quantity = item.quantity
                )
                val payloadJson = objectMapper.writeValueAsString(orderCancelledEvent)
                val outboxEvent = OutboxEvent(
                    aggregateType = "ORDER",
                    aggregateId = order.orderNumber,
                    eventType = "OrderCancelled",
                    payload = payloadJson,
                    status = "PENDING"
                )
                outboxRepository.save(outboxEvent)
            }
            log.info("💾 [Order Service] Đã ghi nhận OutboxEvent hủy hàng cho đơn hàng ${order.orderNumber}!")
        } else {
            throw IllegalArgumentException("Order not found with order number: $orderNumber")
        }
    }

    @Transactional(readOnly = true)
    fun getAllOrders(): List<Order> {
        return orderRepository.findAll().sortedByDescending { it.id }
    }
}