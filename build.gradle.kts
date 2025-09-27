import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    // Root project keeps plugin block to satisfy Gradle but does not apply specific plugins
}

allprojects {
    group = "dev.nefor.activator"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }

    configurations.configureEach {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }
}
