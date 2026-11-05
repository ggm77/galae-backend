package com.seohamin.galae.domain.group.entity.member;

import com.seohamin.galae.domain.group.entity.GalaeGroup;
import com.seohamin.galae.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 그룹 가입 정보 (그룹장도 멤버로 들어간다)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "group_member",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_member", columnNames = {"group_id", "user_id"}),
        indexes = @Index(name = "idx_group_member_user", columnList = "user_id")
)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GalaeGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Builder
    public GroupMember(final GalaeGroup group, final User user) {
        this.group = group;
        this.user = user;
    }
}
