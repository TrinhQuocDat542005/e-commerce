package com.dat.ecommerce.order_service.service

import org.springframework.kafka.core.KafkaTemplate
import com.ecommerce.common.event.OrderPlacedEvent
import com.dat.ecommerce.order_service.dto.InventoryResponse
import com.dat.ecommerce.order_service.dto.OrderRequest
import com.dat.ecommerce.order_service.model.Order
import com.dat.ecommerce.order_service.model.OrderLineItems
import com.dat.ecommerce.order_service.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val webClient: WebClient,
    private val KafkaTemplate: KafkaTemplate<String, OrderPlacedEvent> // Inject WebClient thần thánh vào đây
) {

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

        // 1. Gom tất cả mã SKU từ đơn hàng của khách thành một danh sách List<String>
        val skuCodes = order.orderLineItemsList.map { it.skuCode }

        // 2. Dùng WebClient bắn HTTP GET Request sang Inventory Service (Cổng 8083)
        val inventoryResponseArray = webClient.get()
            .uri("http://inventory-service:8083/api/inventory") { uriBuilder ->
                uriBuilder.queryParam("skuCode", skuCodes).build()
            }
            
            .retrieve()
            .bodyToMono(Array<InventoryResponse>::class.java) // Hứng mớ JSON trả về ép thành mảng Object
            .block() // Ép WebClient chạy đồng bộ (Synchronous) để đợi kết quả trả về rồi mới đi tiếp

        // 3. Kiểm tra xem TẤT CẢ các món hàng khách mua có đều còn hàng (isInStock == true) không
        val allProductsInStock = inventoryResponseArray != null && 
                inventoryResponseArray.isNotEmpty() &&
                inventoryResponseArray.all { it.isInStock }

        // 4. Nếu đủ hàng thì chốt đơn lưu DB, nếu có thằng hết hàng thì quăng lỗi văng ra ngoài ngay!
        if (allProductsInStock) {
            orderRepository.save(order)
            println(">> Order Placed Successfully!")
        } else {
            throw IllegalArgumentException("Product is not in stock, please try again later")
        }
    }
    
}
