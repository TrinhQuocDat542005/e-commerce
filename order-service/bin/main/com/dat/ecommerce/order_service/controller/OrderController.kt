package com.dat.ecommerce.order_service.controller

import com.dat.ecommerce.order_service.dto.OrderRequest
import com.dat.ecommerce.order_service.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/order")
class OrderController(private val orderService: OrderService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) 
    fun placeOrder(@RequestBody orderRequest: OrderRequest): String {
        orderService.placeOrder(orderRequest)
        return "Order Placed Successfully"
    }
}