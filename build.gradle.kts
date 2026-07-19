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

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.enoch02.MainKt"
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    manifest.from(tasks.jar.get().manifest)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(tasks.compileKotlin.get().destinationDirectory)
    archiveClassifier.set("all")
}

tasks.test {
    useJUnitPlatform()
}