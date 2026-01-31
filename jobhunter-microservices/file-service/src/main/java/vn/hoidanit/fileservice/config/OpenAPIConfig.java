package vn.hoidanit.fileservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI fileServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("File Service API")
                        .description("File Storage and Management Service (MinIO) for JobHunter Microservices")
                        .version("1.0")
                        .contact(new Contact()
                                .name("JobHunter Team")
                                .email("htqhuy1762@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://choosealicense.com/licenses/mit/")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("File Service - Development"),
                        new Server().url("http://localhost:8080").description("API Gateway - Development")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token for authentication. Example: 'Bearer {token}'")));
    }
}
