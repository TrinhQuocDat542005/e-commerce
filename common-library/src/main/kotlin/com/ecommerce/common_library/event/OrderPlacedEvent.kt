package com.ecommerce.common.event

data class OrderPlacedEvent(
    val orderNumber: String,
    val skuCode: String,
    val quantity: Int
)