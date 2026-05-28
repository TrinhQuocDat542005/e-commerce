package com.dat.ecommerce.inventory_service.model

import jakarta.persistence.*

@Entity
@Table(name = "inventory")
class Inventory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    
    var skuCode: String = " ",
    var quantity: Int = 0
)