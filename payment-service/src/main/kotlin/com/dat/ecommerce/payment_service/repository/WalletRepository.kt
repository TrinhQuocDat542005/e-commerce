package com.dat.ecommerce.payment_service.repository

import com.dat.ecommerce.payment_service.model.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WalletRepository : JpaRepository<Wallet, Long> {
    fun findByUsername(username: String): Optional<Wallet>
}
