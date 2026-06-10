package com.dat.ecommerce.inventory_service.kafka

import com.dat.ecommerce.common_library.event.OrderPlacedEvent
import com.dat.ecommerce.inventory_service.service.InventoryService // Nhớ import đúng package InventoryService của ông
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class InventoryConsumer(
    // 💡 Inject thằng Service quản lý logic kho vào đây
    private val inventoryService: InventoryService 
) {

    private val log = LoggerFactory.getLogger(InventoryConsumer::class.java)

    @KafkaListener(topics = ["order-placed-topic"], groupId = "inventory-group")
    @Transactional // Đảm bảo toàn bộ quá trình trừ kho của đơn này nằm trong 1 Transaction
    fun consumeOrderPlacedEvent(event: OrderPlacedEvent) {
        log.info("=====> 📩 [Inventory Service] Nhận được tín hiệu chốt đơn từ Kafka! <=====")
        log.info("📦 Đang xử lý kiểm tra và trừ kho cho Đơn hàng ID: ${event.orderId}")
        
        try {
            // 1. Duyệt qua từng sản phẩm khách mua trong Event bản tin gửi sang
            for (item in event.items) {
                log.info("🔍 Đang kiểm tra mã: ${item.skuCode} | Số lượng mua: ${item.quantity}")
                
                // 2. Gọi Service xử lý trừ kho thực tế dưới Database
                inventoryService.deductStock(item.skuCode, item.quantity)
            }
            log.info("✅ [Thành công] Toàn bộ mặt hàng của đơn ${event.orderId} đã được trừ tồn kho thành công!")
            
        } catch (e: Exception) {
            // 🚨 Nếu có bất kỳ món nào bị thiếu hàng, Service sẽ ném ra Exception
            log.error("❌ [Thất bại] Không thể xử lý đơn hàng ${event.orderId} do lỗi: ${e.message}")
            // (Chỗ này sau này sẽ là nơi bắn sự kiện thất bại ngược lại cho Order Service rollback đơn hàng)
        }
    }
}