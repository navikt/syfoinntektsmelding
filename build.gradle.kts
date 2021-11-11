val micrometerVersion = "1.6.3"
val flywayVersion = "6.1.4"
val cxfVersion = "3.4.4"
val kotlinVersion = "1.4.10"
val hikariVersion = "3.4.5"
val ktorVersion = "1.6.5"
val koinVersion = "3.1.2"
val tokenSupportVersion = "1.3.8"
val mockOAuth2ServerVersion = "0.3.4"
val brukernotifikasjonSchemasVersion = "1.2021.01.18-11.12-b9c8c40b98d1"
val jacksonVersion = "2.12.3"
val junitJupiterVersion = "5.7.0"
val assertJVersion = "3.12.2"
val prometheusVersion = "0.6.0"
val githubPassword: String? by project

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.4.20"
    id("com.github.ben-manes.versions") version "0.27.0"
    id("org.flywaydb.flyway") version "5.1.4"
    id("io.snyk.gradle.plugin.snykplugin") version "0.4"
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    mavenCentral()
    google()
    maven(url = "https://packages.confluent.io/maven/")
    maven {
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/inntektsmelding-kontrakt")
    }
    maven {
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/helse-arbeidsgiver-felles-backend")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.guava:guava:30.1.1-jre")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.10")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
    implementation("org.apache.cxf:cxf-core:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-databinding-jaxb:$cxfVersion")
    implementation("org.postgresql:postgresql:42.2.23")
    implementation("org.apache.neethi:neethi:3.1.0")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2020.04.06-12-19-94de1")
    implementation("no.nav.tjenestespesifikasjoner:nav-altinn-inntektsmelding:1.2021.02.22-10.45-4201aaea72fb")
    implementation("no.nav.syfo.sm:syfosm-common-rest-sts:2019.09.03-10-50-64032e3b6381665e9f9c0914cef626331399e66d")
    implementation("no.nav.syfo.sm:syfosm-common-networking:2019.09.03-10-50-64032e3b6381665e9f9c0914cef626331399e66d")
    implementation("no.nav:vault-jdbc:1.3.1")
    implementation("no.nav.common:log:2.2021.01.05_08.07-2c586ccadf95")
    implementation("no.nav.helsearbeidsgiver:helse-arbeidsgiver-felles-backend:2021.06.28-09-42-e08ae")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-ktor:$tokenSupportVersion")
    implementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("net.logstash.logback:logstash-logback-encoder:6.4")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("com.google.guava:guava:30.0-jre")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("io.confluent:kafka-streams-avro-serde:6.2.1")
    implementation("io.confluent:kafka-avro-serializer:6.2.1")
    implementation("org.apache.kafka:kafka-streams:2.8.0")
    testImplementation("io.mockk:mockk:1.11.0")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    implementation("io.insert-koin:koin-core-jvm:$koinVersion")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation(platform("org.junit:junit-bom:$junitJupiterVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:1.2019.09.25-00.21-49b69f0625e0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

tasks.jar {
    archiveBaseName.set("app")
    manifest {
        attributes["Main-Class"] = application.mainClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }
    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "11"
        suppressWarnings = true
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "11"
        suppressWarnings = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<Test>("test") {
    include("no/nav/syfo/**")
    exclude("no/nav/syfo/slowtests/**")
}

task<Test>("slowTests") {
    include("no/nav/syfo/slowtests/**")
    outputs.upToDateWhen { false }
    group = "verification"
}

configure<io.snyk.gradle.plugin.SnykExtension> {
    setSeverity("high")
    setAutoDownload(true)
    setAutoUpdate(true)
    setArguments("--all-sub-projects")
}
