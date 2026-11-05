package com.seohamin.galae.domain.group.service.message;

import com.seohamin.galae.domain.answer.repository.AnswerRepository;
import com.seohamin.galae.domain.group.dto.message.GroupMessageListResponseDto;
import com.seohamin.galae.domain.group.dto.message.GroupMessageRequestDto;
import com.seohamin.galae.domain.group.entity.GalaeGroup;
import com.seohamin.galae.domain.group.entity.message.GroupMessage;
import com.seohamin.galae.domain.group.repository.message.GroupMessageRepository;
import com.seohamin.galae.domain.group.service.GroupService;
import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.service.QuestionService;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 그룹 대화 (질문별)
 */
@Service
@RequiredArgsConstructor
public class GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final QuestionService questionService;

    /**
     * 질문별 그룹 대화 조회 (증분 폴링)
     * after가 없으면 전체, 있으면 그 메시지 ID보다 새로운 것만 돌려준다
     * 내가 이 질문에 답하기 전에는 다른 사람의 선택과 글을 숨긴다
     */
    @Transactional(readOnly = true)
    public GroupMessageListResponseDto getMessages(
            final Long groupId,
            final String questionId,
            final Long after,
            final Long userId
    ) {
        if (after != null && after < 0) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST, "after는 0 이상이어야 합니다.");
        }
        final long afterId = after == null ? 0L : after;

        // 1) 가입한 그룹과 공개된 질문인지 확인 (나가거나 내보내지면 폴링도 막힌다)
        groupService.getJoinedGroup(groupId, userId);
        questionService.getVisibleQuestion(questionId);

        // 2) 새 메시지
        final boolean locked = !answerRepository.existsByUserIdAndQuestionId(userId, questionId);
        final List<GroupMessage> messages = groupMessageRepository.findAllByGroupIdAndQuestionIdAfter(groupId, questionId, afterId);
        final long lastMessageId = messages.isEmpty() ? afterId : messages.getLast().getId();

        return new GroupMessageListResponseDto(
                groupId,
                questionId,
                locked,
                groupMessageRepository.countSharedMembers(groupId, questionId),
                messages.stream().map(m -> new GroupMessageListResponseDto.Item(m, userId, locked)).toList(),
                lastMessageId
        );
    }

    /**
     * 일반 메시지 보내기
     */
    @Transactional
    public GroupMessageListResponseDto.Item sendMessage(
            final Long groupId,
            final String questionId,
            final GroupMessageRequestDto groupMessageRequestDto,
            final Long userId
    ) {
        final GalaeGroup group = groupService.getJoinedGroup(groupId, userId);
        final Question question = questionService.getVisibleQuestion(questionId);
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        final String text = groupMessageRequestDto.getText().trim();
        if (text.isEmpty()) {
            throw new CustomException(ExceptionCode.EMPTY_MESSAGE);
        }

        final GroupMessage message = groupMessageRepository.save(GroupMessage.builder()
                .group(group)
                .question(question)
                .author(user)
                .text(text)
                .build());

        return new GroupMessageListResponseDto.Item(message, userId, false);
    }
}
