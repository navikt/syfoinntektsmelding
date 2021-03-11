package no.nav.syfo.web.selftest

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class SimpleSelfTestState {
    var IS_ALIVE = true
    var IS_READY = true
}
