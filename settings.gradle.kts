// Tên dự án tổng
rootProject.name = "ecommerce-microservices"

// Khai báo các service con (modules)
include("discovery-server")
include("api-gateway")
include("auth-service")
include("product-service")
include("inventory-service")
include("order-service")
include("common-proto")