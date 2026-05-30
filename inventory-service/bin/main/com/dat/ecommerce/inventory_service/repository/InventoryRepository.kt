package com.dat.ecommerce.inventory_service.repository

import com.dat.ecommerce.inventory_service.model.Inventory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface InventoryRepository : JpaRepository<Inventory, Long> {
    fun findBySkuCode(skuCode: String): Optional<Inventory>
    
    fun findBySkuCodeIn(skuCodes: List<String>): List<Inventory>
}