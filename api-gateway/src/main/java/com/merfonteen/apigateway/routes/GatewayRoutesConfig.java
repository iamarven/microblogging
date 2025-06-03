package com.merfonteen.apigateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://user-service"))
                .route("post-service", r -> r.path("/api/posts/**")
                        .uri("lb://post-service"))
                .route("feed-service", r -> r.path("/api/feed/**")
                        .uri("lb://feed-service"))
                .route("like-service", r -> r.path("/api/likes/**")
                        .uri("lb://like-service"))
                .route("notification-service", r -> r.path("/api/notifications/**")
                        .uri("lb://notification-service"))
                .route("comment-service", r -> r.path("/api/comments/**")
                        .uri("lb://comment-service"))
                .build();
    }
}
