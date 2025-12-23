package com.go.server.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI goGameOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Go Game Session Server API")
                .description("API for Go Game Session Management and Gameplay")
                .version("v0.0.1"));
    }

    @Bean
    public org.springdoc.core.customizers.OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            var converter = io.swagger.v3.core.converter.ModelConverters.getInstance();
            var schemas = openApi.getComponents().getSchemas();
            
            // Helper to add schema and all its dependencies
            java.util.List.of(
                    com.go.server.game.model.dto.EndGameDto.class,
                    com.go.server.game.model.dto.GameDto.class,
                    com.go.server.game.model.dto.BoardStateDto.class,
                    com.go.server.game.model.dto.IntersectionRowDto.class,
                    com.go.server.game.model.DeviceMove.class,
                    com.go.server.game.session.model.input.CreateSessionDto.class
            ).forEach(clazz -> {
                var resolved = converter.readAll(clazz);
                resolved.forEach(schemas::putIfAbsent);
            });
        };
    }
}
