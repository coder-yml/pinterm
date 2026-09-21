plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

fun localProperty(name: String): String? {
    val localPropertiesFile = rootProject.file("gradle.local.properties")
    if (!localPropertiesFile.exists()) {
        return null
    }
    return localPropertiesFile.readLines()
        .map { it.trim() }
        .firstOrNull { it.startsWith("$name=") && !it.startsWith("#") }
        ?.substringAfter("=")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

val configuredLocalIde = sequenceOf(
    localProperty("localIdePath"),
    System.getenv("PINTERM_LOCAL_IDE")
).firstOrNull { !it.isNullOrBlank() }

val localIdePath = if (!configuredLocalIde.isNullOrBlank()) {
    providers.provider { configuredLocalIde }
} else {
    providers.gradleProperty("localIdePath").orElse("/Applications/IntelliJ IDEA.app")
}

val junitTests = configurations.create("junitTests")
val localIdeHome = file(localIdePath.get())
val localIdeJars = files(
    fileTree(localIdeHome.resolve("Contents/lib")) { include("*.jar") },
    fileTree(localIdeHome.resolve("Contents/plugins/terminal/lib")) { include("**/*.jar") }
)

dependencies {
    junitTests("junit:junit:4.13.2")

    intellijPlatform {
        local(localIdePath)

        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins").map { it.split(',') }
        )

        bundledModules(
            providers.gradleProperty("platformBundledModules").map { it.split(',') }
        )
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("sinceBuild")
        }
    }
}

tasks {
    patchPluginXml {
        sinceBuild = providers.gradleProperty("sinceBuild")
    }

    compileTestJava {
        classpath = sourceSets["main"].output + junitTests + localIdeJars
    }

    test {
        useJUnit()
        include("**/*Test.class")
        exclude("**/*\$*")
        classpath = sourceSets["test"].output + sourceSets["main"].output + junitTests + localIdeJars
    }
}
