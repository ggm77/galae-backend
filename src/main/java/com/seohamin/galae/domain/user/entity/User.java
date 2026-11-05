package com.seohamin.galae.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 유저 정보를 저장하는 엔티티
 * 아이디(handle)·비밀번호로 가입한다 (자체 로그인)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_handle", columnNames = "handle")
)
public class User {

    //PK 유저 고유 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //아이디 (영문 소문자·숫자·마침표·밑줄, 20자, 중복 불가)
    @Column(length = 20, nullable = false)
    private String handle;

    //BCrypt 해시
    @Column(length = 100, nullable = false)
    private String passwordHash;

    //이름 (10자)
    @Column(length = 10, nullable = false)
    private String name;

    //소개 (80자)
    @Column(length = 80, nullable = false)
    private String bio;

    //프로필 색 (sage, blue, peach, lilac)
    @Column(length = 10, nullable = false)
    private String color;

    //프로필 사진 JPEG (프론트에서 256px로 줄여 보냄)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = true)
    private byte[] photo;

    //사진이 바뀐 시각. 사진 URL의 캐시 무효화용
    @Column(nullable = true)
    private LocalDateTime photoUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role;

    //가입한 날 (서비스 기준 날짜). 달력에서 누를 수 있는 첫 날
    @Column(nullable = false)
    private LocalDate joinedOn;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(
            final String handle,
            final String passwordHash,
            final String name,
            final Role role,
            final LocalDate joinedOn
    ) {
        this.handle = handle;
        this.passwordHash = passwordHash;
        this.name = name;
        this.bio = "";
        this.color = "blue";
        this.role = role;
        this.joinedOn = joinedOn;
    }

    public boolean hasPhoto() {
        return photoUpdatedAt != null;
    }

    public void updateHandle(final String handle) {
        this.handle = handle;
    }

    public void updateName(final String name) {
        this.name = name;
    }

    public void updateBio(final String bio) {
        this.bio = bio;
    }

    public void updateColor(final String color) {
        this.color = color;
    }

    //null이면 사진 삭제
    public void updatePhoto(final byte[] photo) {
        this.photo = photo;
        this.photoUpdatedAt = photo == null ? null : LocalDateTime.now();
    }

    public void updatePasswordHash(final String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateRole(final Role role) {
        this.role = role;
    }
}
