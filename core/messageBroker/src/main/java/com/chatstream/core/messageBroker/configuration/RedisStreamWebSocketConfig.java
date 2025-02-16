package com.chatstream.core.messageBroker.configuration;

import com.chatstream.core.messageBroker.handler.RedisStreamWebSocketHandler;
import com.chatstream.core.messageBroker.service.RedisStreamService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisStreamWebSocketConfig {

    private final RedisStreamService redisStreamService;

    public RedisStreamWebSocketConfig(RedisStreamService redisStreamService) {
        this.redisStreamService = redisStreamService;
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/stream", new RedisStreamWebSocketHandler(redisStreamService));

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(map);
        handlerMapping.setOrder(-1); // Before annotated controllers
        return handlerMapping;
    }
}