package com.dat.ecommerce.order_service.service

import com.ecommerce.common.event.OrderPlacedEvent
import com.ecommerce.common.event.OrderCancelledEvent
import com.ecommerce.common.event.OrderItemEvent
import com.ecommerce.common.proto.ProductPriceRequest
import com.ecommerce.common.proto.ProductServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
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
import io.micrometer.tracing.Tracer

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    private fun getTraceHeadersJson(): String? {
        return try {
            val tracingHeaders = mutableMapOf<String, String>()
            tracer.propagation().inject(tracer.currentSpan()?.context(), tracingHeaders) { carrier, key, value ->
                carrier[key] = value
            }
            if (tracingHeaders.isNotEmpty()) {
                objectMapper.writeValueAsString(tracingHeaders)
            } else null
        } catch (e: Exception) {
            log.error("Failed to inject trace headers", e)
            null
        }
    }

    @GrpcClient("product-service")
    private lateinit var productServiceStub: ProductServiceGrpc.ProductServiceBlockingStub

    fun placeOrder(orderRequest: OrderRequest) {
        val order = Order()
        order.orderNumber = UUID.randomUUID().toString()
        order.status = "PENDING"

        val orderLineItems = orderRequest.orderLineItemsDtoList.map { dto ->
            val grpcRequest = ProductPriceRequest.newBuilder()
                .setSkuCode(dto.skuCode)
                .build()
            val grpcResponse = productServiceStub.getProductPriceBySku(grpcRequest)
            if (!grpcResponse.exists) {
                throw IllegalArgumentException("Product with SKU ${dto.skuCode} does not exist in catalog!")
            }
            val authenticPrice = java.math.BigDecimal.valueOf(grpcResponse.price)
            log.info("🎯 [Order Service] Đã xác thực giá qua gRPC cho ${dto.skuCode}: Giá niêm yết = $authenticPrice (Client gửi = ${dto.price})")
            OrderLineItems(
                skuCode = dto.skuCode,
                price = authenticPrice,
                quantity = dto.quantity
            )
        }
        order.orderLineItemsList = orderLineItems

        // 1. Lưu đơn hàng vào DB trước
        orderRepository.save(order)
        log.info("✅ [Order Service] Đơn hàng ${order.orderNumber} đã lưu DB!")

        // 2. Tạo nội dung sự kiện OrderPlacedEvent
        val orderPlacedEvent = OrderPlacedEvent(
            orderNumber = order.orderNumber,
            items = orderLineItems.map { item ->
                OrderItemEvent(
                    skuCode = item.skuCode,
                    quantity = item.quantity,
                    price = item.price.toDouble()
                )
            }
        )

        // 3. Serialize event sang JSON và lưu vào Outbox table trong cùng transaction
        val payloadJson = objectMapper.writeValueAsString(orderPlacedEvent)
        val outboxEvent = OutboxEvent(
            aggregateType = "ORDER",
            aggregateId = order.orderNumber,
            eventType = "OrderPlaced",
            payload = payloadJson,
            status = "PENDING",
            traceHeaders = getTraceHeadersJson()
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

            // Lưu một OutboxEvent duy nhất chứa toàn bộ sản phẩm cần hoàn trả kho
            val orderCancelledEvent = OrderCancelledEvent(
                orderNumber = order.orderNumber,
                items = order.orderLineItemsList.map { item ->
                    OrderItemEvent(
                        skuCode = item.skuCode,
                        quantity = item.quantity,
                        price = item.price.toDouble()
                    )
                }
            )
            val payloadJson = objectMapper.writeValueAsString(orderCancelledEvent)
            val outboxEvent = OutboxEvent(
                aggregateType = "ORDER",
                aggregateId = order.orderNumber,
                eventType = "OrderCancelled",
                payload = payloadJson,
                status = "PENDING",
                traceHeaders = getTraceHeadersJson()
            )
            outboxRepository.save(outboxEvent)
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