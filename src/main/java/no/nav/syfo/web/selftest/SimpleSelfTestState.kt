package no.nav.syfo.web.selftest;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class SimpleSelfTestState {

    public boolean IS_ALIVE = true;

    public boolean IS_READY = true;
}
