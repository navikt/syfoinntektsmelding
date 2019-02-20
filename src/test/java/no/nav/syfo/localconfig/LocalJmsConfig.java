package no.nav.syfo.localconfig;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.mq.MQErrorHandler;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.NamingException;

@Configuration
@Slf4j
@EnableJms
public class LocalJmsConfig {

    @Bean(name = "jmsListenerContainerFactory")
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory, Destination destination, JmsTransactionManager jmsTransactionManager, MQErrorHandler errorHandler) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDestinationResolver((session, s, b) -> destination);
        factory.setConcurrency("3-10");
        factory.setTransactionManager(jmsTransactionManager);
        factory.setErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public Destination inntektsmeldingDestination() {
        return new ActiveMQQueue("inntektsmelding");
    }

    @Bean(name = "inntektsmeldingQueue")
    public JmsTemplate inntektsmeldingQueue(ConnectionFactory jmsConnectionFactory) throws NamingException {
        JmsTemplate jmsTemplate = new JmsTemplate(jmsConnectionFactory);
        jmsTemplate.setDefaultDestination(inntektsmeldingDestination());
        return jmsTemplate;
    }

    @Bean
    public JmsTransactionManager jmsTransactionManager(ConnectionFactory connectionFactory) {
        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
        jmsTransactionManager.setConnectionFactory(connectionFactory);
        jmsTransactionManager.setDefaultTimeout(300);
        return jmsTransactionManager;
    }
}
