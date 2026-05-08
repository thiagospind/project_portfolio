package com.codegroup.portfolio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI portfolioOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portfolio API")
                        .version("1.0.0")
                        .description("API de gerenciamento de portfólio de projetos.")
                        .contact(new Contact().name("Thiago Spindola").email("..."))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Credenciais: admin / admin@portfolio")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }

}
