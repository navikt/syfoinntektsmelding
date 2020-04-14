package no.nav.syfo.consumer.graphql

import io.aexp.nodes.graphql.*
import no.nav.syfo.config.PDLConfig
import no.nav.syfo.consumer.rest.TokenConsumer
import org.springframework.stereotype.Component
import java.awt.image.SampleModel
import java.math.BigDecimal


@Component
class PDLClient constructor(
    val config: PDLConfig,
    val tokenConsumer: TokenConsumer
) {
    var graphQLTemplate: GraphQLTemplate = GraphQLTemplate()

    var requestEntity: GraphQLRequestEntity = GraphQLRequestEntity.Builder()
        .url(config.url)
        .variables(Variable("timeFormat", "MM/dd/yyyy"))
        .arguments(Arguments("path.to.argument.property",
            Argument("id", "d070633a9f9")))
        .scalars(BigDecimal::class.java)
        .request(SampleModel::class.java)
        .build()
    var responseEntity = graphQLTemplate.query(requestEntity, SampleModel::class.java)

}
