package com.ecommerce.common.event

data class OrderCancelledEvent(
    val orderNumber: String,
    val items: List<OrderItemEvent>
)
