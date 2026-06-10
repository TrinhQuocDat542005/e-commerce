package com.dat.ecommerce.inventory_service.service

import com.dat.ecommerce.inventory_service.dto.InventoryResponse
import com.dat.ecommerce.inventory_service.repository.InventoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryService(private val inventoryRepository: InventoryRepository) {

    @Transactional(readOnly = true)
    fun isInStock(skuCodes: List<String>): List<InventoryResponse> {
        return inventoryRepository.findBySkuCodeIn(skuCodes).map { inventory ->
            InventoryResponse(
                skuCode = inventory.skuCode,
                isInStock = inventory.quantity > 0
            )
        }
    }
    fun deductStock(skuCode: String, quantity: Int) {
        // 1. Tìm thông tin sản phẩm trong Database theo skuCode
        val inventory = inventoryRepository.findBySkuCode(skuCode)
            ?: throw RuntimeException("Mặt hàng với mã $skuCode không tồn tại trong hệ thống kho!")

        // 2. Kiểm tra xem số lượng trong kho có đủ bán không
        if (inventory.quantity < quantity) {
            throw RuntimeException("Mặt hàng $skuCode đã hết hàng hoặc không đủ số lượng! (Trong kho còn: ${inventory.quantity}, Khách mua: $quantity)")
        }

        // 3. Đủ hàng thì trừ trực tiếp số lượng tồn kho
        inventory.quantity = inventory.quantity - quantity
        
        // 4. Lưu lại cập nhật mới xuống Database PostgreSQL
        inventoryRepository.save(inventory)
    }
}