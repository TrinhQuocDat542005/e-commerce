package com.dat.ecommerce.product_service.repository

import com.dat.ecommerce.product_service.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long>