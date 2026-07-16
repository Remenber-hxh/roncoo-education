package com.roncoo.education.gateway.common;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地二开：网关路由（原配置在 Nacos，缺失，此处按服务端口重建）
 * /system/** -> 7710, /user/** -> 7720, /course/** -> 7730
 *
 * @author roncoo (二开补充)
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("service-system", r -> r.path("/system/**").uri("http://127.0.0.1:7710"))
                .route("service-user", r -> r.path("/user/**").uri("http://127.0.0.1:7720"))
                .route("service-course", r -> r.path("/course/**").uri("http://127.0.0.1:7730"))
                .build();
    }
}
