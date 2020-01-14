package no.nav.syfo.config.mq;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import no.nav.syfo.consumer.mq.MQErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.support.destination.DestinationResolver;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;

import static com.ibm.mq.constants.CMQC.MQENC_NATIVE;
import static com.ibm.msg.client.wmq.WMQConstants.*;

@Configuration
@EnableJms
@Profile({"remote"})
public class JmsConfig {
    private static final int UTF_8_WITH_PUA = 1208;

    @Value("${syfo.mottak.inntektsmelding.queuename}")
    private String inntektsmeldingQueuename;
    @Value("${syfoinntektsmelding.channel.name}")
    private String channelName;
    @Value("${mqgateway03.hostname}")
    private String gatewayHostname;
    @Value("${mqgateway03.name}")
    private String gatewayName;
    @Value("${mqgateway03.port}")
    private int gatewayPort;
    @Value("${srvsyfoinntektsmelding.username}")
    private String srvAppserverUsername;
    @Value("${srvsyfoinntektsmelding.password}")
    private String srvAppserverPassword;

    @Bean
    public Queue inntektsmeldingQueue() throws JMSException {
        return new MQQueue(inntektsmeldingQueuename);
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory, DestinationResolver destinationResolver, JmsTransactionManager jmsTransactionManager, MQErrorHandler errorHandler) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDestinationResolver(destinationResolver);
        factory.setConcurrency("3-10");
        factory.setTransactionManager(jmsTransactionManager);
        factory.setErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public DestinationResolver destinationResolver(ApplicationContext context) {
        return (session, destinationName, pubSubDomain) -> context.getBean(destinationName, Queue.class);
    }

    @Bean
    public ConnectionFactory connectionFactory() throws Exception {
        MQConnectionFactory connectionFactory = new MQConnectionFactory();
        connectionFactory.setHostName(gatewayHostname);
        connectionFactory.setPort(gatewayPort);
        connectionFactory.setChannel(channelName);
        connectionFactory.setQueueManager(gatewayName);
        connectionFactory.setTransportType(WMQ_CM_CLIENT);
        connectionFactory.setCCSID(UTF_8_WITH_PUA);
        connectionFactory.setIntProperty(JMS_IBM_ENCODING, MQENC_NATIVE);
        connectionFactory.setIntProperty(JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA);
        UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
        adapter.setTargetConnectionFactory(connectionFactory);
        adapter.setUsername(srvAppserverUsername);
        adapter.setPassword(srvAppserverPassword);
        return adapter;
    }

    @Bean
    public JmsTransactionManager jmsTransactionManager(ConnectionFactory connectionFactory) {
        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
        jmsTransactionManager.setConnectionFactory(connectionFactory);
        jmsTransactionManager.setDefaultTimeout(300);
        return jmsTransactionManager;
    }
}
