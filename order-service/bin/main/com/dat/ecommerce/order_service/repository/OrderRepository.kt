package com.dat.ecommerce.order_service.repository

import com.dat.ecommerce.order_service.model.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long>