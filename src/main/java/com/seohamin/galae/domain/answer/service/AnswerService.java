package com.seohamin.galae.domain.answer.service;

import com.seohamin.galae.domain.answer.dto.AnswerProgressRequestDto;
import com.seohamin.galae.domain.answer.dto.AnswerRequestDto;
import com.seohamin.galae.domain.answer.dto.AnswerResponseDto;
import com.seohamin.galae.domain.answer.dto.MyAnswerItemResponseDto;
import com.seohamin.galae.domain.answer.entity.Answer;
import com.seohamin.galae.domain.answer.repository.AnswerRepository;
import com.seohamin.galae.domain.group.repository.member.GroupMemberRepository;
import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.entity.choice.Choice;
import com.seohamin.galae.domain.question.service.QuestionService;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import com.seohamin.galae.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 나의 선택과 이유 (docs/APP_FLOW.md 5. 답변과 공유 규칙)
 */
@Service
@RequiredArgsConstructor
public class AnswerService {

    private static final long MAX_RANGE_DAYS = 400;

    private final AnswerRepository answerRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;
    private final TimeProvider timeProvider;

    /**
     * 내 답변 조회
     */
    @Transactional(readOnly = true)
    public AnswerResponseDto getAnswer(final String questionId, final Long userId) {
        questionService.getVisibleQuestion(questionId);
        return new AnswerResponseDto(findAnswer(userId, questionId));
    }

    /**
     * 선택하기
     * 처음 답하면 답한 날을 남기고, 다시 답하면 선택과 이유만 바꾼다.
     * 선택과 이유가 이전과 같으면 아무것도 바뀌지 않는다 (docs/APP_FLOW.md 5-5)
     */
    @Transactional
    public AnswerResponseDto submitAnswer(
            final String questionId,
            final AnswerRequestDto answerRequestDto,
            final Long userId
    ) {
        // 1) 질문과 선택지 확인
        final Question question = questionService.getVisibleQuestion(questionId);
        final Choice choice = question.findChoice(answerRequestDto.getChoiceId())
                .orElseThrow(() -> new CustomException(ExceptionCode.CHOICE_NOT_EXIST));
        final String reason = answerRequestDto.getReason() == null ? "" : answerRequestDto.getReason().trim();

        // 2) 이미 답했으면 다시 하기
        final Answer existing = answerRepository.findByUserIdAndQuestionId(userId, questionId).orElse(null);
        if (existing != null) {
            existing.resubmit(choice, reason);
            return new AnswerResponseDto(existing);
        }

        // 3) 처음 답함
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        final Answer answer = answerRepository.save(Answer.builder()
                .user(user)
                .question(question)
                .choice(choice)
                .reason(reason)
                .answeredOn(timeProvider.today())
                .build());

        return new AnswerResponseDto(answer);
    }

    /**
     * 확장하기에서 다음을 누르면 완료 처리 (docs/APP_FLOW.md 5-4)
     */
    @Transactional
    public AnswerResponseDto completeAnswer(final String questionId, final Long userId) {
        questionService.getVisibleQuestion(questionId);
        final Answer answer = findAnswer(userId, questionId);
        answer.complete();
        return new AnswerResponseDto(answer);
    }

    /**
     * 마지막으로 보던 단계 저장
     */
    @Transactional
    public AnswerResponseDto updateProgress(
            final String questionId,
            final AnswerProgressRequestDto answerProgressRequestDto,
            final Long userId
    ) {
        questionService.getVisibleQuestion(questionId);
        final Answer answer = findAnswer(userId, questionId);

        if (answerProgressRequestDto.getLastStep() != null) {
            answer.updateLastStep(answerProgressRequestDto.getLastStep());
        }

        //공유 초안은 가입한 그룹만 고를 수 있다. 보내기 전에는 그룹에 보이지 않는다
        if (answerProgressRequestDto.getShareDraft() != null) {
            final Set<Long> requested = new LinkedHashSet<>(answerProgressRequestDto.getShareDraft());
            if (requested.contains(null)) {
                throw new CustomException(ExceptionCode.INVALID_REQUEST);
            }
            if (!requested.isEmpty() && new HashSet<>(groupMemberRepository.findJoinedGroupIds(userId, requested)).size() != requested.size()) {
                throw new CustomException(ExceptionCode.GROUP_NOT_JOINED);
            }
            answer.updateShareDraft(requested);
        }

        return new AnswerResponseDto(answer);
    }

    /**
     * 나 탭 달력: 기간 안에 답한 답변
     */
    @Transactional(readOnly = true)
    public List<MyAnswerItemResponseDto> getMyAnswers(final Long userId, final LocalDate from, final LocalDate to) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST, "기간은 " + MAX_RANGE_DAYS + "일 이내여야 합니다.");
        }

        return answerRepository.findAllByUserIdAndAnsweredOnBetween(userId, from, to).stream()
                .map(MyAnswerItemResponseDto::new)
                .toList();
    }

    private Answer findAnswer(final Long userId, final String questionId) {
        return answerRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseThrow(() -> new CustomException(ExceptionCode.ANSWER_NOT_EXIST));
    }
}
