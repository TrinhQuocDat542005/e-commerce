plugins {
    java
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.23" apply false
    kotlin("plugin.spring") version "1.9.23" apply false
}

allprojects {
    group = "com.dat.ecommerce"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}