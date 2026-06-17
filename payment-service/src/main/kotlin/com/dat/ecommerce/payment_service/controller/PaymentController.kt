package com.dat.ecommerce.payment_service.controller

import com.dat.ecommerce.payment_service.service.PaymentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/payment")
class PaymentController(private val paymentService: PaymentService) {

    @GetMapping("/balance")
    fun getBalance(@RequestParam(defaultValue = "testuser") username: String): Map<String, Any> {
        val balance = paymentService.getBalance(username)
        return mapOf(
            "username" to username,
            "balance" to balance
        )
    }
}
