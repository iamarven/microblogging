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
                // --- USER SERVICE ---
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://user-service"))
                .route("user-service-docs", r -> r.path("/v3/api-docs/users")
                        .filters(f -> f.rewritePath("/v3/api-docs/users", "/v3/api-docs"))
                        .uri("lb://user-service"))

                // --- POST SERVICE ---
                .route("post-service", r -> r.path("/api/posts/**")
                        .uri("lb://post-service"))
                .route("post-service-docs", r -> r.path("/v3/api-docs/posts")
                        .filters(f -> f.rewritePath("/v3/api-docs/posts", "/v3/api-docs"))
                        .uri("lb://post-service"))

                // --- FEED SERVICE ---
                .route("feed-service", r -> r.path("/api/feed/**")
                        .uri("lb://feed-service"))
                .route("feed-service-docs", r -> r.path("/v3/api-docs/feed")
                        .filters(f -> f.rewritePath("/v3/api-docs/feed", "/v3/api-docs"))
                        .uri("lb://feed-service"))

                // --- LIKE SERVICE ---
                .route("like-service", r -> r.path("/api/likes/**")
                        .uri("lb://like-service"))
                .route("like-service-docs", r -> r.path("/v3/api-docs/likes")
                        .filters(f -> f.rewritePath("/v3/api-docs/likes", "/v3/api-docs"))
                        .uri("lb://like-service"))

                // --- NOTIFICATION SERVICE ---
                .route("notification-service", r -> r.path("/api/notifications/**")
                        .uri("lb://notification-service"))
                .route("notification-docs", r -> r.path("/v3/api-docs/notifications")
                        .filters(f -> f.rewritePath("/v3/api-docs/notifications", "/v3/api-docs"))
                        .uri("lb://notification-service"))

                // --- COMMENT SERVICE ---
                .route("comment-service", r -> r.path("/api/comments/**")
                        .uri("lb://comment-service"))
                .route("comment-docs", r -> r.path("/v3/api-docs/comments")
                        .filters(f -> f.rewritePath("/v3/api-docs/comments", "/v3/api-docs"))
                        .uri("lb://comment-service"))

                // --- MEDIA SERVICE ---
                .route("media-service", r -> r.path("/api/files/**")
                        .uri("lb://media-service"))
                .route("media-docs", r -> r.path("/v3/api-docs/media")
                        .filters(f -> f.rewritePath("/v3/api-docs/media", "/v3/api-docs"))
                        .uri("lb://media-service"))

                .build();
    }
}
