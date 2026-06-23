package com.dat.ecommerce.payment_service.service

import com.dat.ecommerce.payment_service.model.Wallet
import com.dat.ecommerce.payment_service.model.OutboxEvent
import com.dat.ecommerce.payment_service.repository.WalletRepository
import com.dat.ecommerce.payment_service.repository.OutboxRepository
import com.ecommerce.common.event.PaymentResponseEvent
import com.ecommerce.common.event.OrderItemEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator

@Service
class PaymentService(
    private val walletRepository: WalletRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer,
    private val propagator: Propagator
) {
    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    private fun getTraceHeadersJson(): String? {
        return try {
            val tracingHeaders = mutableMapOf<String, String>()
            val currentContext = tracer.currentSpan()?.context()
            if (currentContext != null) {
                propagator.inject(currentContext, tracingHeaders) { carrier, key, value ->
                    carrier?.put(key, value)
                }
            }
            if (tracingHeaders.isNotEmpty()) {
                objectMapper.writeValueAsString(tracingHeaders)
            } else null
        } catch (e: Exception) {
            log.error("Failed to inject trace headers", e)
            null
        }
    }

    @Transactional
    fun deductBalance(
        orderNumber: String,
        username: String,
        amount: BigDecimal,
        items: List<OrderItemEvent>
    ) {
        log.info("⚙️ [Payment Service] Đang tiến hành thanh toán cho Đơn hàng: $orderNumber, Tài khoản: $username, Số tiền: $$amount")

        // Tìm ví của username, nếu chưa có thì tự khởi tạo với số dư mặc định $2000.00 để tiện test
        val wallet = walletRepository.findByUsername(username).orElseGet {
            val newWallet = Wallet(username = username, balance = BigDecimal("2000.00"))
            walletRepository.save(newWallet)
            log.info("ℹ️ [Payment Service] Đã tự động tạo ví mới cho $username với số dư ban đầu $2000.00")
            newWallet
        }

        val paymentResponseEvent = if (wallet.balance >= amount) {
            val oldBalance = wallet.balance
            wallet.balance = oldBalance.subtract(amount)
            walletRepository.save(wallet)
            log.info("✅ [Payment Service] Thanh toán THÀNH CÔNG! Tài khoản [$username]: $oldBalance -> ${wallet.balance}")

            PaymentResponseEvent(
                orderNumber = orderNumber,
                isSuccess = true,
                reason = null,
                items = items
            )
        } else {
            log.error("❌ [Payment Service] Thất bại: Số dư tài khoản không đủ cho [$username] (Hiện có: ${wallet.balance}, Yêu cầu: $amount)")

            PaymentResponseEvent(
                orderNumber = orderNumber,
                isSuccess = false,
                reason = "Insufficient balance (Available: ${wallet.balance}, Requested: $amount)",
                items = items
            )
        }

        // Lưu phản hồi vào Outbox table trong cùng transaction
        val payloadJson = objectMapper.writeValueAsString(paymentResponseEvent)
        val outboxEvent = OutboxEvent(
            aggregateType = "ORDER",
            aggregateId = orderNumber,
            eventType = "PaymentResponse",
            payload = payloadJson,
            status = "PENDING",
            traceHeaders = getTraceHeadersJson()
        )
        outboxRepository.save(outboxEvent)
        log.info("💾 [Payment Service] Đã ghi nhận OutboxEvent thanh toán cho đơn hàng $orderNumber!")
    }

    @Transactional(readOnly = true)
    fun getBalance(username: String): BigDecimal {
        return walletRepository.findByUsername(username)
            .map { it.balance }
            .orElse(BigDecimal("2000.00"))
    }
}
