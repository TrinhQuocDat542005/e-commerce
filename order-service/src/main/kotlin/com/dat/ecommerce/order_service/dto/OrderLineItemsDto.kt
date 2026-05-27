package com.dat.ecommerce.order_service.dto

import java.math.BigDecimal

data class OrderLineItemsDto(
    val id: Long? = null,
    val skuCode: String = "",
    val price: BigDecimal = BigDecimal.ZERO,
    val quantity: Int = 0
)