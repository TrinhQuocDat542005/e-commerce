package com.dat.ecommerce.inventory_service.util

import com.dat.ecommerce.inventory_service.model.Inventory
import com.dat.ecommerce.inventory_service.repository.InventoryRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataLoader(private val inventoryRepository: InventoryRepository) : CommandLineRunner {
    override fun run(vararg args: String?) {
        if (inventoryRepository.count() == 0L) {
            val stock1 = Inventory(skuCode = "iphone_15", quantity = 100)   
            val stock2 = Inventory(skuCode = "iphone_15_pro", quantity = 0)  

            inventoryRepository.saveAll(listOf(stock1, stock2))
            println(">> Inventory Data Seeded Successfully!")
        }
    }
}