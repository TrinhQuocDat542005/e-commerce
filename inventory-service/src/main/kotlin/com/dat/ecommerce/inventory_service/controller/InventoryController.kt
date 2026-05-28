package com.dat.ecommerce.inventory_service.controller

import com.dat.ecommerce.inventory_service.dto.InventoryResponse
import com.dat.ecommerce.inventory_service.service.InventoryService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/inventory")
class InventoryController(private val inventoryService: InventoryService) {

    // API mới gom đơn nhận parameter dạng mảng từ URL (?skuCode=...&skuCode=...)
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun isInStock(@RequestParam skuCode: List<String>): List<InventoryResponse> {
        return inventoryService.isInStock(skuCode)
    }
}