package com.dat.ecommerce.inventory_service.service

import com.dat.ecommerce.inventory_service.repository.InventoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryService(private val inventoryRepository: InventoryRepository) {

    @Transactional(readOnly = true)
    fun isInStock(skuCode: String): Boolean {
        return inventoryRepository.findBySkuCode(skuCode)
            .map { inventory -> inventory.quantity > 0 }
            .orElse(false)
    }
}