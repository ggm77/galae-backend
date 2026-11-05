package com.seohamin.galae.domain.group.service;

import com.seohamin.galae.domain.achievement.service.AchievementService;
import com.seohamin.galae.domain.block.service.BlockService;
import com.seohamin.galae.domain.answer.repository.AnswerRepository;
import com.seohamin.galae.domain.group.dto.GroupJoinRequestDto;
import com.seohamin.galae.domain.group.dto.GroupRequestDto;
import com.seohamin.galae.domain.group.dto.GroupResponseDto;
import com.seohamin.galae.domain.group.dto.GroupSummaryResponseDto;
import com.seohamin.galae.domain.group.entity.GalaeGroup;
import com.seohamin.galae.domain.group.entity.member.GroupMember;
import com.seohamin.galae.domain.group.repository.GroupRepository;
import com.seohamin.galae.domain.group.repository.member.GroupMemberRepository;
import com.seohamin.galae.domain.group.repository.message.GroupMessageRepository;
import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.service.QuestionService;
import com.seohamin.galae.domain.user.dto.UserSimpleResponseDto;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import com.seohamin.galae.global.util.PhotoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 그룹 만들기·참여·설정·나가기·내보내기
 */
@Service
@RequiredArgsConstructor
public class GroupService {

    //헷갈리는 글자(0, O, 1, I)는 뺀다
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;
    private final AchievementService achievementService;
    private final BlockService blockService;

    /**
     * 가입한 그룹 목록 (가입순)
     */
    @Transactional(readOnly = true)
    public List<GroupSummaryResponseDto> getMyGroups(final Long userId) {

        // 1) 가입한 그룹
        final List<GalaeGroup> groups = groupMemberRepository.findAllWithGroupByUserId(userId).stream()
                .map(GroupMember::getGroup)
                .toList();
        if (groups.isEmpty()) {
            return List.of();
        }
        final List<Long> groupIds = groups.stream().map(GalaeGroup::getId).toList();

        // 2) 멤버 수
        final Map<Long, Long> memberCounts = new HashMap<>();
        for (final Object[] row : groupMemberRepository.countByGroupIds(groupIds)) {
            memberCounts.put((Long) row[0], (Long) row[1]);
        }

        // 3) 오늘의 질문을 나눈 멤버 (내가 차단한 사람 제외)
        final Map<Long, List<UserSimpleResponseDto>> sharedMembers = todaySharedMembers(groupIds, blockService.blockedIds(userId));

        return groups.stream()
                .map(g -> new GroupSummaryResponseDto(
                        g,
                        memberCounts.getOrDefault(g.getId(), 0L),
                        g.isOwnedBy(userId),
                        sharedMembers.getOrDefault(g.getId(), List.of())
                ))
                .toList();
    }

    /**
     * 그룹 상세 (가입한 사람만)
     */
    @Transactional(readOnly = true)
    public GroupResponseDto getGroup(final Long groupId, final Long userId) {
        final GalaeGroup group = getJoinedGroup(groupId, userId);
        return new GroupResponseDto(group, groupMemberRepository.findAllWithUserByGroupId(groupId), userId, blockService.blockedIds(userId));
    }

    /**
     * 그룹 만들기. 만든 사람이 그룹장이자 첫 멤버가 된다
     */
    @Transactional
    public GroupResponseDto createGroup(final GroupRequestDto groupRequestDto, final Long userId) {

        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        final GalaeGroup group = GalaeGroup.builder()
                .name(groupRequestDto.getName().trim())
                .description(Optional.ofNullable(groupRequestDto.getDescription()).map(String::trim).orElse(""))
                .color(groupRequestDto.getColor())
                .inviteCode(newInviteCode())
                .owner(user)
                .build();
        if (groupRequestDto.getPhoto() != null) {
            group.updatePhoto(PhotoUtil.decode(groupRequestDto.getPhoto()));
        }
        groupRepository.save(group);

        groupMemberRepository.save(GroupMember.builder().group(group).user(user).build());

        //개척자 업적
        achievementService.evaluate(userId);

        return new GroupResponseDto(group, groupMemberRepository.findAllWithUserByGroupId(group.getId()), userId, blockService.blockedIds(userId));
    }

