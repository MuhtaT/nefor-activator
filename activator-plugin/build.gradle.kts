plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    withSourcesJar()
}

dependencies {
    implementation(project(":activator-api"))
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

val pluginVersion = project.version

tasks.processResources {
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("activator-plugin")
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

