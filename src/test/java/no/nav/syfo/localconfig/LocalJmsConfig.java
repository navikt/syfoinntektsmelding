package no.nav.syfo.localconfig;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.NamingException;

@Configuration
@Slf4j
@EnableJms
public class LocalJmsConfig {

    @Bean(name = "jmsListenerContainerFactory")
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory xaJmsConnectionFactory, Destination destination, PlatformTransactionManager platformTransactionManager) throws NamingException {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(xaJmsConnectionFactory);
        factory.setDestinationResolver((session, s, b) -> destination);
        factory.setConcurrency("3-10");
        factory.setTransactionManager(platformTransactionManager);
        return factory;
    }

    @Bean
    public Destination minDestination() throws NamingException {
        log.info("Oppretter koeoe");
        return new ActiveMQQueue("min");
    }

    @Bean(name = "minqueue")
    public JmsTemplate minqueue(ConnectionFactory xaJmsConnectionFactory) throws NamingException {
        JmsTemplate jmsTemplate = new JmsTemplate(xaJmsConnectionFactory);
        jmsTemplate.setDefaultDestination(minDestination());
        return jmsTemplate;
    }
}
