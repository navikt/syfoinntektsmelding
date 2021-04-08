import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val micrometerVersion = "1.6.3"
val flywayVersion = "6.1.4"
val cxfVersion = "3.4.2"
val swaggerVersion = "2.10.0"
val kotlinVersion = "1.4.10"
val hikariVersion = "3.4.5"
val ktorVersion = "1.4.3"
val koinVersion = "2.0.1"
val tokenSupportVersion = "1.3.1"
val mockOAuth2ServerVersion = "0.2.1"
val brukernotifikasjonSchemasVersion = "1.2021.01.18-11.12-b9c8c40b98d1"
val jacksonVersion = "2.10.3"
val junitJupiterVersion = "5.7.0"
val assertJVersion = "3.12.2"

val mainClass = "no.nav.syfo.AppKt"


val githubPassword: String by project

plugins {
    application
    kotlin("jvm") version "1.4.20"
    id("com.github.ben-manes.versions") version "0.27.0"
    id("org.flywaydb.flyway") version "5.1.4"
    id("org.sonarqube") version "3.0"
    jacoco
}
application {
    mainClassName = mainClass
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
            exclude("**/App**", "**Mock**")
        }
    )
}


buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

repositories {
    jcenter {
        content {
            excludeGroup("no.nav.helsearbeidsgiver")
        }
    }
    mavenCentral {
        content {
            excludeGroup("no.nav.helsearbeidsgiver")
        }
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
        setUrl("https://maven.pkg.github.com/navikt/inntektsmelding-kontrakt")
    }
    maven("https://kotlin.bintray.com/ktor")
    maven("https://packages.confluent.io/maven/")
    maven(url = "https://jitpack.io") {
        content {
            excludeGroup("no.nav.helsearbeidsgiver")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {

    // SNYK overrides
    implementation("commons-collections:commons-collections:3.2.2")
    // - end SNYK overrides

   // implementation("javax.inject:javax.inject:1")
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
    //runtimeOnly("org.apache.cxf:cxf-spring-boot-starter-jaxws:$cxfVersion")
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
    implementation("no.nav.tjenestespesifikasjoner:nav-altinn-inntektsmelding:1.2021.02.22-10.45-4201aaea72fb")
    implementation("no.nav.tjenestespesifikasjoner:arbeidsfordeling-v1-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:dial-nav-tjeneste-aktoer_v2:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:person-v3-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.tjenestespesifikasjoner:diskresjonskodev1-tjenestespesifikasjon:1.2019.08.16-13.46-35cbdfd492d4")
    implementation("no.nav.syfo.sm:syfosm-common-rest-sts:2019.09.03-10-50-64032e3b6381665e9f9c0914cef626331399e66d")
    implementation("no.nav.syfo.sm:syfosm-common-networking:2019.09.03-10-50-64032e3b6381665e9f9c0914cef626331399e66d")
    implementation("no.nav:vault-jdbc:1.3.1")
    implementation("no.nav.common:log:2.2021.01.05_08.07-2c586ccadf95")
    implementation("no.nav.helsearbeidsgiver:helse-arbeidsgiver-felles-backend:2021.03.02-15-19-eb0ee")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-ktor:$tokenSupportVersion")
    implementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    implementation("com.github.navikt:brukernotifikasjon-schemas:$brukernotifikasjonSchemasVersion")
    //NAV

    implementation("com.zaxxer:HikariCP:$hikariVersion")

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("net.logstash.logback:logstash-logback-encoder:6.4")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

   /* compileOnly("org.projectlombok:lombok:1.18.18")
    annotationProcessor("org.projectlombok:lombok:1.18.18")
    testCompileOnly("org.projectlombok:lombok:1.18.18")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.18")*/
    implementation("com.google.guava:guava:30.0-jre")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("io.confluent:kafka-streams-avro-serde:5.5.2")


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

    implementation("org.koin:koin-core:$koinVersion")
    implementation("org.koin:koin-ktor:$koinVersion")
    testImplementation("org.koin:koin-test:$koinVersion")



    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

   /* testImplementation ("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")*/
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation(platform("org.junit:junit-bom:$junitJupiterVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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
tasks.named<KotlinCompile>("compileKotlin")

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.suppressWarnings = true
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.suppressWarnings = true
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

tasks.withType<Wrapper> {
    gradleVersion = "6.8.2"
}

