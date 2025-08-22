package com.lion.be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;

@Configuration
//@Profile("!test")
@Profile("prod")
public class WebSocketProdConfig implements WebSocketMessageBrokerConfigurer {

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

        // SSL/TLS를 지원하는 Netty TcpClient 생성
        TcpClient tcpClient = TcpClient.create()
                .host(host)
                .port(port)
                .secure(SslProvider.defaultClientProvider());

        StompReactorNettyCodec codec = new StompReactorNettyCodec();

        ReactorNettyTcpClient<byte[]> reactorNettyTcpClient = new ReactorNettyTcpClient<>(tcpClient, codec);

        // STOMP Broker Relay 설정
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setTcpClient(reactorNettyTcpClient)
                .setSystemLogin(username)
                .setSystemPasscode(password)
                .setClientLogin(username)
                .setClientPasscode(password);
    }

}