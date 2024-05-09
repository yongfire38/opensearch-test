package egovframework.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
	@Bean
	public OpenAPI api() {
		Info info = new Info()
                .title("Open Search 연동 Test API Document")
                .version("v0.0.1")
                .description("Open Search - Spring Boot 연동의 API 명세서입니다.");
        return new OpenAPI()
                .components(new Components())
                .info(info);
	}
}
