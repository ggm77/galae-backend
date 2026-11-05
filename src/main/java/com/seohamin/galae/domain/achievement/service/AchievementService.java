package com.seohamin.galae.domain.achievement.service;

import com.seohamin.galae.domain.achievement.dto.AchievementResponseDto;
import com.seohamin.galae.domain.achievement.entity.AchievementType;
import com.seohamin.galae.domain.achievement.entity.UserAchievement;
import com.seohamin.galae.domain.achievement.repository.UserAchievementRepository;
import com.seohamin.galae.domain.answer.repository.AnswerRepository;
import com.seohamin.galae.domain.answer.service.StreakCalculator;
import com.seohamin.galae.domain.comment.repository.CommentRepository;
import com.seohamin.galae.domain.comment.repository.like.CommentLikeRepository;
import com.seohamin.galae.domain.group.repository.GroupRepository;
import com.seohamin.galae.domain.group.repository.message.GroupMessageRepository;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 업적 판정 (docs/ACHIEVEMENTS.md)
 * 답변·나눔·댓글·공감·그룹 만들기 뒤에 evaluate를 불러 새로 맺은 열매에 날짜를 남긴다
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupRepository groupRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final TimeProvider timeProvider;

    /**
     * 유저 활동 통계
     */
    @Transactional(readOnly = true)
    public UserStats getStats(final Long userId) {
        final List<LocalDate> days = answerRepository.findAnsweredDays(userId);
        return new UserStats(
                answerRepository.countByUserId(userId),
                answerRepository.countPastAnswers(userId),
                groupMessageRepository.countSharedQuestions(userId),
                commentRepository.countByAuthorId(userId),
                commentLikeRepository.countByUserId(userId),
                groupRepository.existsByOwnerId(userId),
                StreakCalculator.current(days, timeProvider.today()),
                StreakCalculator.longest(days)
        );
    }

    /**
     * 새로 달성한 업적을 저장하고 돌려준다. 한 번 맺힌 열매는 사라지지 않는다
     */
    @Transactional
    public List<AchievementResponseDto> evaluate(final Long userId) {
        final Set<AchievementType> owned = userAchievementRepository.findAllByUserIdOrderByAchievedOnAscIdAsc(userId).stream()
                .map(UserAchievement::getType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AchievementType.class)));

        final UserStats stats = getStats(userId);
        final LocalDate today = timeProvider.today();

        return Arrays.stream(AchievementType.values())
                .filter(type -> !owned.contains(type) && type.isAchieved(stats))
                .map(type -> userAchievementRepository.save(UserAchievement.builder()
                        .user(userRepository.getReferenceById(userId))
                        .type(type)
                        .achievedOn(today)
                        .build()))
                .map(AchievementResponseDto::new)
                .toList();
    }

    /**
     * 맺은 열매 (맺은 순서대로)
     */
    @Transactional(readOnly = true)
    public List<AchievementResponseDto> getAchievements(final Long userId) {
        return userAchievementRepository.findAllByUserIdOrderByAchievedOnAscIdAsc(userId).stream()
                .map(AchievementResponseDto::new)
                .toList();
    }
}
