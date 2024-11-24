plugins {
    kotlin("jvm") version "2.0.21"
}

group = "gg.aquatic.packeteventsext"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}
dependencies {
    compileOnly("com.github.retrooper:packetevents-spigot:2.6.0")
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-all:4.1.115.Final")
}

kotlin {
    jvmToolchain(17)
}