package com.dat.ecommerce.order_service.service

import com.dat.ecommerce.order_service.dto.OrderLineItemsDto
import com.dat.ecommerce.order_service.dto.OrderRequest
import com.dat.ecommerce.order_service.model.Order
import com.dat.ecommerce.order_service.model.OrderLineItems
import com.dat.ecommerce.order_service.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class OrderService(private val orderRepository: OrderRepository) {

    fun placeOrder(orderRequest: OrderRequest) {
        // 1. Ánh xạ danh sách DTO từ request sang danh sách Entity tương ứng
        val orderLineItems = orderRequest.orderLineItemsDtoList
            .map { mapToEntity(it) }

        // 2. Khởi tạo một Đơn hàng mới
        val order = Order(
            orderNumber = UUID.randomUUID().toString(), // Tự sinh chuỗi mã đơn hàng ngẫu nhiên không trùng lặp
            orderLineItemsList = orderLineItems
        )

        // 3. Đổ dữ liệu xuống lưu trong PostgreSQL
        orderRepository.save(order)
    }

    private fun mapToEntity(orderLineItemsDto: OrderLineItemsDto): OrderLineItems {
        return OrderLineItems(
            skuCode = orderLineItemsDto.skuCode,
            price = orderLineItemsDto.price,
            quantity = orderLineItemsDto.quantity
        )
    }
}