package com.seohamin.galae.domain.question.service;

import com.seohamin.galae.domain.answer.entity.Answer;
import com.seohamin.galae.domain.answer.repository.AnswerRepository;
import com.seohamin.galae.domain.question.dto.*;
import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.entity.QuestionStatus;
import com.seohamin.galae.domain.question.entity.choice.Choice;
import com.seohamin.galae.domain.question.entity.perspective.PerspectiveStatus;
import com.seohamin.galae.domain.question.repository.QuestionRepository;
import com.seohamin.galae.domain.question.repository.perspective.PerspectiveRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import com.seohamin.galae.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 질문 조회
 * 결과·의견·확장하기는 내 답을 먼저 남겨야 열린다 (docs/PLAN.md 원칙 2, docs/APP_FLOW.md 5-3)
 * 화면에서만 잠그면 보안 경계가 아니므로 서버에서 막는다 (docs/APP_FLOW.md 7)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private static final int MAX_OPINION_PAGE_SIZE = 50;

    private final QuestionRepository questionRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final AnswerRepository answerRepository;
    private final TimeProvider timeProvider;

    /**
     * 공개된 질문을 찾는 메서드. 공개 전이거나 없는 질문은 존재하지 않는 것으로 본다
     * 다른 도메인(답변, 그룹)에서도 쓴다
     */
    public Question getVisibleQuestion(final String questionId) {
        return questionRepository.findById(questionId)
                .filter(q -> q.getStatus() == QuestionStatus.PUBLISHED)
                .filter(q -> !q.getPublishDate().isAfter(timeProvider.today()))
                .orElseThrow(() -> new CustomException(ExceptionCode.QUESTION_NOT_EXIST));
    }

    /**
     * 질문 탭 목록
     * @param query 제목·본문 검색어 (없으면 전체)
     * @param filter all, unanswered, answered
     */
    public List<QuestionSummaryResponseDto> getQuestions(final Long userId, final String query, final String filter) {

        // 1) 공개된 질문 조회
        final String trimmed = query == null ? "" : query.trim();
        final List<Question> questions = trimmed.isEmpty()
                ? questionRepository.findVisible(QuestionStatus.PUBLISHED, timeProvider.today())
                : questionRepository.searchVisible(QuestionStatus.PUBLISHED, timeProvider.today(), trimmed);
        if (questions.isEmpty()) {
            return List.of();
        }

        // 2) 참여 수와 내 답변을 한 번에 조회
        final List<String> ids = questions.stream().map(Question::getId).toList();
        final Map<String, Long> counts = participantCounts(ids);
        final Map<String, Answer> myAnswers = answerRepository.findAllByUserIdAndQuestionIdIn(userId, ids).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity()));

        // 3) 답변 상태 필터
        return questions.stream()
                .filter(q -> switch (filter == null ? "all" : filter) {
                    case "answered" -> myAnswers.containsKey(q.getId());
                    case "unanswered" -> !myAnswers.containsKey(q.getId());
                    default -> true;
                })
                .map(q -> new QuestionSummaryResponseDto(q, counts.getOrDefault(q.getId(), 0L), myAnswers.get(q.getId())))
                .toList();
    }

    /**
     * 오늘의 질문. 오늘 날짜 질문이 없으면 가장 최근 질문
     */
    public QuestionResponseDto getTodayQuestion(final Long userId) {
        final Question question = findTodayQuestion()
                .orElseThrow(() -> new CustomException(ExceptionCode.QUESTION_NOT_EXIST));

        return toDetail(question, userId);
    }

    /**
     * 오늘의 질문 엔티티. 그룹 카드의 '오늘 나눈 멤버'에서도 쓴다
     */
    public Optional<Question> findTodayQuestion() {
        return questionRepository
                .findFirstByStatusAndPublishDateLessThanEqualOrderByPublishDateDesc(QuestionStatus.PUBLISHED, timeProvider.today());
    }

    /**
     * 질문 상세 (선택 화면)
     */
    public QuestionResponseDto getQuestion(final String questionId, final Long userId) {
        return toDetail(getVisibleQuestion(questionId), userId);
    }

    /**
     * 결과: 선택별 수와 비율
     */
    public QuestionResultResponseDto getResult(final String questionId, final Long userId) {

        // 1) 질문 조회와 답변 확인
        final Question question = getVisibleQuestion(questionId);
        final Answer myAnswer = requireAnswer(userId, questionId);

        // 2) 선택지별 응답 수
        final Map<Integer, Long> counts = new HashMap<>();
        for (final Object[] row : answerRepository.countByChoice(questionId)) {
            counts.put((Integer) row[0], (Long) row[1]);
        }
        final long total = counts.values().stream().mapToLong(Long::longValue).sum();

        // 3) 비율 계산
        final List<Choice> choices = question.getChoices();
        final int[] pcts = percentages(choices.stream().mapToLong(c -> counts.getOrDefault(c.getChoiceIndex(), 0L)).toArray());

        final List<QuestionResultResponseDto.ChoiceResult> results = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            final Choice choice = choices.get(i);
            results.add(new QuestionResultResponseDto.ChoiceResult(choice, counts.getOrDefault(choice.getChoiceIndex(), 0L), pcts[i]));
        }

        return new QuestionResultResponseDto(questionId, total, myAnswer.getChoice().getChoiceIndex(), results);
    }

    /**
     * 선택지 코멘트: 그 선택지를 고른 다른 사람들의 이유 (익명)
     */
    public OpinionListResponseDto getOpinions(
            final String questionId,
            final Integer choiceId,
            final Long userId,
            final int page,
            final int size
    ) {
        if (page < 0 || size < 1 || size > MAX_OPINION_PAGE_SIZE) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST, "page는 0 이상, size는 1~" + MAX_OPINION_PAGE_SIZE + "이어야 합니다.");
        }

        // 1) 질문·선택지·내 답변 확인
        final Question question = getVisibleQuestion(questionId);
        question.findChoice(choiceId).orElseThrow(() -> new CustomException(ExceptionCode.CHOICE_NOT_EXIST));
        final Answer myAnswer = requireAnswer(userId, questionId);

        // 2) 내가 고른 선택지면 내 이유를 맨 위에
        final String myReason = myAnswer.getChoice().getChoiceIndex().equals(choiceId) && !myAnswer.getReason().isEmpty()
                ? myAnswer.getReason()
                : null;

        // 3) 다른 사람의 이유
        final Slice<Answer> slice = answerRepository.findOpinions(questionId, choiceId, userId, PageRequest.of(page, size));

        return new OpinionListResponseDto(
                questionId,
                choiceId,
                myReason,
                slice.getContent().stream().map(Answer::getReason).toList(),
                page,
                slice.hasNext()
        );
    }

    /**
     * 확장하기: 승인된 관점별 해설
     */
    public ReadingResponseDto getReading(final String questionId, final Long userId) {
        getVisibleQuestion(questionId);
        requireAnswer(userId, questionId);

        return new ReadingResponseDto(
                questionId,
                perspectiveRepository.findAllByQuestionIdAndStatusOrderBySortOrderAscIdAsc(questionId, PerspectiveStatus.APPROVED)
        );
    }

    private QuestionResponseDto toDetail(final Question question, final Long userId) {
        final long count = answerRepository.countByQuestionId(question.getId());
        final Answer answer = answerRepository.findByUserIdAndQuestionId(userId, question.getId()).orElse(null);
        return new QuestionResponseDto(question, count, answer);
    }

    private Answer requireAnswer(final Long userId, final String questionId) {
        return answerRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseThrow(() -> new CustomException(ExceptionCode.ANSWER_REQUIRED));
    }

    private Map<String, Long> participantCounts(final List<String> questionIds) {
        final Map<String, Long> counts = new HashMap<>();
        for (final Object[] row : answerRepository.countByQuestionIds(questionIds)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }

    /**
     * 정수 비율을 최대 잉여 방식으로 계산해 합이 100이 되게 한다
     * 응답이 없으면 모두 0
     */
    static int[] percentages(final long[] counts) {
        final long total = Arrays.stream(counts).sum();
        final int[] result = new int[counts.length];
        if (total == 0) {
            return result;
        }

        int assigned = 0;
        final double[] remainders = new double[counts.length];
        for (int i = 0; i < counts.length; i++) {
            final double exact = counts[i] * 100.0 / total;
            result[i] = (int) Math.floor(exact);
            remainders[i] = exact - result[i];
            assigned += result[i];
        }

        final Integer[] order = new Integer[counts.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(remainders[b], remainders[a]));
        for (int k = 0; k < 100 - assigned; k++) {
            result[order[k % order.length]]++;
        }
        return result;
    }
}
