package no.nav.syfo.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun logger(name: String): Logger =
    LoggerFactory.getLogger(name)
