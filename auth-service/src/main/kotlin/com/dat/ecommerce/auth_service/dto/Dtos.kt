package com.dat.ecommerce.auth_service.dto

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val role: String = "USER"
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val username: String,
    val role: String
)

data class ValidateTokenResponse(
    val isValid: Boolean,
    val username: String?,
    val role: String?
)
