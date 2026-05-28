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
}