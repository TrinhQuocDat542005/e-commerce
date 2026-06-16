package com.ecommerce.common.event

data class InventoryResponseEvent(
    val orderNumber: String,
    val isSuccess: Boolean,
    val reason: String? = null
)