    /**
     * 초대 코드로 참여
     */
    @Transactional
    public GroupResponseDto joinGroup(final GroupJoinRequestDto groupJoinRequestDto, final Long userId) {

        // 1) 코드 확인
        final String code = groupJoinRequestDto.getCode().trim().toUpperCase(Locale.ROOT);
        final GalaeGroup group = groupRepository.findByInviteCode(code)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_CODE_INVALID));

        // 2) 중복 가입 확인
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
            throw new CustomException(ExceptionCode.GROUP_ALREADY_JOINED);
        }

        // 3) 가입. 가입만으로 기존 답변이 공유되지는 않는다 (docs/APP_FLOW.md 5-6)
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
        groupMemberRepository.save(GroupMember.builder().group(group).user(user).build());

        return new GroupResponseDto(group, groupMemberRepository.findAllWithUserByGroupId(group.getId()), userId, blockService.blockedIds(userId));
    }

    /**
     * 그룹 설정 (그룹장만): 색, 이름, 소개, 사진
     */
    @Transactional
    public GroupResponseDto updateGroup(final Long groupId, final GroupRequestDto groupRequestDto, final Long userId) {

        final GalaeGroup group = getOwnedGroup(groupId, userId);

        Optional.ofNullable(groupRequestDto.getName()).map(String::trim).filter(s -> !s.isEmpty()).ifPresent(group::updateName);
        Optional.ofNullable(groupRequestDto.getDescription()).map(String::trim).ifPresent(group::updateDescription);
        Optional.ofNullable(groupRequestDto.getColor()).ifPresent(group::updateColor);
        if (groupRequestDto.getPhoto() != null) {
            group.updatePhoto(PhotoUtil.decode(groupRequestDto.getPhoto()));
        }

        return new GroupResponseDto(group, groupMemberRepository.findAllWithUserByGroupId(groupId), userId, blockService.blockedIds(userId));
    }

    /**
     * 그룹 나가기
     * 그 그룹 초안과 그 그룹에 보낸 메시지(공유 포함)가 지워지고 개인 기록은 남는다.
     * 그룹장이 나가면 그룹이 사라진다 (docs/APP_FLOW.md 5-9)
     */
    @Transactional
    public void leaveGroup(final Long groupId, final Long userId) {
        final GalaeGroup group = getJoinedGroup(groupId, userId);

        if (group.isOwnedBy(userId)) {
            deleteGroup(groupId);
            return;
        }

        removeMember(groupId, userId);
    }

    /**
     * 멤버 내보내기 (그룹장만)
     * 내보낸 사람의 메시지와 초안도 나갈 때와 똑같이 지운다
     */
    @Transactional
    public void kickMember(final Long groupId, final Long memberUserId, final Long userId) {
        getOwnedGroup(groupId, userId);

        if (memberUserId.equals(userId)) {
            throw new CustomException(ExceptionCode.CANNOT_KICK_SELF);
        }
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, memberUserId)) {
            throw new CustomException(ExceptionCode.MEMBER_NOT_EXIST);
        }

        removeMember(groupId, memberUserId);
    }

    /**
     * 그룹 사진 (img 태그로 불러오므로 인증 없이)
     */
    @Transactional(readOnly = true)
    public byte[] getPhoto(final Long groupId) {
        final GalaeGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_EXIST));
        if (group.getPhoto() == null) {
            throw new CustomException(ExceptionCode.PHOTO_NOT_EXIST);
        }
        return group.getPhoto();
    }

    /**
     * 가입한 그룹을 찾는 메서드. 메시지·나누기에서도 쓴다
     */
    public GalaeGroup getJoinedGroup(final Long groupId, final Long userId) {
        final GalaeGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_EXIST));
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_JOINED);
        }
        return group;
    }

    private GalaeGroup getOwnedGroup(final Long groupId, final Long userId) {
        final GalaeGroup group = getJoinedGroup(groupId, userId);
        if (!group.isOwnedBy(userId)) {
            throw new CustomException(ExceptionCode.PERMISSION_DENIED);
        }
        return group;
    }

    private void removeMember(final Long groupId, final Long memberUserId) {
        answerRepository.removeGroupFromUserShareDrafts(groupId, memberUserId);
        groupMessageRepository.deleteAllByGroupIdAndAuthorId(groupId, memberUserId);
        groupMemberRepository.findByGroupIdAndUserId(groupId, memberUserId).ifPresent(groupMemberRepository::delete);
    }

    private void deleteGroup(final Long groupId) {
        answerRepository.removeGroupFromAllShareDrafts(groupId);
        groupMessageRepository.deleteAllByGroupId(groupId);
        groupMemberRepository.deleteAllByGroupId(groupId);
        groupRepository.deleteById(groupId);
    }

    private Map<Long, List<UserSimpleResponseDto>> todaySharedMembers(final List<Long> groupIds, final Set<Long> blockedIds) {
        final Optional<Question> today = questionService.findTodayQuestion();
        if (today.isEmpty()) {
            return Map.of();
        }

        final List<Object[]> rows = groupMessageRepository.findSharedAuthors(groupIds, today.get().getId());
        final Set<Long> authorIds = rows.stream().map(r -> (Long) r[1]).filter(id -> !blockedIds.contains(id)).collect(Collectors.toSet());
        final Map<Long, User> authors = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        final Map<Long, List<UserSimpleResponseDto>> result = new HashMap<>();
        for (final Object[] row : rows) {
            final User author = authors.get((Long) row[1]);
            if (author != null) {
                result.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add(new UserSimpleResponseDto(author));
            }
        }
        return result;
    }

    private String newInviteCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            final StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            final String code = sb.toString();
            if (!groupRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR, "초대 코드를 만들지 못했습니다.");
    }
}
