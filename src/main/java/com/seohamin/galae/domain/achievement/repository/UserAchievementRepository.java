package com.seohamin.galae.domain.achievement.repository;

import com.seohamin.galae.domain.achievement.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    //맺은 순서대로
    List<UserAchievement> findAllByUserIdOrderByAchievedOnAscIdAsc(Long userId);
}
