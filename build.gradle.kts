import java.text.SimpleDateFormat
import java.util.Date

plugins {
    java
    application
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

application {
    mainClass = "de.aminh.jcmp.Main"
}

val timestamp: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
tasks.jmh {
    humanOutputFile = project.layout.buildDirectory.file("reports/jmh/human_${timestamp}.txt").get().asFile
    resultsFile = project.layout.buildDirectory.file("reports/jmh/results_${timestamp}.csv").get().asFile
    profilers = listOf("perfnorm:events=power/energy-pkg/")
//    profilers = listOf("perfasm")
    resultFormat = "CSV"
}

tasks.test {
    useJUnitPlatform()
}