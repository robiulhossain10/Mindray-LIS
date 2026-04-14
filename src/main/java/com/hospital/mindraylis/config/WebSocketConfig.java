package com.hospital.mindraylis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ফ্রন্টএন্ড এই এন্ডপয়েন্টে কানেক্ট হবে (যেমন: http://localhost:8081/ws-lis)
        registry.addEndpoint("/ws-lis").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // সার্ভার থেকে ক্লায়েন্টে ডেটা পাঠানোর প্রিফিক্স
        config.enableSimpleBroker("/topic");
        // ক্লায়েন্ট থেকে সার্ভারে ডেটা পাঠানোর প্রিফিক্স (যদি লাগে)
        config.setApplicationDestinationPrefixes("/app");
    }
}