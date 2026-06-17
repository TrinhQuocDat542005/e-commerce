package com.ecommerce.common.event

data class OrderItemEvent(
    val skuCode: String,
    val quantity: Int,
    val price: Double
)
