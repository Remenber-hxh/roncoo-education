package com.roncoo.education.gateway.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由
 * <p>
 * 二开：roncoo 原本把路由放在 Nacos 配置中心，仓库未开源该配置，此处按服务端口重建。
 * 地址走配置项而非硬编码，换服务器或调整端口时只改配置、不用重新编译。
 * 默认值即本机开发环境，生产在 application-prod.properties 或外部配置里覆盖。
 *
 * @author roncoo (二开补充)
 */
@Configuration
public class RouteConfig {

    @Value("${roncoo.route.system:http://127.0.0.1:7710}")
    private String systemUri;

    @Value("${roncoo.route.user:http://127.0.0.1:7720}")
    private String userUri;

    @Value("${roncoo.route.course:http://127.0.0.1:7730}")
    private String courseUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("service-system", r -> r.path("/system/**").uri(systemUri))
                .route("service-user", r -> r.path("/user/**").uri(userUri))
                .route("service-course", r -> r.path("/course/**").uri(courseUri))
                .build();
    }
}
