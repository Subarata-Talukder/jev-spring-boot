package subarata.softlabio.jevspringboot.integration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import subarata.softlabio.jevspringboot.config.JevProperties;
import subarata.softlabio.jevspringboot.exception.EvaluationException;
import subarata.softlabio.jevspringboot.exception.JevIntegrationException;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JevClient {

    private final RestClient restClient;
    private final JevProperties properties;

    public JevClient(
            RestClient jevRestClient,
            JevProperties properties
    ) {
        this.restClient = jevRestClient;
        this.properties = properties;
    }

    public JsonNode evaluate(String state, Map<String, Object> questions) {
        if (!properties.hasApiKey()) {
            throw new EvaluationException("Jev API key is not configured");
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.model());
        request.put("state", state);
        request.put("questions", questions);

        int attempts = Math.max(1, properties.maxAttempts());

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                JsonNode response = restClient.post()
                        .uri("/v1/systemone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.setBearerAuth(properties.apiKey()))
                        .body(request)
                        .retrieve()
                        .body(JsonNode.class);

                validateResponse(response, questions);
                return response;

            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();

                if (isRetryable(status) && attempt < attempts) {
                    pause(attempt);
                    continue;
                }

                throw new JevIntegrationException("Jev API request failed with HTTP " + status, status, ex);

            } catch (ResourceAccessException ex) {
                if (attempt < attempts) {
                    pause(attempt);
                    continue;
                }

                throw new JevIntegrationException("Jev API connection failed or timed out", 0, ex);
            }
        }

        throw new JevIntegrationException("Jev API request failed after retries", 0);
    }

    private void validateResponse(JsonNode response, Map<String, Object> questions) {
        if (response == null
                || !response.isObject()
                || !response.path("model").isString()
                || !response.path("answers").isObject()) {
            throw new JevIntegrationException("Jev returned an invalid response envelope", 502);
        }

        JsonNode answers = response.path("answers");

        for (String questionId : questions.keySet()) {
            JsonNode answer = answers.get(questionId);

            if (answer == null
                    || !answer.isObject()
                    || !answer.path("type").isString()) {
                throw new JevIntegrationException("Jev response is missing answer: " + questionId, 502);
            }
        }
    }

    private boolean isRetryable(int status) {
        return status == 408
                || status == 429
                || status == 500
                || status == 502
                || status == 503
                || status == 504
                || status == 529;
    }

    private void pause(int attempt) {
        long delayMillis = Math.min(2000L, 200L * (1L << (attempt - 1)));

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new JevIntegrationException("Interrupted while waiting to retry Jev", 0, ex);
        }
    }
}