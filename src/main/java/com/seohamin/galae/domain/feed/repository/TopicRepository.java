package com.seohamin.galae.domain.feed.repository;

import com.seohamin.galae.domain.feed.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, String> {

    List<Topic> findAllByOrderByIdAsc();

    List<Topic> findAllByCheckedOnIsNotNullOrderByIdAsc();
}
