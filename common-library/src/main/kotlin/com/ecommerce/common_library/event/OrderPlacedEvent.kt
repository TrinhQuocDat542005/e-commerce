package com.ecommerce.common.event

data class OrderPlacedEvent(
    val orderNumber: String,
    val items: List<OrderItemEvent>
)