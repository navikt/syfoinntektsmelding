package no.nav.syfo.web.selftest

import lombok.extern.slf4j.Slf4j
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate

@Slf4j
@RestController
@RequestMapping(value = ["/internal"])
class SelftestController {
    @Autowired
    private val template: JdbcTemplate? = null

    @get:RequestMapping(value = ["/isAlive"], produces = [MediaType.TEXT_PLAIN_VALUE])
    @get:ResponseBody
    val isAlive: String
        get() {
            checkDatabase()
            return APPLICATION_LIVENESS
        }

    @get:RequestMapping(value = ["/isReady"], produces = [MediaType.TEXT_PLAIN_VALUE])
    @get:ResponseBody
    val isReady: String
        get() {
            checkDatabase()
            return APPLICATION_READY
        }

    private fun checkDatabase() {
        template!!.execute("select now();")
    }

    companion object {
        private const val APPLICATION_LIVENESS = "Application is alive!"
        private const val APPLICATION_READY = "Application is ready!"
    }
}
