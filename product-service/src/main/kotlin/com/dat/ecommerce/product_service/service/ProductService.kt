package com.dat.ecommerce.product_service.service

import com.dat.ecommerce.product_service.model.Product
import com.dat.ecommerce.product_service.repository.ProductRepository
import org.springframework.stereotype.Service

@Service
class ProductService(private val productRepository: ProductRepository) {

    // Hàm lấy toàn bộ danh sách sản phẩm từ database
    fun getAllProducts(): List<Product> {
        return productRepository.findAll()
    }

    // Hàm lưu một sản phẩm mới vào database
    fun createProduct(product: Product): Product {
        return productRepository.save(product)
    }
}