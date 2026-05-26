package com.dat.ecommerce.product_service.controller

import com.dat.ecommerce.product_service.model.Product
import com.dat.ecommerce.product_service.service.ProductService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/products")
class ProductController(private val productService: ProductService) {

    // API lấy toàn bộ danh sách sản phẩm
    @GetMapping
    fun getAllProducts(): List<Product> {
        return productService.getAllProducts()
    }

    // API tạo một sản phẩm mới
    @PostMapping
    fun createProduct(@RequestBody product: Product): ResponseEntity<Product> {
        val savedProduct = productService.createProduct(product)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct)
    }
}