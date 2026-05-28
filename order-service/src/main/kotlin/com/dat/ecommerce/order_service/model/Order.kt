package com.dat.ecommerce.order_service.model

import jakarta.persistence.*

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var orderNumber: String = "",
    
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    var orderLineItemsList: List<OrderLineItems> = mutableListOf()
)