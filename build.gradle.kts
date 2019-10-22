import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springBootVersion = "2.2.0.RELEASE"
val springVersion = "5.2.0.RELEASE"
val springKafkaVersion = "2.3.1.RELEASE"
val micrometerVersion = "1.3.0"

val mainClass = "no.nav.syfo.Application"

val githubPassword: String by project

plugins {
    "maven-publish"
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.50"
    id("org.flywaydb.flyway") version "6.0.6"
    java
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

repositories {
    mavenCentral()
    maven("https://kotlin.bintray.com/ktor")
    maven("http://packages.confluent.io/maven/")
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
    implementation("org.springframework:spring-jms:$springVersion")
    implementation("org.springframework.kafka:spring-kafka:$springKafkaVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-activemq:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-test:$springBootVersion")
    testImplementation("org.springframework:spring-test:$springVersion")

    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("javax.inject:javax.inject:1")

    implementation("com.sun.xml.ws:jaxws-ri:2.3.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.41")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.41")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")

    implementation("org.apache.cxf:cxf-rt-ws-security:3.3.3")
    implementation("org.apache.cxf:cxf-rt-ws-policy:3.3.3")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:3.3.3")
    implementation("org.apache.cxf:cxf-rt-frontend-simple:3.3.3")
    implementation("org.apache.cxf:cxf-rt-security:3.3.3")
    implementation("org.apache.cxf:cxf-core:3.3.3")
    implementation("org.apache.cxf:cxf-rt-bindings-soap:3.3.3")
    implementation("org.apache.cxf:cxf-rt-databinding-jaxb:3.3.3")
    runtime("org.apache.cxf:cxf-spring-boot-starter-jaxws:3.3.3")
    runtime("org.apache.cxf:cxf-rt-features-logging:3.3.3")

    runtime("com.oracle.ojdbc:ojdbc10:19.3.0.0")

    implementation("org.apache.neethi:neethi:3.1.0")

    implementation("org.flywaydb:flyway-core:6.0.6")

    implementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2019.10.14-12-21-local-build")

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

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("net.logstash.logback:logstash-logback-encoder:4.10")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.0.4.0")
    implementation("javax.jms:javax.jms-api:2.0.1")
    testImplementation("org.apache.activemq:activemq-client:5.15.6")

    implementation("org.apache.httpcomponents:httpclient:4.5.6")

    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    runtime("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    implementation("io.ktor:ktor-client-core:1.2.4")
    implementation("io.ktor:ktor:1.2.4")
    implementation("io.ktor:ktor-client-jackson:1.2.4")
    implementation("io.ktor:ktor-client-apache:1.2.4")

    implementation("no.nav.syfo.sm:syfosm-common-rest-sts:2019.09.03-10-50-64032e3b6381665e9f9c0914cef626331399e66d")
    implementation("no.nav.syfo.sm:syfosm-common-networking:2019.09.03-10-50-64032e3b6381665e9f9c0914cef626331399e66d")

    testImplementation("org.springframework.kafka:spring-kafka-test:2.1.10.RELEASE")

    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.23.4")
    testImplementation("org.assertj:assertj-core:3.11.1")

    implementation("com.h2database:h2:1.4.199")

    compileOnly("org.projectlombok:lombok:1.18.8")
    annotationProcessor("org.projectlombok:lombok:1.18.8")
    testCompileOnly("org.projectlombok:lombok:1.18.8")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.8")
}

tasks.named<Jar>("jar") {
    baseName = "app"

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
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "11"
}
