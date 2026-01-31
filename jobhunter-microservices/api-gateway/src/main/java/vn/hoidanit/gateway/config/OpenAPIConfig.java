package vn.hoidanit.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI apiGatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JobHunter API Gateway")
                        .description("Unified API Gateway for JobHunter Microservices Platform")
                        .version("1.0")
                        .contact(new Contact()
                                .name("JobHunter Team")
                                .email("htqhuy1762@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://choosealicense.com/licenses/mit/")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway - Development")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token for authentication. Format: 'Bearer {token}'")));
    }

    @Bean
    public GroupedOpenApi authServiceApi() {
        return GroupedOpenApi.builder()
                .group("auth-service")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi companyServiceApi() {
        return GroupedOpenApi.builder()
                .group("company-service")
                .pathsToMatch("/api/v1/companies/**")
                .build();
    }

    @Bean
    public GroupedOpenApi jobServiceApi() {
        return GroupedOpenApi.builder()
                .group("job-service")
                .pathsToMatch("/api/v1/jobs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi resumeServiceApi() {
        return GroupedOpenApi.builder()
                .group("resume-service")
                .pathsToMatch("/api/v1/resumes/**")
                .build();
    }

    @Bean
    public GroupedOpenApi fileServiceApi() {
        return GroupedOpenApi.builder()
                .group("file-service")
                .pathsToMatch("/api/v1/files/**", "/storage/**")
                .build();
    }
}
