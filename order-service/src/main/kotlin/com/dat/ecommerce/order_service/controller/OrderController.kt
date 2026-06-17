package com.dat.ecommerce.order_service.controller

import com.dat.ecommerce.order_service.dto.OrderRequest
import com.dat.ecommerce.order_service.model.Order
import com.dat.ecommerce.order_service.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/order")
class OrderController(private val orderService: OrderService) {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getAllOrders(): List<Order> {
        return orderService.getAllOrders()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) 
    fun placeOrder(@RequestBody orderRequest: OrderRequest): String {
        orderService.placeOrder(orderRequest)
        return "Order Placed Successfully"
    }

    @PutMapping("/cancel/{orderNumber}")
    @ResponseStatus(HttpStatus.OK)
    fun cancelOrder(@PathVariable("orderNumber") orderNumber: String): String {
        orderService.cancelOrder(orderNumber)
        return "Order Cancelled Successfully"
    }
}