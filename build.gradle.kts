plugins {
    kotlin("jvm") version "2.0.20"
}

group = "com.enoch02"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("it.skrape:skrapeit:1.2.2")
    implementation("it.skrape:skrapeit-browser-fetcher:1.1.5")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}