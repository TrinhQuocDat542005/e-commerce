package com.dat.ecommerce.order_service.model

import jakarta.persistence.*

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val orderNumber: String = "",
    
    // Mối quan hệ 1-Nhiều: Một đơn hàng chứa danh sách nhiều mặt hàng
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id") // Tự sinh khóa ngoại liên kết bảng trong PostgreSQL
    val orderLineItemsList: List<OrderLineItems> = mutableListOf()
)