import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val version = "1.0"
val mainClass = "no.nav.syfo.AppKt"
val group = "no.nav.syfo"
val githubPassword: String by project
// Dependencies
val micrometerVersion = "1.8.3"
val flywayVersion = "8.5.10"
val cxfVersion = "3.5.2"
val kotlinVersion = "1.4.10"
val hikariVersion = "5.0.1"
val ktorVersion = "1.6.7"
val koinVersion = "3.1.5"
val tokenSupportVersion = "2.1.0"
val mockOAuth2ServerVersion = "0.4.4"
val brukernotifikasjonSchemasVersion = "1.2021.01.18-11.12-b9c8c40b98d1"
val jacksonVersion = "2.13.3"
val junitJupiterVersion = "5.8.2"
val assertJVersion = "3.23.1"
val prometheusVersion = "0.15.0"
val joarkHendelseVersion = "96ec5ebb"
val mockkVersion = "1.12.2"
val jetbrainsStdLib = "kotlin-stdlib-jdk8"
val guavaVersion = "31.0.1-jre"
val kotlinLibsVersion = "1.7.0"
val postgresVersion = "42.3.1"
val neethiVersion = "3.2.0"
val imkontraktVersion = "2022.02.25-10-37-3934b"
val altinnInntektsmeldingVersion = "1.2021.02.22-10.45-4201aaea72fb"
val navSyfoCommonVersion = "2019.09.25-05-44-08e26429f4e37cd57d99ba4d39fc74099a078b97"
val vaultJdbcVersion = "1.3.7"
val navLogVersion = "2.2022.02.18_14.38-8d8bb494bd41"
val fellesBackendVersion = "2022.07.06-11-34-2e18c"
val slf4Version = "1.7.36"
val logbackVersion = "7.2"
val apacheHttpClientVersion = "4.5.13"
val kotlinCoroutinesVersion = "1.6.0-native-mt"
val kafkaVersion = "7.1.1"
val apacheKafkaStreamsVersion = "7.1.1-ce"
val annotationApiVersion = "1.3.2"
val altinnCorrespondanceVersion = "1.2019.09.25-00.21-49b69f0625e0"

plugins {
    kotlin("jvm") version "1.7.0"
    id("com.github.ben-manes.versions") version "0.42.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("org.flywaydb.flyway") version "8.4.2"
    jacoco
    id("org.sonarqube") version "3.3"
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
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

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_syfoinntektsmelding")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sourceEncoding", "UTF-8")
    }
}

tasks.jar {
    archiveBaseName.set("app")
    manifest {
        attributes["Main-Class"] = mainClass
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

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    implementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:$joarkHendelseVersion")
    implementation("org.jetbrains.kotlin:$jetbrainsStdLib")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinLibsVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinLibsVersion")
    implementation("org.apache.cxf:cxf-core:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-databinding-jaxb:$cxfVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.apache.neethi:neethi:$neethiVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:$imkontraktVersion")
    implementation("no.nav.tjenestespesifikasjoner:nav-altinn-inntektsmelding:$altinnInntektsmeldingVersion")
    implementation("no.nav.syfo.sm:syfosm-common-rest-sts:$navSyfoCommonVersion")
    implementation("no.nav.syfo.sm:syfosm-common-networking:$navSyfoCommonVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("no.nav.common:log:$navLogVersion")
    implementation("no.nav.helsearbeidsgiver:helse-arbeidsgiver-felles-backend:$fellesBackendVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-ktor:$tokenSupportVersion")
    implementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.slf4j:slf4j-api:$slf4Version")
    implementation("net.logstash.logback:logstash-logback-encoder:$logbackVersion")
    implementation("org.apache.httpcomponents:httpclient:$apacheHttpClientVersion")
    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("io.confluent:kafka-streams-avro-serde:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$apacheKafkaStreamsVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.insert-koin:koin-core-jvm:$koinVersion")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:$altinnCorrespondanceVersion")
    implementation("javax.annotation:javax.annotation-api:$annotationApiVersion")
}
