package com.trao.tripplanner.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// SOLID-SRP: Handles only OpenAPI/Swagger documentation configuration
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Trip Planner API")
                .description("AI-powered travel itinerary generator using Grok (xAI). " +
                        "Generate day-by-day itineraries, budget estimates, and hotel suggestions.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("Trao Assessment")
                        .email("dev@trao.com"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT token obtained from /api/v1/auth/login");
    }
}
