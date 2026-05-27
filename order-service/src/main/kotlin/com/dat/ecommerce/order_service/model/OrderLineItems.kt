package com.dat.ecommerce.order_service.model

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "order_line_items")
data class OrderLineItems(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val skuCode: String = "",
    val price: BigDecimal = BigDecimal.ZERO, // Kiểu dữ liệu chuẩn chống sai số tiền tệ
    val quantity: Int = 0
)