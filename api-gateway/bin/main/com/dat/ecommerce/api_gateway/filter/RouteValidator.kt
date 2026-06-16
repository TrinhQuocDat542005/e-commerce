package com.dat.ecommerce.api_gateway.filter

import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import java.util.function.Predicate

@Component
class RouteValidator {

    companion object {
        val openApiEndpoints = listOf(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/validate",
            "/eureka"
        )
    }

    var isSecured: Predicate<ServerHttpRequest> = Predicate { request ->
        openApiEndpoints.none { uri -> request.uri.path.contains(uri) }
    }
}
