plugins {
    java
    id("org.springframework.boot") version "3.3.10" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.jpa") version "1.9.24" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

allprojects {
    group = "com.dat.ecommerce"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"  // 
    }
}