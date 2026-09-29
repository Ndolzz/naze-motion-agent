plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
dependencies {
    api(project(":core:action"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation(project(":core:engine"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(kotlin("test"))
}
