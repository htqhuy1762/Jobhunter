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
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Kafka for job alerts
    implementation("org.springframework.kafka:spring-kafka")

    // Resilience4j for Rate Limiting
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.1.0")

    // Spring Filter for dynamic querying
    implementation("com.turkraft.springfilter:jpa:3.1.7")

    // Database
    runtimeOnly("com.mysql:mysql-connector-j")

    // JWT - Spring Security OAuth2 (includes Nimbus)
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    // Zipkin tracing
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
