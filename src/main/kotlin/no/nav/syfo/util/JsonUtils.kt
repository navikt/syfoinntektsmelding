package no.nav.syfo.util

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun customObjectMapper(customPrettyPrinter: Boolean = true): ObjectMapper =
    jacksonObjectMapper().apply {
        registerModules(
            Jdk8Module(),
            JavaTimeModule(),
        )

        enable(SerializationFeature.INDENT_OUTPUT)
        enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)

        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true) // pga aareg

        if (customPrettyPrinter) {
            setDefaultPrettyPrinter(
                DefaultPrettyPrinter().apply {
                    indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                    indentObjectsWith(DefaultIndenter("  ", "\n"))
                },
            )
        }
    }
