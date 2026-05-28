plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.dat.ecommerce"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

val springCloudVersion = "2023.0.5"

dependencies {
    // Thư viện cốt lõi của API Gateway
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    
    // Đăng ký làm client về tổng đài Eureka để Gateway biết đường tìm các service khác
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}