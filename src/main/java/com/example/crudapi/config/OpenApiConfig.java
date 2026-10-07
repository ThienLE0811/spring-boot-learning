package com.example.crudapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI crudApiOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("CRUD API - Spring Boot learning")
                .version("v1")
                .description("API CRUD mau cho muc dich hoc Spring Boot + PostgreSQL"));
    }
}
