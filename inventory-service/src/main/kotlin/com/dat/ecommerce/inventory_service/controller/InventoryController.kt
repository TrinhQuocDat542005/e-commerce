package com.dat.ecommerce.inventory_service.controller

import com.dat.ecommerce.inventory_service.service.InventoryService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/inventory")
class InventoryController(private val inventoryService: InventoryService) {

    @GetMapping("/{sku-code}")
    @ResponseStatus(HttpStatus.OK)
    fun isInStock(@PathVariable("sku-code") skuCode: String): Boolean {
        return inventoryService.isInStock(skuCode)
    }
}