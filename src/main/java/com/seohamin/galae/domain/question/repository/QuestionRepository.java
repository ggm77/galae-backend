package com.seohamin.galae.domain.question.repository;

import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.entity.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, String> {

    //공개된 질문 (최신순)
    @Query("""
            select q from Question q
            where q.status = :status and q.publishDate <= :today
            order by q.publishDate desc
            """)
    List<Question> findVisible(@Param("status") QuestionStatus status, @Param("today") LocalDate today);

    //공개된 질문 검색 (제목·본문)
    @Query("""
            select q from Question q
            where q.status = :status and q.publishDate <= :today
              and (lower(q.title) like lower(concat('%', :query, '%')) or lower(q.body) like lower(concat('%', :query, '%')))
            order by q.publishDate desc
            """)
    List<Question> searchVisible(
            @Param("status") QuestionStatus status,
            @Param("today") LocalDate today,
            @Param("query") String query
    );

    //오늘 또는 가장 최근에 공개된 질문
    Optional<Question> findFirstByStatusAndPublishDateLessThanEqualOrderByPublishDateDesc(QuestionStatus status, LocalDate today);

    boolean existsByPublishDate(LocalDate publishDate);

    boolean existsByPublishDateAndIdNot(LocalDate publishDate, String id);

    List<Question> findAllByOrderByPublishDateDesc();
}
