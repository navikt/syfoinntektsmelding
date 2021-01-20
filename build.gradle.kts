import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springBootVersion = "2.4.2"
val springVersion = "5.3.3"
val springKafkaVersion = "2.6.5"
val micrometerVersion = "1.3.0"
val flywayVersion = "6.1.4"
val cxfVersion = "3.4.2"
val swaggerVersion = "2.10.0"
val kotlinVersion = "1.4.10"
val hikariVersion = "3.4.2"
val ktorVersion = "1.4.3"

val mainClass = "no.nav.syfo.Application"

val githubPassword: String by project

plugins {
    "maven-publish"
    id("org.jetbrains.kotlin.jvm") version "1.4.10"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.4.10"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.4.10"
    id("org.flywaydb.flyway") version "5.1.4"
    id("org.sonarqube") version "3.0"
    java
    jacoco
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_syfoinntektsmelding")
        property("sonar.organization", "navit")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.exclusions", "**/Koin*,**Mock**,**/App**")
    }
}

tasks.jacocoTestReport {
    executionData("build/jacoco/test.exec")
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

tasks.withType<JacocoReport> {
    classDirectories.setFrom(
            sourceSets.main.get().output.asFileTree.matching {
                exclude( "**/App**", "**Mock**")
            }
    )
}


buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
        classpath("org.jetbrains.kotlin:kotlin-noarg:1.4.10")
    }
}

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

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

allOpen {
    annotation("org.springframework.context.annotation.Configuration")
    annotation("org.springframework.stereotype.Service")
    annotation("org.springframework.stereotype.Component")
}



dependencies {

    // SNYK overrides
    implementation("commons-collections:commons-collections:3.2.2")
    // - end SNYK overrides
    implementation("com.vladmihalcea:hibernate-types-52:2.10.2") {

    }

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")

    // Spring
    implementation("io.springfox:springfox-swagger2:$swaggerVersion")
    implementation("io.springfox:springfox-swagger-ui:$swaggerVersion")
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-logging:$springBootVersion")
    implementation("org.springframework:spring-tx:$springVersion")
    implementation("org.springframework:spring-beans:$springVersion")
    implementation("org.springframework:spring-web:$springVersion")
    implementation("org.springframework:spring-core:$springVersion")
    implementation("org.springframework:spring-context:$springVersion")
    implementation("org.springframework:spring-jdbc:$springVersion")
    implementation("org.springframework.kafka:spring-kafka:$springKafkaVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-test:$springBootVersion")
    testImplementation("org.springframework:spring-test:$springVersion")

    implementation("javax.inject:javax.inject:1")
    implementation("jakarta.activation:jakarta.activation-api:1.2.1")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:2.3.1")
    implementation("jakarta.xml.ws:jakarta.xml.ws-api:2.3.2")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:2.1.6")
    implementation("com.sun.activation:javax.activation:1.2.0")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.10")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-security:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-policy:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-simple:$cxfVersion")
    implementation("org.apache.cxf:cxf-core:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-bindings-soap:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-databinding-jaxb:$cxfVersion")
    runtimeOnly("org.apache.cxf:cxf-spring-boot-starter-jaxws:$cxfVersion")
    runtimeOnly("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.postgresql:postgresql:42.2.13")
    implementation("org.apache.neethi:neethi:3.1.0")
    implementation("org.flywaydb:flyway-core:$flywayVersion")

    // NAV
    implementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2020.04.06-12-19-94de1")
    implementation("no.nav.tjenestespesifikasjoner:nav-inngaaendeJournal-v1-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:nav-behandleInngaaendeJournal-v1-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:behandlesak-v2-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:oppgavebehandling-v3-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:nav-journal-v2-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:nav-altinn-inntektsmelding:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:arbeidsfordeling-v1-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:dial-nav-tjeneste-aktoer_v2:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:person-v3-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:diskresjonskodev1-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.syfo.sm:syfosm-common-rest-sts:2019.09.03-10-50-64032e3b6381665e9f9c0914cef626331399e66d")
    implementation("no.nav.syfo.sm:syfosm-common-networking:2019.09.03-10-50-64032e3b6381665e9f9c0914cef626331399e66d")
    implementation("no.nav:migrator:0.2.2")
    implementation("no.nav:vault-jdbc:1.3.1")
    implementation("no.nav.common:log:2.2021.01.05_08.07-2c586ccadf95")

    implementation("com.zaxxer:HikariCP:$hikariVersion")

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("net.logstash.logback:logstash-logback-encoder:6.4")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-core:3.1.0")
    testImplementation("org.assertj:assertj-core:3.11.1")
    compileOnly("org.projectlombok:lombok:1.18.8")
    annotationProcessor("org.projectlombok:lombok:1.18.8")
    testCompileOnly("org.projectlombok:lombok:1.18.8")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.8")
    implementation("com.google.guava:guava:30.0-jre")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("io.confluent:kafka-streams-avro-serde:5.2.1")
}

tasks.named<Jar>("jar") {
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

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.suppressWarnings = true
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.suppressWarnings = true
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isWarnings = false
    options.compilerArgs.add("-Xlint:-deprecation")
    options.compilerArgs.add("-Xlint:-unchecked")
}
