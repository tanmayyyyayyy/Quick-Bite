package com.quickbite.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigurationTest {
    @Test
    void publishesApiMetadataAndJwtBearerScheme() {
        var openApi = new OpenApiConfiguration().quickBiteOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("QuickBite API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openApi.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfiguration.BEARER_AUTH);
        assertThat(openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.BEARER_AUTH).getType())
                .isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.BEARER_AUTH).getScheme())
                .isEqualTo("bearer");
    }
}
