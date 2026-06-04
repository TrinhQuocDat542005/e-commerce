plugins {
    kotlin("jvm")
    // Nạp plugin quản lý dependency của Spring vào nhưng để apply false ở đây
    id("io.spring.dependency-management") version "1.1.7"
}

// Bật tính năng quản lý version tự động lên
apply(plugin = "io.spring.dependency-management")

group = "com.dat.ecommerce"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencyManagement {
    imports {
        // Ép module này sử dụng đúng bảng tổng sắp version của Spring Boot hệ thống đang chạy
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.10")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Không cần ghi :2.17.1 nữa, Spring Boot Bom tự điền bản tương thích nhất!
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}