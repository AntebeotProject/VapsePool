import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20" // or kotlin("multiplatform") or any other kotlin plugin
    kotlin("plugin.serialization") version "1.7.20"
    application
}

group = "org.antibiotic.pool"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    jcenter()
}

dependencies {
    implementation(project(mapOf("path" to ":AsyncServer")))
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.litote.kmongo:kmongo:4.8.0")
    implementation("org.litote.kmongo:kmongo-async:4.8.0")
    // implementation("org.litote.kmongo:kmongo-coroutine:4.8.0")
    implementation("org.bitcoinj:bitcoinj-core:0.16.2") // not need for now if sure.
    implementation("org.eclipse.jetty:jetty-server:11.0.8") // org.eclipse.jetty:jetty-http:11.0.8
    implementation("org.eclipse.jetty:jetty-http:11.0.8")
    implementation("jakarta.mail:jakarta.mail-api:2.1.1")
    implementation("dev.turingcomplete:kotlin-onetimepassword:2.4.0")
    implementation("io.github.g0dkar:qrcode-kotlin-jvm:3.3.0")
    //
    implementation ("com.github.pengrad:java-telegram-bot-api:6.0.1")
    implementation ("com.squareup.okhttp3:okhttp:4.5.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.5.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        val kotlinVersion = "1.7.20"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
        classpath("jakarta.mail:jakarta.mail-api:2.1.1")
    }
}
kotlin {

}