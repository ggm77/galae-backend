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
     * 질문별 그룹 대화 조회
     * 내가 이 질문에 답하기 전에는 다른 사람의 선택과 글을 숨긴다
     */
    @Transactional(readOnly = true)
    public GroupMessageListResponseDto getMessages(final Long groupId, final String questionId, final Long userId) {
        groupService.getJoinedGroup(groupId, userId);
        questionService.getVisibleQuestion(questionId);

        final boolean locked = !answerRepository.existsByUserIdAndQuestionId(userId, questionId);
        final List<GroupMessage> messages = groupMessageRepository.findAllByGroupIdAndQuestionId(groupId, questionId);
        final long sharedMemberCount = messages.stream()
                .filter(GroupMessage::isShare)
                .map(m -> m.getAuthor().getId())
                .distinct()
                .count();

        return new GroupMessageListResponseDto(
                groupId,
                questionId,
                locked,
                sharedMemberCount,
                messages.stream().map(m -> new GroupMessageListResponseDto.Item(m, userId, locked)).toList()
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
