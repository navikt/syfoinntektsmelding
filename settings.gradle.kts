rootProject.name = "spinosaurus"

pluginManagement {
    plugins {
        val kotlinVersion: String by settings
        val kotlinterVersion: String by settings
        val versionsVersion: String by settings

        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jmailen.kotlinter") version kotlinterVersion
        id("com.github.ben-manes.versions") version versionsVersion
    }
}
