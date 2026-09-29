plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
repositories { mavenCentral() }
dependencies {
    api(project(":core:domain"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
tasks.test { useJUnitPlatform() }
