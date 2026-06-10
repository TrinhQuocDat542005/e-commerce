package com.dat.ecommerce.inventory_service.service

import com.dat.ecommerce.inventory_service.dto.InventoryResponse
import com.dat.ecommerce.inventory_service.repository.InventoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryService(private val inventoryRepository: InventoryRepository) {

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
    fun decreaseStock(skuCode: String, quantity: Int) {
        log.info("⚙️ [Inventory Service] Đang tiến hành xử lý trừ kho ngầm cho SKU: $skuCode, Số lượng mua: $quantity")
        
        // 1. Tìm sản phẩm trong DB bằng skuCode thông qua Repository ông vừa viết ở Bước 1
        val inventoryOptional = inventoryRepository.findBySkuCode(skuCode)
        
        if (inventoryOptional.isPresent) {
            val inventory = inventoryOptional.get()
            
            // 2. Kiểm tra xem số lượng hàng trong kho có đủ đáp ứng không
            if (inventory.quantity >= quantity) {
                val oldStock = inventory.quantity
                inventory.quantity = oldStock - quantity // Trừ số lượng thực tế
                
                // 3. Cập nhật xuống Database PostgreSQL
                inventoryRepository.save(inventory)
                log.info("✅ [Inventory Service] Trừ kho THÀNH CÔNG! Sản phẩm [$skuCode]: $oldStock -> ${inventory.quantity}")
            } else {
                log.error("❌ [Inventory Service] Thất bại: Số lượng hàng trong kho không đủ cho SKU: $skuCode (Hiện có: ${inventory.quantity}, Yêu cầu: $quantity)")
                // Sau này sẽ bắn OutOfStockEvent lên Kafka phục vụ Saga Pattern ở đây
            }
        } else {
            log.error("❌ [Inventory Service] Thất bại: Không tìm thấy mã SKU [$skuCode] trong Database!")
        }
    }
}