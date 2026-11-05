package com.seohamin.galae.domain.answer.repository;

import com.seohamin.galae.domain.answer.entity.Answer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    Optional<Answer> findByUserIdAndQuestionId(Long userId, String questionId);

    boolean existsByUserIdAndQuestionId(Long userId, String questionId);

    List<Answer> findAllByUserIdAndQuestionIdIn(Long userId, Collection<String> questionIds);

    List<Answer> findAllByUserId(Long userId);

    //달력: 기간 안에 답한 답변
    @Query("""
            select a from Answer a join fetch a.question join fetch a.choice
            where a.user.id = :userId and a.answeredOn between :from and :to
            order by a.answeredOn desc, a.createdAt desc
            """)
    List<Answer> findAllByUserIdAndAnsweredOnBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    long countByQuestionId(String questionId);

    //질문별 참여 수 [questionId, count]
    @Query("""
            select a.question.id, count(a) from Answer a
            where a.question.id in :questionIds
            group by a.question.id
            """)
    List<Object[]> countByQuestionIds(@Param("questionIds") Collection<String> questionIds);

    //선택지별 응답 수 [choiceIndex, count]
    @Query("""
            select a.choice.choiceIndex, count(a) from Answer a
            where a.question.id = :questionId
            group by a.choice.choiceIndex
            """)
    List<Object[]> countByChoice(@Param("questionId") String questionId);

    //선택지 코멘트: 다른 사람이 남긴 이유 (익명, 최신순)
    @Query("""
            select a from Answer a
            where a.question.id = :questionId and a.choice.choiceIndex = :choiceIndex
              and a.reason <> '' and a.user.id <> :userId
            order by a.updatedAt desc, a.id desc
            """)
    Slice<Answer> findOpinions(
            @Param("questionId") String questionId,
            @Param("choiceIndex") Integer choiceIndex,
            @Param("userId") Long userId,
            Pageable pageable
    );

    //업적·통계용
    long countByUserId(Long userId);

    @Query("select distinct a.answeredOn from Answer a where a.user.id = :userId")
    List<LocalDate> findAnsweredDays(@Param("userId") Long userId);

    //지난 질문에 답한 수 (답한 날이 질문 날과 다름)
    @Query("select count(a) from Answer a where a.user.id = :userId and a.answeredOn <> a.question.publishDate")
    long countPastAnswers(@Param("userId") Long userId);
}
