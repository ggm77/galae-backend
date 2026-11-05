package com.seohamin.galae.global.infra.ai;

/**
 * LLM 응답
 *
 * @param content 생성된 텍스트
 * @param model   실제로 응답한 모델 ID
 * @param cost    호출 비용 (비즈라우터 usage.cost, 없으면 0)
 */
public record AiCompletion(String content, String model, double cost) {}
