import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val version = "1.0"
val mainClass = "no.nav.syfo.AppKt"
val group = "no.nav.syfo"

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jmailen.kotlinter")
    id("com.github.ben-manes.versions")
    jacoco
    application
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.test {
    useJUnitPlatform()
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
    include("**/*Test.class")
    exclude("**/*Spec.class")
}

task<Test>("slowTests") {
    include("**/*Spec.class")
    exclude("**/*Test.class")
    outputs.upToDateWhen { false }
    group = "verification"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jar {
    archiveBaseName.set("app")
    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] =
            configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
    }
    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file =
                layout.buildDirectory
                    .file("libs/${it.name}")
                    .get()
                    .asFile
            if (!file.exists()) {
                it.copyTo(file)
            }
        }
    }
}

repositories {
    val githubPassword: String by project
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven {
        setUrl("https://maven.pkg.github.com/navikt/*")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}

dependencies {
    val altinnCorrespondanceVersion: String by project
    val altinnInntektsmeldingVersion: String by project
    val annotationApiVersion: String by project
    val apacheHttpClientVersion: String by project
    val apacheKafkaStreamsVersion: String by project
    val assertJVersion: String by project
    val bakgrunnsjobbVersion: String by project
    val brukernotifikasjonSchemasVersion: String by project
    val cxfVersion: String by project
    val fellesBackendVersion: String by project
    val flywayVersion: String by project
    val guavaVersion: String by project
    val hagDomeneInntektsmeldingVersion: String by project
    val hagUtilsVersion: String by project
    val hikariVersion: String by project
    val imkontraktVersion: String by project
    val jacksonVersion: String by project
    val jetbrainsStdLib: String by project
    val joarkHendelseVersion: String by project
    val junitJupiterVersion: String by project
    val kafkaVersion: String by project
    val koinVersion: String by project
    val kotlinCoroutinesVersion: String by project
    val kotlinLibsVersion: String by project
    val kotlinVersion: String by project
    val kotlinxSerializationVersion: String by project
    val ktorVersion: String by project
    val logbackClassicVersion: String by project
    val logbackVersion: String by project
    val micrometerVersion: String by project
    val mockOAuth2ServerVersion: String by project
    val mockkVersion: String by project
    val navLogVersion: String by project
    val navSyfoCommonVersion: String by project
    val neethiVersion: String by project
    val oppgaveClientVersion: String by project
    val pdlClientVersion: String by project
    val postgresVersion: String by project
    val prometheusVersion: String by project
    val slf4Version: String by project
    val tokenSupportVersion: String by project
    val nettyVersion: String by project
    val swaggerVersion: String by project

    runtimeOnly("ch.qos.logback:logback-classic:$logbackClassicVersion")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("io.confluent:kafka-avro-serializer:$kafkaVersion") { exclude("org.apache.avro:avro") }
    implementation("io.confluent:kafka-streams-avro-serde:$kafkaVersion")
    implementation("io.insert-koin:koin-core-jvm:$koinVersion")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-locations:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-apache5:$ktorVersion")
    implementation("io.ktor:ktor-server:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    implementation("io.mockk:mockk:$mockkVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("javax.annotation:javax.annotation-api:$annotationApiVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logbackVersion")
    implementation("no.nav.helsearbeidsgiver:domene-inntektsmelding:$hagDomeneInntektsmeldingVersion")
    implementation("no.nav.helsearbeidsgiver:hag-bakgrunnsjobb:$bakgrunnsjobbVersion")
    implementation("no.nav.helsearbeidsgiver:oppgave-client:$oppgaveClientVersion")
    implementation("no.nav.helsearbeidsgiver:pdl-client:$pdlClientVersion")
    implementation("no.nav.helsearbeidsgiver:utils:$hagUtilsVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    implementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:$imkontraktVersion")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:$joarkHendelseVersion")
    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:$altinnCorrespondanceVersion")
    implementation("no.nav.tjenestespesifikasjoner:nav-altinn-inntektsmelding:$altinnInntektsmeldingVersion")
    implementation("org.apache.cxf:cxf-core:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-databinding-jaxb:$cxfVersion")
    implementation("org.apache.httpcomponents:httpclient:$apacheHttpClientVersion")
    implementation("org.apache.kafka:kafka-streams:$apacheKafkaStreamsVersion")
    implementation("org.apache.neethi:neethi:$neethiVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("org.jetbrains.kotlin:$jetbrainsStdLib")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinLibsVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinLibsVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.slf4j:slf4j-api:$slf4Version")
    implementation("io.netty:netty-common:$nettyVersion")
    implementation("io.swagger.core.v3:swagger-annotations:$swaggerVersion")

    testImplementation(testFixtures("no.nav.helsearbeidsgiver:utils:$hagUtilsVersion"))
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
}
