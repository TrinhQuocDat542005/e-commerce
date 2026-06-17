package com.dat.ecommerce.inventory_service.service

import com.dat.ecommerce.inventory_service.dto.InventoryResponse
import com.dat.ecommerce.inventory_service.model.Inventory
import com.dat.ecommerce.inventory_service.model.OutboxEvent
import com.dat.ecommerce.inventory_service.repository.InventoryRepository
import com.dat.ecommerce.inventory_service.repository.OutboxRepository
import com.ecommerce.common.event.InventoryResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(InventoryService::class.java)

    // --- Hàm cũ (Giữ nguyên để check stock qua HTTP/Gateway nếu cần) ---
    @Transactional(readOnly = true)
    fun isInStock(skuCodes: List<String>): List<InventoryResponse> {
        return inventoryRepository.findBySkuCodeIn(skuCodes).map { inventory ->
            InventoryResponse(
                skuCode = inventory.skuCode,
                isInStock = inventory.quantity > 0
            )
        }
    }

    // --- BỔ SUNG THÊM: Hàm xử lý trừ kho ngầm nhận lệnh từ Kafka ---
    @Transactional
    fun decreaseStock(orderNumber: String, skuCode: String, quantity: Int) {
        log.info("⚙️ [Inventory Service] Đang tiến hành xử lý trừ kho ngầm cho SKU: $skuCode, Số lượng mua: $quantity, Đơn hàng: $orderNumber")
        
        // 1. Tìm sản phẩm trong DB bằng skuCode
        val inventoryOptional = inventoryRepository.findBySkuCode(skuCode)
        
        val inventoryResponseEvent = if (inventoryOptional.isPresent) {
            val inventory = inventoryOptional.get()
            
            // 2. Kiểm tra xem số lượng hàng trong kho có đủ đáp ứng không
            if (inventory.quantity >= quantity) {
                val oldStock = inventory.quantity
                inventory.quantity = oldStock - quantity // Trừ số lượng thực tế
                
                // 3. Cập nhật xuống Database PostgreSQL
                inventoryRepository.save(inventory)
                log.info("✅ [Inventory Service] Trừ kho THÀNH CÔNG! Sản phẩm [$skuCode]: $oldStock -> ${inventory.quantity}")
                
                InventoryResponseEvent(orderNumber, true)
            } else {
                log.error("❌ [Inventory Service] Thất bại: Số lượng hàng trong kho không đủ cho SKU: $skuCode (Hiện có: ${inventory.quantity}, Yêu cầu: $quantity)")
                
                InventoryResponseEvent(orderNumber, false, "Out of stock (Available: ${inventory.quantity}, Requested: $quantity)")
            }
        } else {
            log.error("❌ [Inventory Service] Thất bại: Không tìm thấy mã SKU [$skuCode] trong Database!")
            
            InventoryResponseEvent(orderNumber, false, "SKU not found")
        }

        // 4. Lưu phản hồi vào Outbox table trong cùng transaction
        val payloadJson = objectMapper.writeValueAsString(inventoryResponseEvent)
        val outboxEvent = OutboxEvent(
            aggregateType = "ORDER",
            aggregateId = orderNumber,
            eventType = "InventoryResponse",
            payload = payloadJson,
            status = "PENDING"
        )
        outboxRepository.save(outboxEvent)
        log.info("💾 [Inventory Service] Đã ghi nhận OutboxEvent phản hồi cho đơn hàng $orderNumber!")
    }

    @Transactional
    fun increaseStock(orderNumber: String, skuCode: String, quantity: Int) {
        log.info("⚙️ [Inventory Service] Đang tiến hành xử lý hoàn kho cho SKU: $skuCode, Số lượng: $quantity, Đơn hàng: $orderNumber")
        val inventoryOptional = inventoryRepository.findBySkuCode(skuCode)
        if (inventoryOptional.isPresent) {
            val inventory = inventoryOptional.get()
            val oldStock = inventory.quantity
            inventory.quantity = oldStock + quantity
            inventoryRepository.save(inventory)
            log.info("✅ [Inventory Service] Hoàn kho THÀNH CÔNG! Sản phẩm [$skuCode]: $oldStock -> ${inventory.quantity}")
        } else {
            log.error("❌ [Inventory Service] Thất bại hoàn kho: Không tìm thấy mã SKU [$skuCode] trong Database!")
        }
    }

    @Transactional(readOnly = true)
    fun getAllInventory(): List<Inventory> {
        return inventoryRepository.findAll()
    }
}