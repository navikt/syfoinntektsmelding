import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val version = "1.0"
val mainClass = "no.nav.syfo.AppKt"
val group = "no.nav.syfo"
val githubPassword: String by project

// Dependencies
val micrometerVersion: String by project
val flywayVersion: String by project
val cxfVersion: String by project
val kotlinVersion: String by project
val hikariVersion: String by project
val ktorVersion: String by project
val koinVersion: String by project
val tokenSupportVersion: String by project
val mockOAuth2ServerVersion: String by project
val brukernotifikasjonSchemasVersion: String by project
val jacksonVersion: String by project
val junitJupiterVersion: String by project
val assertJVersion: String by project
val prometheusVersion: String by project
val joarkHendelseVersion: String by project
val mockkVersion: String by project
val jetbrainsStdLib: String by project
val guavaVersion: String by project
val kotlinLibsVersion: String by project
val postgresVersion: String by project
val neethiVersion: String by project
val imkontraktVersion: String by project
val altinnInntektsmeldingVersion: String by project
val navSyfoCommonVersion: String by project
val vaultJdbcVersion: String by project
val navLogVersion: String by project
val fellesBackendVersion: String by project
val helseArbeidsgiverUtilsVersion: String by project
val slf4Version: String by project
val logbackVersion: String by project
val apacheHttpClientVersion: String by project
val kotlinCoroutinesVersion: String by project
val kafkaVersion: String by project
val apacheKafkaStreamsVersion: String by project
val annotationApiVersion: String by project
val altinnCorrespondanceVersion: String by project
val inntektsmeldingVersion: String by project

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.github.ben-manes.versions") version "0.44.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.flywaydb.flyway") version "9.19.2"
    jacoco
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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
    maven {
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/helsearbeidsgiver-inntektsmelding")
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
    implementation("no.nav.helsearbeidsgiver:utils:$helseArbeidsgiverUtilsVersion")
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
    implementation("no.nav.helsearbeidsgiver.inntektsmelding:dokument:$inntektsmeldingVersion")
}
