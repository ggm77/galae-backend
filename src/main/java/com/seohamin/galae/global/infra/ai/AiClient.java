package com.seohamin.galae.global.infra.ai;

/**
 * LLM 호출 (해설 생성용)
 * 구현은 비즈라우터(BizRouterClient). 테스트에서는 목으로 바꾼다
 */
public interface AiClient {

    /**
     * API 키가 설정되어 있는지
     */
    boolean isConfigured();

    /**
     * 기본 모델 ID (예: anthropic/claude-sonnet-5.5)
     */
    String defaultModel();

    /**
     * 시스템 프롬프트와 사용자 프롬프트로 한 번 생성한다
     * @param model 모델 ID. null이면 기본 모델
     * @param temperature 0.3~0.5 권장 (docs/PLAN.md 5.5)
     */
    AiCompletion complete(String model, String systemPrompt, String userPrompt, double temperature);
}
