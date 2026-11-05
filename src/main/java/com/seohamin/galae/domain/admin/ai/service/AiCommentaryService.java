package com.seohamin.galae.domain.admin.ai.service;

import com.seohamin.galae.domain.admin.ai.dto.AiConfigResponseDto;
import com.seohamin.galae.domain.admin.ai.dto.PerspectiveGenerateRequestDto;
import com.seohamin.galae.domain.admin.question.dto.AdminPerspectiveResponseDto;
import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.entity.perspective.Perspective;
import com.seohamin.galae.domain.question.entity.perspective.PerspectiveStatus;
import com.seohamin.galae.domain.question.repository.QuestionRepository;
import com.seohamin.galae.domain.question.repository.perspective.PerspectiveRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import com.seohamin.galae.global.infra.ai.AiClient;
import com.seohamin.galae.global.infra.ai.AiCompletion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 해설 생성 (docs/PLAN.md 5.3 파이프라인)
 *
 * 질문 + 필수 가치 태그 + 출처 목록
 *   → 생성 → 자기비평 1회 → 자동 검증
 *   → 실패하면 사유를 덧붙여 1회 재생성 (다시 자기비평 → 검증)
 *   → 또 실패하면 FAILED로 검수 큐에 올림
 *   → 통과하면 DRAFT (사람이 승인해야 앱에 노출)
 *
 * 인용 구절 원문은 모델이 쓰지 않고, 모델이 고른 ref에 맞춰 입력 목록의 원문을 그대로 붙인다 (7.1 동일성유지권)
 * AI 호출은 오래 걸리므로 DB 트랜잭션 밖에서 하고, 읽기·저장만 짧은 트랜잭션으로 묶는다
 */
@Slf4j
@Service
public class AiCommentaryService {

    private static final int MAX_ATTEMPTS = 2;
    private static final int MAX_BODY_COLUMN = 2000;

    private final AiClient aiClient;
    private final QuestionRepository questionRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate transactionTemplate;

