import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val version = "1.0"
val mainClass = "no.nav.syfo.AppKt"
val group = "no.nav.syfo"
// Dependencies
val micrometerVersion = "1.8.3"
val flywayVersion = "8.4.2"
val cxfVersion = "3.4.4"
val kotlinVersion = "1.4.10"
val hikariVersion = "5.0.1"
val ktorVersion = "1.6.7"
val koinVersion = "3.1.5"
val tokenSupportVersion = "2.0.8"
val mockOAuth2ServerVersion = "0.4.4"
val brukernotifikasjonSchemasVersion = "1.2021.01.18-11.12-b9c8c40b98d1"
val jacksonVersion = "2.13.1"
val junitJupiterVersion = "5.8.2"
val assertJVersion = "3.22.0"
val prometheusVersion = "0.15.0"
val joarkHendelseVersion = "96ec5ebb"
val githubPassword: String by project

plugins {
    application
    kotlin("jvm") version "1.5.30"
    id("org.sonarqube") version "3.3"
    id("com.github.ben-manes.versions") version "0.42.0"
    id("org.flywaydb.flyway") version "8.4.2"
    jacoco
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

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "11"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "11"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.test {
    useJUnitPlatform()
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

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.5".toBigDecimal()
            }
        }
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

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_syfoinntektsmelding")
        property("sonar.organization", "navit")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.2".toBigDecimal()
            }
        }
    }
}

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    implementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.mockk:mockk:1.12.2")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:$joarkHendelseVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
    implementation("org.apache.cxf:cxf-core:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-databinding-jaxb:$cxfVersion")
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("org.apache.neethi:neethi:3.2.0")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2022.02.25-10-37-3934b")
    implementation("no.nav.tjenestespesifikasjoner:nav-altinn-inntektsmelding:1.2021.02.22-10.45-4201aaea72fb")
    implementation("no.nav.syfo.sm:syfosm-common-rest-sts:2019.09.25-05-44-08e26429f4e37cd57d99ba4d39fc74099a078b97")
    implementation("no.nav.syfo.sm:syfosm-common-networking:2019.09.25-05-44-08e26429f4e37cd57d99ba4d39fc74099a078b97")
    implementation("no.nav:vault-jdbc:1.3.7")
    implementation("no.nav.common:log:2.2022.02.18_14.38-8d8bb494bd41")
    implementation("no.nav.helsearbeidsgiver:helse-arbeidsgiver-felles-backend:2022.01.18-08-47-f6aa0")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-ktor:$tokenSupportVersion")
    implementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("net.logstash.logback:logstash-logback-encoder:7.0.1")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt")
    implementation("io.confluent:kafka-streams-avro-serde:6.2.1")
    implementation("io.confluent:kafka-avro-serializer:6.2.1")
    implementation("org.apache.kafka:kafka-streams:7.0.1-ce")
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
    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:1.2019.09.25-00.21-49b69f0625e0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}
