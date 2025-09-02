package com.lion.be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@Profile("local")
public class WebSocketLocalConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${mq.stomp.host}")
    private String host;

    @Value("${mq.stomp.port}")
    private int port;

    @Value("${mq.stomp.username}")
    private String username;

    @Value("${mq.stomp.password}")
    private String password;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Application Destination Prefix 설정
        registry.setApplicationDestinationPrefixes("/app");

        // STOMP Broker Relay 설정
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(host)
                .setRelayPort(port)
                .setSystemLogin(username)
                .setSystemPasscode(password)
                .setClientLogin(username)
                .setClientPasscode(password);
    }
}
