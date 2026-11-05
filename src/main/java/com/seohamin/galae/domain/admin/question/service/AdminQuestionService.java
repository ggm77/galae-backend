package com.seohamin.galae.domain.admin.question.service;

import com.seohamin.galae.domain.admin.question.dto.*;
import com.seohamin.galae.domain.answer.repository.AnswerRepository;
import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.entity.QuestionStatus;
import com.seohamin.galae.domain.question.entity.choice.Choice;
import com.seohamin.galae.domain.question.entity.perspective.Perspective;
import com.seohamin.galae.domain.question.entity.perspective.PerspectiveStatus;
import com.seohamin.galae.domain.question.repository.QuestionRepository;
import com.seohamin.galae.domain.question.repository.perspective.PerspectiveRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 관리자: 질문 등록과 해설 검수 (docs/PLAN.md 5.3 - draft → approved, 9.1 검수 페이지)
 */
@Service
@RequiredArgsConstructor
public class AdminQuestionService {

    private final QuestionRepository questionRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final AnswerRepository answerRepository;
    private final EntityManager entityManager;

    /**
     * 모든 질문 (최신순, 공개 전 포함)
     */
    @Transactional(readOnly = true)
    public List<AdminQuestionResponseDto> getQuestions() {
        return questionRepository.findAllByOrderByPublishDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 질문 등록
     */
    @Transactional
    public AdminQuestionResponseDto createQuestion(final AdminQuestionRequestDto requestDto) {

        // 1) ID·날짜 중복 확인 (하루에 질문 하나)
        if (questionRepository.existsById(requestDto.getId()) || questionRepository.existsByPublishDate(requestDto.getDate())) {
            throw new CustomException(ExceptionCode.QUESTION_ALREADY_EXIST);
        }

        // 2) 질문과 선택지 저장
        final Question question = Question.builder()
                .id(requestDto.getId())
                .publishDate(requestDto.getDate())
                .title(requestDto.getTitle().trim())
                .body(requestDto.getBody().trim())
                .status(Optional.ofNullable(requestDto.getStatus()).orElse(QuestionStatus.DRAFT))
                .build();
        addChoices(question, requestDto.getChoices());

        return toResponse(questionRepository.save(question));
    }

    /**
     * 질문 수정
     * 선택지는 아직 답변이 없을 때만 바꿀 수 있다 (선택지 번호가 답변·공유와 이어져 있음)
     */
    @Transactional
    public AdminQuestionResponseDto updateQuestion(final String questionId, final AdminQuestionRequestDto requestDto) {
        final Question question = findQuestion(questionId);

        if (requestDto.getDate() != null && !requestDto.getDate().equals(question.getPublishDate())) {
            if (questionRepository.existsByPublishDateAndIdNot(requestDto.getDate(), questionId)) {
                throw new CustomException(ExceptionCode.QUESTION_ALREADY_EXIST);
            }
            question.updatePublishDate(requestDto.getDate());
        }
        Optional.ofNullable(requestDto.getTitle()).map(String::trim).filter(s -> !s.isEmpty()).ifPresent(question::updateTitle);
        Optional.ofNullable(requestDto.getBody()).map(String::trim).filter(s -> !s.isEmpty()).ifPresent(question::updateBody);
        Optional.ofNullable(requestDto.getStatus()).ifPresent(question::updateStatus);

        if (requestDto.getChoices() != null) {
            if (answerRepository.countByQuestionId(questionId) > 0) {
                throw new CustomException(ExceptionCode.QUESTION_HAS_ANSWERS);
            }
            //유니크 제약(question_id, choice_index) 때문에 지운 뒤 flush하고 새로 넣는다
            question.getChoices().clear();
            entityManager.flush();
            addChoices(question, requestDto.getChoices());
        }

        return toResponse(question);
    }

    /**
     * 해설 등록 (DRAFT)
     */
    @Transactional
    public AdminPerspectiveResponseDto createPerspective(final String questionId, final AdminPerspectiveRequestDto requestDto) {
        final Question question = findQuestion(questionId);

        final Perspective perspective = Perspective.builder()
                .label(requestDto.getLabel().trim())
                .body(requestDto.getBody().trim())
                .verse(requestDto.getVerse())
                .ref(requestDto.getRef())
                .openQuestion(requestDto.getOpenQuestion())
                .sortOrder(Optional.ofNullable(requestDto.getSortOrder()).orElse(question.getPerspectives().size()))
                .status(PerspectiveStatus.DRAFT)
                .model(requestDto.getModel())
                .promptVersion(requestDto.getPromptVersion())
                .build();
        question.addPerspective(perspective);

        return new AdminPerspectiveResponseDto(perspectiveRepository.save(perspective));
    }

    /**
     * 해설 수정. 다시 DRAFT가 된다
     */
    @Transactional
    public AdminPerspectiveResponseDto updatePerspective(final Long perspectiveId, final AdminPerspectiveRequestDto requestDto) {
        final Perspective perspective = findPerspective(perspectiveId);

        perspective.updateContent(
                requestDto.getLabel().trim(),
                requestDto.getBody().trim(),
                requestDto.getVerse(),
                requestDto.getRef(),
                requestDto.getOpenQuestion(),
                Optional.ofNullable(requestDto.getSortOrder()).orElse(perspective.getSortOrder()),
                requestDto.getModel(),
                requestDto.getPromptVersion()
        );

        return new AdminPerspectiveResponseDto(perspective);
    }

    /**
     * 검수 큐 (오래된 순)
     */
    @Transactional(readOnly = true)
    public List<AdminPerspectiveResponseDto> getPerspectives(final PerspectiveStatus status) {
        return perspectiveRepository.findAllByStatusOrderByCreatedAtAsc(status).stream()
                .map(AdminPerspectiveResponseDto::new)
                .toList();
    }

    /**
     * 승인·반려. 부적절한 해설 신고 시 반려로 즉시 비공개 전환할 때도 쓴다 (docs/PLAN.md 5.10)
     */
    @Transactional
    public AdminPerspectiveResponseDto reviewPerspective(
            final Long perspectiveId,
            final PerspectiveReviewRequestDto requestDto,
            final Long reviewerId
    ) {
        if (requestDto.getStatus() != PerspectiveStatus.APPROVED && requestDto.getStatus() != PerspectiveStatus.REJECTED) {
            throw new CustomException(ExceptionCode.INVALID_REVIEW_STATUS);
        }

        final Perspective perspective = findPerspective(perspectiveId);
        perspective.review(requestDto.getStatus(), reviewerId);

        return new AdminPerspectiveResponseDto(perspective);
    }

    private void addChoices(final Question question, final List<AdminQuestionRequestDto.ChoiceItem> choices) {
        for (int i = 0; i < choices.size(); i++) {
            question.addChoice(Choice.builder()
                    .choiceIndex(i)
                    .text(choices.get(i).getText().trim())
                    .shortText(choices.get(i).getShortText().trim())
                    .build());
        }
    }

    private AdminQuestionResponseDto toResponse(final Question question) {
        final Map<Integer, Long> counts = new HashMap<>();
        for (final Object[] row : answerRepository.countByChoice(question.getId())) {
            counts.put((Integer) row[0], (Long) row[1]);
        }
        final long total = counts.values().stream().mapToLong(Long::longValue).sum();
        final long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        final int maxPct = total == 0 ? 0 : (int) Math.round(max * 100.0 / total);

        return new AdminQuestionResponseDto(question, total, maxPct);
    }

    private Question findQuestion(final String questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(ExceptionCode.QUESTION_NOT_EXIST));
    }

    private Perspective findPerspective(final Long perspectiveId) {
        return perspectiveRepository.findById(perspectiveId)
                .orElseThrow(() -> new CustomException(ExceptionCode.PERSPECTIVE_NOT_EXIST));
    }
}
