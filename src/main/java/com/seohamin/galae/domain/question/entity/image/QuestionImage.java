package com.seohamin.galae.domain.question.entity.image;

import com.seohamin.galae.domain.question.entity.Question;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 질문 이미지 (질문당 하나)
 * 질문 목록을 불러올 때 이미지 바이트까지 읽지 않도록 question 테이블과 나눠 둔다
 * PK가 질문 ID와 같다 (1:1)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "question_image")
public class QuestionImage {

    @Id
    @Column(name = "question_id", length = 20)
    private String questionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    //길이를 주지 않으면 MariaDB에서 tinyblob(255바이트)이 되므로 지정한다 → mediumblob
    @Lob
    @Column(nullable = false, length = 2_000_000)
    private byte[] data;

    //image/jpeg, image/png, image/webp
    @Column(length = 20, nullable = false)
    private String contentType;

    public QuestionImage(final Question question, final byte[] data, final String contentType) {
        this.question = question;
        this.data = data;
        this.contentType = contentType;
    }

    public void update(final byte[] data, final String contentType) {
        this.data = data;
        this.contentType = contentType;
    }
}