    public AiCommentaryService(
            final AiClient aiClient,
            final QuestionRepository questionRepository,
            final PerspectiveRepository perspectiveRepository,
            final JsonMapper jsonMapper,
            final PlatformTransactionManager transactionManager
    ) {
        this.aiClient = aiClient;
        this.questionRepository = questionRepository;
        this.perspectiveRepository = perspectiveRepository;
        this.jsonMapper = jsonMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public AiConfigResponseDto getConfig() {
        return new AiConfigResponseDto(aiClient.isConfigured(), aiClient.defaultModel(), CommentaryPrompts.PROMPT_VERSION);
    }

    /**
     * 해설 한 건을 생성해 DRAFT(통과) 또는 FAILED(검증 2회 실패)로 저장한다
     */
    public AdminPerspectiveResponseDto generate(final String questionId, final PerspectiveGenerateRequestDto requestDto) {

        if (!aiClient.isConfigured()) {
            throw new CustomException(ExceptionCode.AI_NOT_CONFIGURED);
        }

        // 1) 입력 준비 (질문은 공개 전이어도 된다 - 미리 만들어 두고 검수하는 용도)
        final CommentaryContext ctx = transactionTemplate.execute(status -> context(questionId, requestDto));
        final String model = requestDto.getModel() == null || requestDto.getModel().isBlank()
                ? aiClient.defaultModel()
                : requestDto.getModel();

        // 2) 생성 → 자기비평 → 검증 (최대 2번)
        double cost = 0;
        String respondedModel = model;
        String lastContent = null;
        JsonNode result = null;
        List<String> errors = List.of();
        final List<Map<String, Object>> attempts = new ArrayList<>();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            final AiCompletion draft = aiClient.complete(
                    model,
                    CommentaryPrompts.generateSystem(ctx.label()),
                    CommentaryPrompts.generateUser(ctx, errors),
                    CommentaryPrompts.GENERATE_TEMPERATURE
            );
            final AiCompletion critiqued = aiClient.complete(
                    model,
                    CommentaryPrompts.critiqueSystem(),
                    CommentaryPrompts.critiqueUser(ctx, draft.content()),
                    CommentaryPrompts.CRITIQUE_TEMPERATURE
            );
            cost += draft.cost() + critiqued.cost();
            respondedModel = critiqued.model();

            //자기비평 결과를 우선 쓰고, 읽을 수 없으면 초안을 쓴다
            JsonNode parsed = CommentaryValidator.parse(jsonMapper, critiqued.content());
            lastContent = critiqued.content();
            if (parsed == null) {
                parsed = CommentaryValidator.parse(jsonMapper, draft.content());
                lastContent = draft.content();
            }

            result = parsed;
            errors = CommentaryValidator.validate(parsed, ctx);
            attempts.add(Map.of("attempt", attempt, "errors", errors));

            if (errors.isEmpty()) {
                break;
            }
            log.info("AI 해설 검증 실패 question={} label={} attempt={} errors={}", questionId, ctx.label(), attempt, errors);
        }

        // 3) 저장
        final PerspectiveStatus status = errors.isEmpty() ? PerspectiveStatus.DRAFT : PerspectiveStatus.FAILED;
        final Perspective perspective = toPerspective(ctx, result, lastContent, status, respondedModel,
                detail(ctx, result, errors, attempts, respondedModel, cost));

        return transactionTemplate.execute(tx -> {
            final Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new CustomException(ExceptionCode.QUESTION_NOT_EXIST));
            question.addPerspective(perspective);
            return new AdminPerspectiveResponseDto(perspectiveRepository.save(perspective));
        });
    }

    private CommentaryContext context(final String questionId, final PerspectiveGenerateRequestDto requestDto) {
        final Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(ExceptionCode.QUESTION_NOT_EXIST));

        final List<CommentaryContext.ChoiceItem> choices = question.getChoices().stream()
                .map(c -> new CommentaryContext.ChoiceItem(CommentaryContext.optionOf(c.getChoiceIndex()), c.getChoiceIndex(), c.getText()))
                .toList();

        //같은 ref가 여러 번 오면 처음 것만
        final Map<String, CommentaryContext.SourceItem> sources = new LinkedHashMap<>();
        for (final PerspectiveGenerateRequestDto.SourceItem s : Optional.ofNullable(requestDto.getSources()).orElse(List.of())) {
            sources.putIfAbsent(s.getRef().trim(), new CommentaryContext.SourceItem(s.getRef().trim(), s.getText().trim(),
                    s.getNote() == null ? null : s.getNote().trim()));
        }

        return new CommentaryContext(
                question.getId(),
                question.getTitle(),
                question.getBody(),
                choices,
                requestDto.getLabel().trim(),
                requestDto.getValueTags().stream().map(String::trim).distinct().toList(),
                List.copyOf(sources.values())
        );
    }

    private Perspective toPerspective(
            final CommentaryContext ctx,
            final JsonNode result,
            final String lastContent,
            final PerspectiveStatus status,
            final String model,
            final String detail
    ) {
        String body = result == null ? "" : CommentaryValidator.text(result.path("body")).trim();
        if (body.isEmpty()) {
            //JSON을 읽지 못한 실패: 검수자가 원문을 볼 수 있게 응답을 그대로 남긴다
            body = lastContent == null ? "(응답 없음)" : lastContent.trim();
        }
        if (body.length() > MAX_BODY_COLUMN) {
            body = body.substring(0, MAX_BODY_COLUMN);
        }

        //모델이 고른 ref 중 목록에 있는 첫 번째를 인용 구절로. 원문은 입력 그대로
        final Map<String, CommentaryContext.SourceItem> byRef = ctx.sourcesByRef();
        final Optional<CommentaryContext.SourceItem> quote = result == null ? Optional.empty()
                : CommentaryValidator.strings(result.path("sources")).stream().map(byRef::get).filter(Objects::nonNull).findFirst();

        final String openQuestion = result == null ? null : CommentaryValidator.text(result.path("open_question")).trim();

        return Perspective.builder()
                .label(ctx.label())
                .body(body)
                .verse(quote.map(CommentaryContext.SourceItem::text).orElse(null))
                .ref(quote.map(CommentaryContext.SourceItem::ref).orElse(null))
                .openQuestion(openQuestion == null || openQuestion.isEmpty() ? null : truncate(openQuestion, 300))
                .sortOrder(nextSortOrder(ctx.questionId()))
                .status(status)
                .model(truncate(model, 50))
                .promptVersion(CommentaryPrompts.PROMPT_VERSION)
                .generationDetail(detail)
                .build();
    }

    private int nextSortOrder(final String questionId) {
        return transactionTemplate.execute(tx -> questionRepository.findById(questionId)
                .map(q -> q.getPerspectives().stream().mapToInt(Perspective::getSortOrder).max().orElse(-1) + 1)
                .orElse(0));
    }

    /**
     * 검수자가 볼 생성 근거와 검증 결과
     */
    private String detail(
            final CommentaryContext ctx,
            final JsonNode result,
            final List<String> errors,
            final List<Map<String, Object>> attempts,
            final String model,
            final double cost
    ) {
        final Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("promptVersion", CommentaryPrompts.PROMPT_VERSION);
        detail.put("model", model);
        detail.put("generatedAt", LocalDateTime.now().toString());
        detail.put("requiredTags", ctx.valueTags());
        detail.put("allowedSources", ctx.sources().stream().map(CommentaryContext.SourceItem::ref).toList());

        if (result != null) {
            detail.put("valuesInTension", CommentaryValidator.strings(result.path("values_in_tension")));
            final Map<String, CommentaryContext.ChoiceItem> choices = new HashMap<>();
            ctx.choices().forEach(c -> choices.put(c.option(), c));
            final List<Map<String, Object>> cases = new ArrayList<>();
            for (final JsonNode item : result.path("for_each_option")) {
                final String option = CommentaryValidator.text(item.path("option")).trim().toUpperCase(Locale.ROOT);
                final Map<String, Object> c = new LinkedHashMap<>();
                c.put("option", option);
                c.put("choiceId", choices.containsKey(option) ? choices.get(option).choiceId() : null);
                c.put("choiceText", choices.containsKey(option) ? choices.get(option).text() : null);
                c.put("strongestCase", CommentaryValidator.text(item.path("strongest_case")).trim());
                cases.add(c);
            }
            detail.put("forEachOption", cases);
            detail.put("sources", CommentaryValidator.strings(result.path("sources")));
        }

        detail.put("errors", errors);
        detail.put("attempts", attempts);
        detail.put("cost", Math.round(cost * 10000) / 10000.0);
        return jsonMapper.writeValueAsString(detail);
    }

    private static String truncate(final String value, final int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
