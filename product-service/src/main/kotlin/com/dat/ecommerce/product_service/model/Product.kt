package com.dat.ecommerce.product_service.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    var name: String = "",
    
    var description: String = "",
    
    var price: BigDecimal = BigDecimal.ZERO,
    
    var skuCode: String = ""
)