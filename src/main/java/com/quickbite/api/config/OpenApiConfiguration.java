package com.quickbite.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    public static final String BEARER_AUTH = "bearerAuth";
        private static final String ERROR_SCHEMA = "#/components/schemas/ApiErrorResponse";

    @Bean
    public OpenAPI quickBiteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuickBite API")
                        .version("v1")
                        .description("Food delivery API for customers, restaurant owners, and administrators."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

        @Bean
        public OpenApiCustomizer apiErrorResponses() {
                return openApi -> {
                        openApi.getComponents().addSchemas("ApiErrorResponse", new ObjectSchema()
                                        .addProperty("timestamp", new StringSchema().format("date-time"))
                                        .addProperty("status", new IntegerSchema())
                                        .addProperty("error", new StringSchema())
                                        .addProperty("message", new StringSchema())
                                        .addProperty("path", new StringSchema())
                                        .addProperty("errors", new MapSchema().additionalProperties(new StringSchema())));

                        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap()
                                        .forEach((method, operation) -> addErrorResponses(path, method, operation)));
                };
        }

        private static void addErrorResponses(String path, PathItem.HttpMethod method, Operation operation) {
                add(operation, "400", "Invalid request or validation failure");
                add(operation, "404", "Requested resource was not found");
                add(operation, "409", "Request conflicts with current resource state");
                if (path.equals("/api/orders") && method == PathItem.HttpMethod.POST) {
                        add(operation, "422", "Order cannot be processed with the current cart state");
                }
                boolean publicRead = method == PathItem.HttpMethod.GET
                                && (path.startsWith("/api/restaurants/") || path.equals("/api/restaurants"));
                boolean publicAuth = path.startsWith("/api/auth/");
                if (!publicRead && !publicAuth) {
                        add(operation, "401", "Authentication is required");
                        add(operation, "403", "The authenticated role or owner is not permitted");
                }
                add(operation, "500", "Unexpected server error");
        }

        private static void add(Operation operation, String status, String description) {
                operation.getResponses().addApiResponse(status, new ApiResponse()
                                .description(description)
                                .content(new Content().addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref(ERROR_SCHEMA)))));
        }
}
