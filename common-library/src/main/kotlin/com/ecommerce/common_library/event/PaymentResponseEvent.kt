package com.ecommerce.common.event

data class PaymentResponseEvent(
    val orderNumber: String,
    val isSuccess: Boolean,
    val reason: String? = null,
    val skuCode: String? = null,
    val quantity: Int? = null
)
