package com.dat.ecommerce.api_gateway.filter

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.security.Key

@Component
class AuthenticationFilter(
    private val validator: RouteValidator,
    @Value("\${jwt.secret}")
    private val secret: String
) : AbstractGatewayFilterFactory<AuthenticationFilter.Config>(Config::class.java) {

    class Config

    private fun getSigningKey(): Key {
        val keyBytes = secret.toByteArray(Charsets.UTF_8)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            if (validator.isSecured.test(request)) {
                if (!request.headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization header")
                }

                val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization header format")
                }

                val token = authHeader.substring(7)
                try {
                    val claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .body

                    val username = claims.subject
                    val role = claims["role"] as? String ?: "USER"

                    val modifiedRequest = exchange.request.mutate()
                        .header("X-User-Name", username)
                        .header("X-User-Role", role)
                        .build()

                    chain.filter(exchange.mutate().request(modifiedRequest).build())
                } catch (e: Exception) {
                    throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access: ${e.message}")
                }
            } else {
                chain.filter(exchange)
            }
        }
    }
}
