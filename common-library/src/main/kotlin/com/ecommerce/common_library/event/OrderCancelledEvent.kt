package com.ecommerce.common.event

data class OrderCancelledEvent(
    val orderNumber: String,
    val skuCode: String,
    val quantity: Int
)
