plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}
kotlin { jvmToolchain(17) }
dependencies {
    api(project(":core:domain"))
    implementation(project(":core:action"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
