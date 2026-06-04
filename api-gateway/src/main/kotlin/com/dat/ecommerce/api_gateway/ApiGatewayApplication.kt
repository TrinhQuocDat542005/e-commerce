package com.dat.ecommerce.api_gateway

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.boot.ApplicationRunner

@SpringBootApplication
class ApiGatewayApplication {

    @Bean
    fun testDiscovery(discoveryClient: DiscoveryClient) = ApplicationRunner {

        Thread.sleep(15000)

        println("========== DISCOVERY ==========")

        discoveryClient.services.forEach { service ->
            println("SERVICE: $service")

            discoveryClient.getInstances(service).forEach { instance ->
                println(" -> ${instance.host}:${instance.port}")
            }
        }

        println("================================")
    }
}

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}