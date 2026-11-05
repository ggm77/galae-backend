package com.seohamin.galae.domain.group.service.share;

import com.seohamin.galae.domain.answer.entity.Answer;
import com.seohamin.galae.domain.answer.repository.AnswerRepository;
import com.seohamin.galae.domain.group.dto.share.ShareRequestDto;
import com.seohamin.galae.domain.group.dto.share.ShareResponseDto;
import com.seohamin.galae.domain.group.entity.GalaeGroup;
import com.seohamin.galae.domain.group.entity.message.GroupMessage;
import com.seohamin.galae.domain.group.repository.GroupRepository;
import com.seohamin.galae.domain.group.repository.member.GroupMemberRepository;
import com.seohamin.galae.domain.group.repository.message.GroupMessageRepository;
import com.seohamin.galae.domain.question.service.QuestionService;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 나누기 (docs/APP_FLOW.md 5-6 ~ 5-8)
 * 그룹을 고르고 공유하면 그 그룹 대화에 지금 답(선택과 이유)이 메시지 한 건으로 올라간다.
 * 이미 지금 답을 보낸 그룹에는 다시 보내지 않는다
 */
@Service
@RequiredArgsConstructor
public class ShareService {

    private final AnswerRepository answerRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final QuestionService questionService;

    /**
     * 나누기 상태 조회
     */
    @Transactional(readOnly = true)
    public ShareResponseDto getShareStatus(final String questionId, final Long userId) {
        questionService.getVisibleQuestion(questionId);
        final Answer answer = findAnswer(userId, questionId);
        return toResponse(answer, sentGroupIds(answer));
    }

    /**
     * 고른 그룹에 지금 답을 공유 메시지로 보낸다
     * 그룹을 고르지 않으면(나만 보기) 아무 그룹에도 보내지 않는다
     */
    @Transactional
    public ShareResponseDto share(final String questionId, final ShareRequestDto shareRequestDto, final Long userId) {

        // 1) 질문과 내 답 확인
        questionService.getVisibleQuestion(questionId);
        final Answer answer = findAnswer(userId, questionId);

        // 2) 고른 그룹이 모두 가입한 그룹인지 확인
        final Set<Long> requested = new LinkedHashSet<>(shareRequestDto.getGroupIds());
        if (!requested.isEmpty() && new HashSet<>(groupMemberRepository.findJoinedGroupIds(userId, requested)).size() != requested.size()) {
            throw new CustomException(ExceptionCode.GROUP_NOT_JOINED);
        }

        // 3) 아직 지금 답을 보내지 않은 그룹에만 보낸다
        final Set<Long> sent = new LinkedHashSet<>(sentGroupIds(answer));
        for (final Long groupId : requested) {
            if (sent.contains(groupId)) {
                continue;
            }
            final GalaeGroup group = groupRepository.getReferenceById(groupId);
            groupMessageRepository.save(GroupMessage.builder()
                    .group(group)
                    .question(answer.getQuestion())
                    .author(answer.getUser())
                    .choice(answer.getChoice())
                    .text(answer.getReason())
                    .build());
            sent.add(groupId);
        }

        // 4) 보냈으니 초안은 비운다
        answer.clearShareDraft();

        return toResponse(answer, List.copyOf(sent));
    }

    /**
     * 지금 답을 이미 보낸 그룹: 공유 메시지 가운데 지금 답과 선택·이유가 같은 것
     */
    private List<Long> sentGroupIds(final Answer answer) {
        return groupMessageRepository.findMyShares(answer.getUser().getId(), answer.getQuestion().getId()).stream()
                .filter(m -> m.getChoice().getId().equals(answer.getChoice().getId()) && m.getText().equals(answer.getReason()))
                .map(m -> m.getGroup().getId())
                .distinct()
                .toList();
    }

    private ShareResponseDto toResponse(final Answer answer, final List<Long> sent) {
        return new ShareResponseDto(
                answer.getQuestion().getId(),
                sent,
                answer.getHasShareDraft() ? List.copyOf(answer.getShareDraft()) : null
        );
    }

    private Answer findAnswer(final Long userId, final String questionId) {
        return answerRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseThrow(() -> new CustomException(ExceptionCode.ANSWER_REQUIRED));
    }
}
