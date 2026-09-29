plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
dependencies {
    api(project(":core:domain"))
    api(project(":core:ai"))
    api(project(":core:engine"))
    api(project(":core:adapter"))
    implementation(project(":core:state"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
