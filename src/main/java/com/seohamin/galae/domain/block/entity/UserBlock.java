package com.seohamin.galae.domain.block.entity;

import com.seohamin.galae.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 차단 (한쪽 방향)
 * blocker의 화면에서 blocked가 쓴 댓글·그룹 메시지·익명 의견을 걸러 낸다
 * blocked에게는 차단 사실을 알리지 않는다
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_block",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_block", columnNames = {"blocker_id", "blocked_id"}),
        indexes = @Index(name = "idx_user_block_blocked", columnList = "blocked_id")
)
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //차단한 사람
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    //차단당한 사람
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserBlock(final User blocker, final User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }
}
