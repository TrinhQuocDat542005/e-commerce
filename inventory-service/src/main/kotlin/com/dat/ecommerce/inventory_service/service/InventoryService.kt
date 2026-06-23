package com.dat.ecommerce.inventory_service.service

import com.dat.ecommerce.inventory_service.dto.InventoryResponse
import com.dat.ecommerce.inventory_service.model.Inventory
import com.dat.ecommerce.inventory_service.model.OutboxEvent
import com.dat.ecommerce.inventory_service.repository.InventoryRepository
import com.dat.ecommerce.inventory_service.repository.OutboxRepository
import com.ecommerce.common.event.InventoryResponseEvent
import com.ecommerce.common.event.OrderItemEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer,
    private val propagator: Propagator
) {

    private val log = LoggerFactory.getLogger(InventoryService::class.java)

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

    // --- Hàm cũ (Giữ nguyên để check stock qua HTTP/Gateway nếu cần) ---
    @Transactional(readOnly = true)
    fun isInStock(skuCodes: List<String>): List<InventoryResponse> {
        return inventoryRepository.findBySkuCodeIn(skuCodes).map { inventory ->
            InventoryResponse(
                skuCode = inventory.skuCode,
                isInStock = inventory.quantity > 0
            )
        }
    }

    // --- BỔ SUNG THÊM: Hàm xử lý trừ kho ngầm nhận lệnh từ Kafka ---
    @Transactional
    fun decreaseStock(orderNumber: String, items: List<OrderItemEvent>) {
        log.info("⚙️ [Inventory Service] Đang tiến hành xử lý trừ kho ngầm cho Đơn hàng: $orderNumber, Số mặt hàng: ${items.size}")
        
        val inventories = mutableListOf<Inventory>()
        var isSuccess = true
        var failureReason: String? = null
        
        // 1. Kiểm tra toàn bộ danh mục sản phẩm trước
        for (item in items) {
            val inventoryOptional = inventoryRepository.findBySkuCode(item.skuCode)
            if (inventoryOptional.isPresent) {
                val inventory = inventoryOptional.get()
                if (inventory.quantity >= item.quantity) {
                    inventories.add(inventory)
                } else {
                    isSuccess = false
                    failureReason = "Out of stock for SKU: ${item.skuCode} (Available: ${inventory.quantity}, Requested: ${item.quantity})"
                    break
                }
            } else {
                isSuccess = false
                failureReason = "SKU not found: ${item.skuCode}"
                break
            }
        }
        
        // 2. Thực hiện trừ kho nếu tất cả đều hợp lệ
        if (isSuccess) {
            for (i in 0 until items.size) {
                val item = items[i]
                val inventory = inventories[i]
                val oldStock = inventory.quantity
                inventory.quantity = oldStock - item.quantity
                inventoryRepository.save(inventory)
                log.info("✅ [Inventory Service] Trừ kho THÀNH CÔNG! Sản phẩm [${item.skuCode}]: $oldStock -> ${inventory.quantity}")
            }
        } else {
            log.error("❌ [Inventory Service] Trừ kho THẤT BẠI cho đơn hàng $orderNumber: $failureReason")
        }
        
        // 3. Ghi OutboxEvent phản hồi vào Outbox table
        val inventoryResponseEvent = InventoryResponseEvent(
            orderNumber = orderNumber,
            isSuccess = isSuccess,
            reason = failureReason,
            items = items
        )
        
        val payloadJson = objectMapper.writeValueAsString(inventoryResponseEvent)
        val outboxEvent = OutboxEvent(
            aggregateType = "ORDER",
            aggregateId = orderNumber,
            eventType = "InventoryResponse",
            payload = payloadJson,
            status = "PENDING",
            traceHeaders = getTraceHeadersJson()
        )
        outboxRepository.save(outboxEvent)
        log.info("💾 [Inventory Service] Đã ghi nhận OutboxEvent phản hồi cho đơn hàng $orderNumber!")
    }

    @Transactional
    fun increaseStock(orderNumber: String, items: List<OrderItemEvent>) {
        log.info("⚙️ [Inventory Service] Đang tiến hành xử lý hoàn kho cho Đơn hàng: $orderNumber, Số mặt hàng: ${items.size}")
        for (item in items) {
            val inventoryOptional = inventoryRepository.findBySkuCode(item.skuCode)
            if (inventoryOptional.isPresent) {
                val inventory = inventoryOptional.get()
                val oldStock = inventory.quantity
                inventory.quantity = oldStock + item.quantity
                inventoryRepository.save(inventory)
                log.info("✅ [Inventory Service] Hoàn kho THÀNH CÔNG! Sản phẩm [${item.skuCode}]: $oldStock -> ${inventory.quantity}")
            } else {
                log.error("❌ [Inventory Service] Thất bại hoàn kho: Không tìm thấy mã SKU [${item.skuCode}] trong Database!")
            }
        }
    }

    @Transactional(readOnly = true)
    fun getAllInventory(): List<Inventory> {
        return inventoryRepository.findAll()
    }
}