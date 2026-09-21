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

// Local IDE resolution order: gradle.local.properties -> PINTERM_LOCAL_IDE -> -PlocalIdePath=.
// An explicitly configured path is always honored; the macOS default is only used when it exists.
// Without any usable local IDE (e.g. on CI), the build falls back to a downloadable platform.
val explicitLocalIde: String? = sequenceOf(
    localProperty("localIdePath"),
    System.getenv("PINTERM_LOCAL_IDE"),
    providers.gradleProperty("localIdePath").orNull
).firstOrNull { !it.isNullOrBlank() }

val defaultLocalIde = "/Applications/IntelliJ IDEA.app"
val localIdeHome = file(explicitLocalIde ?: defaultLocalIde)
val useLocalIde = explicitLocalIde != null || localIdeHome.exists()

// Platform version used when downloading an IDE instead of using a local installation.
val platformVersion = providers.gradleProperty("platformVersion").orElse("2026.2.3")

val junitTests = configurations.create("junitTests")

// Resolvable configuration populated by the IntelliJ Platform Gradle Plugin with the platform and
// its bundled plugins/modules. It works for both `local(...)` and downloaded platforms, so tests no
// longer depend on jars extracted from a local IDE install.
val platformTestClasspath = configurations["intellijPlatformTestClasspath"]

dependencies {
    junitTests("junit:junit:4.13.2")

    intellijPlatform {
        if (useLocalIde) {
            local(providers.provider { localIdeHome.absolutePath })
        } else {
            intellijIdea(platformVersion)
        }

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

    publishing {
        // Set PUBLISH_TOKEN in the environment (a CI secret) to run `publishPlugin`.
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    signing {
        // Optional. When these are unset the `signPlugin` task is skipped and the plugin ships unsigned.
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
}

// CHANGELOG.md is the single source of truth for release notes. The section matching the current
// pluginVersion feeds both the Git tag release notes (.github/workflows/release.yml) and the
// plugin's <change-notes> in the built artifact.
fun changelogSection(version: String): String {
    val changelog = rootProject.file("CHANGELOG.md")
    if (!changelog.exists()) {
        return ""
    }
    val lines = changelog.readLines()
    val start = lines.indexOfFirst { it.trimStart().startsWith("## [$version]") }
    if (start < 0) {
        return ""
    }
    return lines.drop(start + 1)
        .takeWhile { !it.trimStart().startsWith("## [") }
        .joinToString("\n")
        .trim()
}

fun markdownToChangeNotesHtml(markdown: String): String {
    val html = StringBuilder()
    var inList = false
    fun closeList() {
        if (inList) {
            html.append("</ul>")
            inList = false
        }
    }

    for (rawLine in markdown.lines()) {
        val line = rawLine.trim()
        when {
            line.isEmpty() -> closeList()
            line.startsWith("#") -> {
                closeList()
                html.append("<p><b>").append(line.trimStart('#').trim()).append("</b></p>")
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                if (!inList) {
                    html.append("<ul>")
                    inList = true
                }
                html.append("<li>").append(line.substring(2).trim()).append("</li>")
            }
            else -> {
                closeList()
                html.append("<p>").append(line).append("</p>")
            }
        }
    }
    closeList()
    return html.toString().replace(Regex("`([^`]+)`"), "<code>$1</code>")
}

val changeNotesHtml = markdownToChangeNotesHtml(changelogSection(project.version.toString()))

tasks {
    patchPluginXml {
        sinceBuild = providers.gradleProperty("sinceBuild")
        if (changeNotesHtml.isNotEmpty()) {
            changeNotes = changeNotesHtml
        }
    }

    compileTestJava {
        classpath = sourceSets["main"].output + junitTests + platformTestClasspath
    }

    test {
        useJUnit()
        include("**/*Test.class")
        exclude("**/*\$*")
        classpath = sourceSets["test"].output + sourceSets["main"].output + junitTests + platformTestClasspath
    }
}
