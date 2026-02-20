plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.3"
}

group = "de.aminh"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("org.codehaus.janino:janino:3.1.12")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}