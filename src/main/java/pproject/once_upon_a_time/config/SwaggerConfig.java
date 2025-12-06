package pproject.once_upon_a_time.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pproject.once_upon_a_time.global.response.ExceptionDto;

@Configuration
@SecurityScheme(
        name = "JWT TOKEN",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Info info = new Info()
                .title("Once Upon A Time API")
                .description("개발 중인 백엔드 REST API 명세")
                .version("v1.0.0");

        String jwtSchemeName = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new io.swagger.v3.oas.models.security.SecurityScheme()
                        .name(jwtSchemeName)
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    @Bean
    OpenApiCustomizer commonErrorResponses() {
        return openApi -> {
            var modelMap = io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .read(ExceptionDto.class);
            if (openApi.getComponents() != null && modelMap != null) {
                modelMap.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            }

            var errorContent = new Content().addMediaType(
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new io.swagger.v3.oas.models.media.MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ExceptionDto"))
            );

            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(op -> {
                        op.getResponses()
                                .addApiResponse("400", new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("잘못된 파라미터 오류").content(errorContent))
                                .addApiResponse("502", new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("외부 연동 API 오류").content(errorContent))
                                .addApiResponse("500", new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("서버 오류").content(errorContent));
                    })
            );
        };
    }
}
