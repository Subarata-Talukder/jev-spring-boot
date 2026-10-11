package subarata.softlabio.jevspringboot.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "jev")
public record JevProperties(
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String model,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Positive int maxAttempts
) {
    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }
}