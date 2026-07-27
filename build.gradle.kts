plugins {
	java
	id("org.springframework.boot") version "3.5.11"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.ject"
version = "0.0.1-SNAPSHOT"
description = "vs-server"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(Dependencies.SpringBoot.DATA_JPA)
	implementation(Dependencies.SpringBoot.SECURITY)
	implementation(Dependencies.SpringBoot.VALIDATION)
	implementation(Dependencies.SpringBoot.WEB)
	implementation(Dependencies.SpringBoot.ACTUATOR)
	implementation(Dependencies.SpringBoot.OAUTH2_CLIENT)
	implementation(Dependencies.SpringBoot.WEBSOCKET)

	// Jwt
	implementation(Dependencies.Jwt.API)
	runtimeOnly(Dependencies.Jwt.IMPL)
	runtimeOnly(Dependencies.Jwt.JACKSON)

	// Database
	runtimeOnly(Dependencies.Database.POSTGRESQL)
	runtimeOnly(Dependencies.Database.H2)
	implementation(Dependencies.Database.FLYWAY)
	runtimeOnly(Dependencies.Database.FLYWAY_POSTGRESQL)

	// Swagger / OpenAPI
	implementation(Dependencies.Swagger.SPRINGDOC)

	// Firebase (FCM)
	implementation(Dependencies.Firebase.ADMIN)

	// AWS S3
	implementation(Dependencies.Aws.S3)

	// Google Gemini AI
	implementation(Dependencies.Ai.GEMINI)

	// Cache (Caffeine)
	implementation(Dependencies.Cache.CAFFEINE)

	compileOnly(Dependencies.Lombok.LOMBOK)
	annotationProcessor(Dependencies.Lombok.LOMBOK)
	testImplementation(Dependencies.SpringBoot.TEST)
	testImplementation(Dependencies.SpringSecurity.TEST)
	testRuntimeOnly(Dependencies.Test.JUNIT_LAUNCHER)

	// Testcontainers (for Postgres-specific integration tests)
	testImplementation("org.testcontainers:testcontainers:1.21.3")
	testImplementation("org.testcontainers:junit-jupiter:1.21.3")
	testImplementation("org.testcontainers:postgresql:1.21.3")

	// Spring Boot official Testcontainers support
	testImplementation("org.springframework.boot:spring-boot-testcontainers:3.5.11")
}

tasks.test {
	useJUnitPlatform()
	outputs.upToDateWhen { false }
	outputs.cacheIf { false }

	// 통합 테스트는 Testcontainers 기반 실제 PostgreSQL을 사용한다.
	// Docker가 없는 환경에서는 -PskipIntegrationTests로 제외한다.
	if (project.hasProperty("skipIntegrationTests")) {
		exclude("**/*IntegrationTest.class")
	}
}
