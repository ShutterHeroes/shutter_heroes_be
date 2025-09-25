package com.example.demo.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server().url("http://localhost:8080").description("로컬 개발 서버")
            ));
    }

    private Info apiInfo() {
        return new Info()
            .title("Shutter Heroes API")
            .description("Shutter Heroes 백엔드 API 문서")
            .version("1.0.0");
    }
}
