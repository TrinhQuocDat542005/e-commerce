package com.dat.ecommerce.product_service.service

import com.dat.ecommerce.product_service.repository.ProductRepository
import com.ecommerce.common.proto.ProductPriceRequest
import com.ecommerce.common.proto.ProductPriceResponse
import com.ecommerce.common.proto.ProductServiceGrpc
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.slf4j.LoggerFactory

@GrpcService
class ProductGrpcService(
    private val productRepository: ProductRepository
) : ProductServiceGrpc.ProductServiceImplBase() {

    private val log = LoggerFactory.getLogger(ProductGrpcService::class.java)

    override fun getProductPriceBySku(
        request: ProductPriceRequest,
        responseObserver: StreamObserver<ProductPriceResponse>
    ) {
        val skuCode = request.skuCode
        log.info("📩 [gRPC Product Service] Nhận yêu cầu truy vấn giá cho SKU: $skuCode")

        val productOptional = productRepository.findFirstBySkuCodeOrderByIdDesc(skuCode)
        val response = if (productOptional.isPresent) {
            val product = productOptional.get()
            log.info("✅ [gRPC Product Service] Đã tìm thấy sản phẩm: ${product.name}, Giá: ${product.price}")
            ProductPriceResponse.newBuilder()
                .setSkuCode(skuCode)
                .setPrice(product.price.toDouble())
                .setExists(true)
                .build()
        } else {
            log.warn("⚠️ [gRPC Product Service] Không tìm thấy sản phẩm cho SKU: $skuCode")
            ProductPriceResponse.newBuilder()
                .setSkuCode(skuCode)
                .setPrice(0.0)
                .setExists(false)
                .build()
        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
