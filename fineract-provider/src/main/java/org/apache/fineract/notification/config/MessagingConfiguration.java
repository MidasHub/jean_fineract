/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.notification.config;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.fineract.notification.eventandlistener.NotificationEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Configuration
@Profile("activeMqEnabled")
public class MessagingConfiguration {

    @Autowired
    private Environment env;

    @Autowired
    private NotificationEventListener notificationEventListener;

    @Bean
    public Logger loggerBean() {
        return LoggerFactory.getLogger(MessagingConfiguration.class);
    }

    // private static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";
    private static final String DEFAULT_BROKER_URL = "tcp://203.205.21.236:61616";
    // Cần check lại chỗ này. để make sure là hệ thống lấy biến mặc định hay biến kahi báo ở file application.properties

    @Bean
    public ActiveMQConnectionFactory amqConnectionFactory() {
        String endPoint = this.env.getProperty("brokerUrl");
        ActiveMQConnectionFactory amqConnectionFactory = new ActiveMQConnectionFactory();
        try {
            amqConnectionFactory.setBrokerURL(DEFAULT_BROKER_URL);
            amqConnectionFactory.setTrustAllPackages(true); // Khi lên Production cần config lại chỗ này
        } catch (Exception e) {
            // old code: amqConnectionFactory.setBrokerURL(this.env.getProperty("brokerUrl"));
            amqConnectionFactory.setBrokerURL(endPoint);
            amqConnectionFactory.setTrustAllPackages(true);// Khi lên Production cần config lại chỗ này
        }
        return amqConnectionFactory;
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(amqConnectionFactory());
        return connectionFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate;
        jmsTemplate = new JmsTemplate(connectionFactory());
        jmsTemplate.setConnectionFactory(connectionFactory());
        return jmsTemplate;
    }

    @Bean
    public DefaultMessageListenerContainer messageListenerContainer() {

        DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();
        messageListenerContainer.setConnectionFactory(connectionFactory());
        messageListenerContainer.setDestinationName("NotificationQueue");
        messageListenerContainer.setMessageListener(notificationEventListener);
        messageListenerContainer.setExceptionListener(new ExceptionListener() {

            @Override
            public void onException(JMSException jmse) {
                loggerBean().error("Network Error: ActiveMQ Broker Unavailable." + jmse);// cài cái biến lỗi để thấy rõ
                // thông tin
                messageListenerContainer.shutdown();
            }
        });
        return messageListenerContainer;
    }

}
