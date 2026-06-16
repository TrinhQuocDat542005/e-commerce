package com.dat.ecommerce.auth_service.controller

import com.dat.ecommerce.auth_service.dto.AuthResponse
import com.dat.ecommerce.auth_service.dto.LoginRequest
import com.dat.ecommerce.auth_service.dto.RegisterRequest
import com.dat.ecommerce.auth_service.dto.ValidateTokenResponse
import com.dat.ecommerce.auth_service.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<Map<String, String>> {
        return try {
            val message = authService.register(request)
            ResponseEntity.ok(mapOf("message" to message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to (e.message ?: "Bad request")))
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to (e.message ?: "Invalid credentials")))
        }
    }

    @GetMapping("/validate")
    fun validateToken(@RequestParam("token") token: String): ResponseEntity<ValidateTokenResponse> {
        val response = authService.validateToken(token)
        return if (response.isValid) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
        }
    }
}
