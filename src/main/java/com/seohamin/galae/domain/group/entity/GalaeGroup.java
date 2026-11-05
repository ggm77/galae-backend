package com.seohamin.galae.domain.group.entity;

import com.seohamin.galae.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 교회 청년부·작은 모임 같은 그룹
 * group은 SQL 예약어라서 테이블 이름을 galae_group으로 둔다
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "galae_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_invite_code", columnNames = "invite_code")
)
public class GalaeGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //이름 (20자)
    @Column(length = 20, nullable = false)
    private String name;

    //소개 (100자)
    @Column(length = 100, nullable = false)
    private String description;

    //그룹 색 (sage, blue, peach, lilac)
    @Column(length = 10, nullable = false)
    private String color;

    //그룹 사진 JPEG
    //길이를 주지 않으면 MariaDB에서 tinyblob(255바이트)이 되므로 지정한다 → mediumblob
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = true, length = 1_000_000)
    private byte[] photo;

    @Column(nullable = true)
    private LocalDateTime photoUpdatedAt;

    //6자리 초대 코드
    @Column(name = "invite_code", length = 6, nullable = false)
    private String inviteCode;

    //그룹장. 그룹장이 나가면 그룹이 사라진다
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public GalaeGroup(
            final String name,
            final String description,
            final String color,
            final String inviteCode,
            final User owner
    ) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.inviteCode = inviteCode;
        this.owner = owner;
    }

    public boolean isOwnedBy(final Long userId) {
        return owner.getId().equals(userId);
    }

    public boolean hasPhoto() {
        return photoUpdatedAt != null;
    }

    public void updateName(final String name) {
        this.name = name;
    }

    public void updateDescription(final String description) {
        this.description = description;
    }

    public void updateColor(final String color) {
        this.color = color;
    }

    //null이면 사진 삭제
    public void updatePhoto(final byte[] photo) {
        this.photo = photo;
        this.photoUpdatedAt = photo == null ? null : LocalDateTime.now();
    }
}
