package no.nav.syfo.job

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val vaultBase = "/var/run/secrets/nais.io/vault"
val vaultBasePath: Path = Paths.get(vaultBase)


fun readServiceUserCredentials() = ServiceUser(
    username = Files.readString(vaultBasePath.resolve("username")),
    password = Files.readString(vaultBasePath.resolve("password"))
)

fun setUpEnvironment() =
    Environment(
        kafkaBootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS")
    )

data class Environment(
    val kafkaBootstrapServers: String,
    val rapidTopic: String = "helse-rapid-v1",
    val sprearbeidsgivertopic: String = "aapen-helse-spre-arbeidsgiver"
)

data class ServiceUser(
    val username: String,
    val password: String
)
