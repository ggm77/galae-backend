package com.seohamin.galae.domain.question.repository.image;

import com.seohamin.galae.domain.question.entity.image.QuestionImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionImageRepository extends JpaRepository<QuestionImage, String> {
}
