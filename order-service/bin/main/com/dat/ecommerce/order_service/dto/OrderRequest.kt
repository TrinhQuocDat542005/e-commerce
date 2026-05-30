package com.dat.ecommerce.order_service.dto

data class OrderRequest(
    val orderLineItemsDtoList: List<OrderLineItemsDto> = emptyList()
)