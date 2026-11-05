package com.seohamin.galae.domain.achievement.entity;

import com.seohamin.galae.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 유저가 맺은 열매(업적)
 * 한 번 맺힌 열매는 공감을 취소해도 사라지지 않는다
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_achievement",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_achievement", columnNames = {"user_id", "type"})
)
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AchievementType type;

    //처음 달성한 날 (서비스 기준 날짜)
    @Column(nullable = false)
    private LocalDate achievedOn;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserAchievement(final User user, final AchievementType type, final LocalDate achievedOn) {
        this.user = user;
        this.type = type;
        this.achievedOn = achievedOn;
    }
}
