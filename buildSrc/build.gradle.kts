plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.1.21"
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
}
