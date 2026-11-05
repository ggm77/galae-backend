package com.seohamin.galae.domain.achievement.repository;

import com.seohamin.galae.domain.achievement.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    //맺은 순서대로
    List<UserAchievement> findAllByUserIdOrderByAchievedOnAscIdAsc(Long userId);

    //회원 탈퇴
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserAchievement a where a.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
