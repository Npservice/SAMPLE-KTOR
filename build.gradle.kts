plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    application
}

group = "com.example"
version = "0.0.1"

// jansi (dipakai ktor-server-call-logging untuk warna console) memanggil kode native.
// Sejak JDK 24, tanpa flag ini muncul warning "A restricted method ... has been called".
val jvmFlags = listOf("--enable-native-access=ALL-UNNAMED")

application {
    mainClass.set("com.example.ApplicationKt")
    applicationDefaultJvmArgs = jvmFlags
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.6.0"
val koinVersion = "4.2.2"
val exposedVersion = "1.5.0"

dependencies {
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-rate-limit")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-call-id")
    implementation("io.ktor:ktor-server-request-validation")
    implementation("io.ktor:ktor-server-double-receive")

    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.mysql:mysql-connector-j:9.7.0")
    implementation("com.zaxxer:HikariCP:7.1.0")

    implementation("io.lettuce:lettuce-core:7.7.0.RELEASE")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("ch.qos.logback:logback-classic:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.11.0")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("com.h2database:h2:2.5.250")
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(jvmFlags)
    // File log dari test ditulis ke build/, bukan ke folder logs/ project.
    systemProperty("LOG_DIR", layout.buildDirectory.dir("test-logs").get().asFile.path)
}

// Isi data awal (permission, role, admin): ./gradlew seed
tasks.register<JavaExec>("seed") {
    group = "application"
    description = "Seed permission, role, dan user admin default"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.database.seeds.RunSeedKt")
    jvmArgs(jvmFlags)
}

// Script `bin/seed` di hasil `installDist`, supaya seed bisa dijalankan di Docker:
// docker compose run --rm app bin/seed
val seedStartScripts by tasks.registering(CreateStartScripts::class) {
    applicationName = "seed"
    mainClass.set("com.example.database.seeds.RunSeedKt")
    classpath = files(tasks.jar, configurations.runtimeClasspath)
    outputDir = layout.buildDirectory.dir("seed-scripts").get().asFile
    defaultJvmOpts = jvmFlags
}

distributions {
    main {
        contents {
            into("bin") { from(seedStartScripts) }
        }
    }
}
