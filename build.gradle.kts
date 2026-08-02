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

	// Thymeleaf (운영자용 어드민 페이지)
	implementation(Dependencies.SpringBoot.THYMELEAF)

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

	// 테스트 소스는 현재 없지만, 추가할 때 바로 쓸 수 있도록 기본 스타터는 남겨둔다.
	testImplementation(Dependencies.SpringBoot.TEST)
	testImplementation(Dependencies.SpringSecurity.TEST)
	testRuntimeOnly(Dependencies.Test.JUNIT_LAUNCHER)
}

tasks.test {
	useJUnitPlatform()
}
