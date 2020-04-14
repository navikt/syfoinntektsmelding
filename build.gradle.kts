import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val githubUser: String by project
val githubPassword: String by project

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
    "maven-publish"
    java
}



allprojects {
    group = "no.nav.syfo"
    version = properties["version"] ?: "local-build"

    repositories {
        mavenCentral()
        maven("https://kotlin.bintray.com/ktor")
        maven("https://packages.confluent.io/maven/")
        maven {
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
            setUrl("https://maven.pkg.github.com/navikt/inntektsmelding-kontrakt")
        }
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.withType<Wrapper> {
        gradleVersion = "6.3"
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isWarnings = false
        options.compilerArgs.add("-Xlint:-deprecation")
        options.compilerArgs.add("-Xlint:-unchecked")
    }

}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }
}



