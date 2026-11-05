package com.seohamin.galae.domain.question.repository.perspective;

import com.seohamin.galae.domain.question.entity.perspective.Perspective;
import com.seohamin.galae.domain.question.entity.perspective.PerspectiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerspectiveRepository extends JpaRepository<Perspective, Long> {

    List<Perspective> findAllByQuestionIdAndStatusOrderBySortOrderAscIdAsc(String questionId, PerspectiveStatus status);

    //검수 큐
    List<Perspective> findAllByStatusOrderByCreatedAtAsc(PerspectiveStatus status);
}
