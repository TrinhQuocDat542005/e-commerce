package com.dat.ecommerce.auth_service.service

import com.dat.ecommerce.auth_service.dto.AuthResponse
import com.dat.ecommerce.auth_service.dto.LoginRequest
import com.dat.ecommerce.auth_service.dto.RegisterRequest
import com.dat.ecommerce.auth_service.dto.ValidateTokenResponse
import com.dat.ecommerce.auth_service.model.User
import com.dat.ecommerce.auth_service.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    fun register(request: RegisterRequest): String {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username ${request.username} is already taken")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email ${request.email} is already registered")
        }

        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            role = request.role
        )
        userRepository.save(user)
        return "User registered successfully"
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByUsername(request.username)
            .orElseThrow { IllegalArgumentException("Invalid username or password") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Invalid username or password") }

        val token = jwtService.generateToken(user.username, user.role)
        return AuthResponse(
            token = token,
            username = user.username,
            role = user.role
        )
    }

    fun validateToken(token: String): ValidateTokenResponse {
        val isValid = jwtService.validateToken(token)
        return if (isValid) {
            val username = jwtService.extractUsername(token)
            val role = jwtService.extractRole(token)
            ValidateTokenResponse(isValid = true, username = username, role = role)
        } else {
            ValidateTokenResponse(isValid = false, username = null, role = null)
        }
    }
}
