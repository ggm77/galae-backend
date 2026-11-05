package com.seohamin.galae.global.infra.ai.bizrouter;

import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import com.seohamin.galae.global.infra.ai.AiClient;
import com.seohamin.galae.global.infra.ai.AiCompletion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 비즈라우터 (OpenAI 호환 Chat Completions)
 * POST {base-url}/chat/completions, Authorization: Bearer {api-key}
 * API 키는 서버 설정(application.yaml)에만 두고 클라이언트로 내보내지 않는다
 */
@Slf4j
@Component
public class BizRouterClient implements AiClient {

    private static final int MAX_TOKENS = 4000;

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;

    public BizRouterClient(
            final JsonMapper jsonMapper,
            @Value("${bizrouter.base-url:https://api.bizrouter.ai/v1}") final String baseUrl,
            @Value("${bizrouter.api-key:}") final String apiKey,
            @Value("${bizrouter.model:anthropic/claude-sonnet-5.5}") final String model,
            @Value("${bizrouter.timeout-seconds:120}") final long timeoutSeconds
    ) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    @Override
    public String defaultModel() {
        return model;
    }

    @Override
    public AiCompletion complete(
            final String requestedModel,
            final String systemPrompt,
            final String userPrompt,
            final double temperature
    ) {
        if (!isConfigured()) {
            throw new CustomException(ExceptionCode.AI_NOT_CONFIGURED);
        }

        final String useModel = requestedModel == null || requestedModel.isBlank() ? model : requestedModel;

        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", useModel);
        body.put("temperature", temperature);
        body.put("max_tokens", MAX_TOKENS);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        final String raw;
        try {
            raw = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(jsonMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            log.warn("BizRouter error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ExceptionCode.AI_REQUEST_FAILED, "AI 호출에 실패했습니다 (" + ex.getStatusCode().value() + "): " + errorMessage(ex.getResponseBodyAsString()));
        } catch (RestClientException ex) {
            log.warn("BizRouter request failed: {}", ex.getMessage());
            throw new CustomException(ExceptionCode.AI_REQUEST_FAILED, "AI 서버에 연결하지 못했습니다.");
        }

        final JsonNode root = jsonMapper.readTree(raw == null ? "{}" : raw);
        final JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isString() || content.asString().isBlank()) {
            throw new CustomException(ExceptionCode.AI_REQUEST_FAILED, "AI 응답이 비어 있습니다.");
        }

        return new AiCompletion(
                content.asString(),
                root.path("model").isString() ? root.path("model").asString() : useModel,
                root.path("usage").path("cost").isNumber() ? root.path("usage").path("cost").asDouble() : 0
        );
    }

    //오류 응답에서 사람이 읽을 메시지만 꺼낸다 (키 등 민감 정보는 응답에 없음)
    private String errorMessage(final String body) {
        try {
            final JsonNode node = jsonMapper.readTree(body);
            final JsonNode message = node.path("error").path("message");
            if (message.isString()) {
                return message.asString();
            }
        } catch (RuntimeException ignored) {
            // JSON이 아니면 앞부분만
        }
        return body == null ? "" : body.substring(0, Math.min(body.length(), 200));
    }
}
