plugins {
    id("java-library")
}

java {
    withSourcesJar()
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
