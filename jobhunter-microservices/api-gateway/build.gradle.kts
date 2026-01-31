plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "vn.hoidanit"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2023.0.0"

dependencies {
    // Spring Cloud Gateway
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    // Service Discovery
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Circuit Breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    // Redis for Rate Limiting
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Security & JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // API Documentation - WebFlux version for reactive gateway
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")

    // Monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    // Zipkin tracing
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")


    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

