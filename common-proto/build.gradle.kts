plugins {
    kotlin("jvm")
    id("com.google.protobuf") version "0.9.4" apply false
}

java {
    toolchain {
        // Dùng phương thức .set() kết hợp với JavaLanguageVersion.of()
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    compilerOptions {
        jvmToolchain(21)
    }
}

repositories {
    mavenCentral()
}