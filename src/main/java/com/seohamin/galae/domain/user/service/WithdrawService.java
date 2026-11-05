package com.seohamin.galae.domain.user.service;

import com.seohamin.galae.domain.achievement.repository.UserAchievementRepository;
import com.seohamin.galae.domain.answer.repository.AnswerRepository;
import com.seohamin.galae.domain.comment.repository.CommentRepository;
import com.seohamin.galae.domain.comment.repository.like.CommentLikeRepository;
import com.seohamin.galae.domain.group.entity.member.GroupMember;
import com.seohamin.galae.domain.group.repository.member.GroupMemberRepository;
import com.seohamin.galae.domain.group.repository.message.GroupMessageRepository;
import com.seohamin.galae.domain.group.service.GroupService;
import com.seohamin.galae.domain.user.dto.WithdrawRequestDto;
import com.seohamin.galae.domain.user.entity.Role;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회원 탈퇴
 * 한 트랜잭션 안에서 유저와 유저가 남긴 데이터를 지운다. 중간에 실패하면 모두 되돌린다.
 *
 * - 그룹: 가입한 모든 그룹에서 '나가기'와 같게 처리 (docs/APP_FLOW.md 5-9)
 *         그 그룹 초안과 내 메시지(공유 포함)가 지워지고, 내가 그룹장인 그룹은 사라진다
 * - 답변·공유 초안·업적: 삭제 (참여 수·비율·익명 의견에서도 빠진다)
 * - 댓글: 내 댓글·답글과 거기 달린 공감 삭제. 다른 사람이 내 댓글에 단 답글은 원 댓글로 올려 남긴다
 * - 공감: 내가 누른 공감 삭제, 그 댓글의 공감 수도 줄인다
 * - 관리자 계정은 탈퇴할 수 없다 (관리자 페이지 접근을 잃지 않도록)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupService groupService;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserAchievementRepository userAchievementRepository;

    @Transactional
    public void withdraw(final WithdrawRequestDto withdrawRequestDto, final Long userId) {

        // 1) 본인 확인
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
        if (!passwordEncoder.matches(withdrawRequestDto.getPassword(), user.getPasswordHash())) {
            throw new CustomException(ExceptionCode.LOGIN_FAILED, "비밀번호가 올바르지 않습니다.");
        }
        if (user.getRole() == Role.ADMIN) {
            throw new CustomException(ExceptionCode.ADMIN_CANNOT_WITHDRAW);
        }

        // 2) 그룹: 모두 나가기 (그룹장인 그룹은 삭제)
        final List<Long> groupIds = groupMemberRepository.findAllWithGroupByUserId(userId).stream()
                .map(GroupMember::getGroup)
                .map(g -> g.getId())
                .toList();
        groupIds.forEach(groupId -> groupService.leaveGroup(groupId, userId));
        groupMessageRepository.deleteAllByAuthorId(userId);

        // 3) 답변과 공유 초안
        answerRepository.deleteShareDraftsByUserId(userId);
        answerRepository.deleteAllByUserId(userId);

        // 4) 공감: 내가 누른 공감 수를 줄이고 기록 삭제
        commentRepository.decreaseLikeCountLikedBy(userId);
        commentLikeRepository.deleteAllByUserId(userId);

        // 5) 댓글: 남의 답글은 남기고, 내 댓글에 달린 공감 → 내 답글 → 내 원 댓글 순으로 삭제
        final List<Long> myCommentIds = commentRepository.findIdsByAuthorId(userId);
        if (!myCommentIds.isEmpty()) {
            commentRepository.detachRepliesOf(myCommentIds, userId);
            commentLikeRepository.deleteAllByCommentIds(myCommentIds);
            commentRepository.deleteRepliesByAuthorId(userId);
            commentRepository.deleteAllByAuthorId(userId);
        }

        // 6) 업적과 유저
        userAchievementRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);

        log.info("회원 탈퇴: userId={}, 나간 그룹 {}개, 지운 댓글 {}개", userId, groupIds.size(), myCommentIds.size());
    }
}
