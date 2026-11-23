package com.barangay.zshared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI / Swagger 3 UI.
 * Enables interactive API documentation with JWT Bearer authentication support.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @org.springframework.beans.factory.annotation.Value("${app.openapi.contact-name:Administrator}")
    private String contactName;

    @org.springframework.beans.factory.annotation.Value("${app.openapi.contact-email:admin@barangayugong.gov.ph}")
    private String contactEmail;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UgongConnect: Barangay Ugong Management System API")
                        .description("Official RESTful API documentation for the digital governance and administration of Barangay Ugong, Valenzuela City. Provides comprehensive endpoints for secure authentication, blotter management, Lupon conciliation, VAWC and BCPC child/women case tracking, barangay clearances, First-Time Jobseekers (FTJS) processing, resident demographic profiling, and administrative workflows.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name(contactName)
                                .email(contactEmail))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT Bearer token obtained from /api/v1/auth/login")));
    }
}
