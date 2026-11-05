package com.seohamin.galae.domain.question.seed;

import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.entity.QuestionStatus;
import com.seohamin.galae.domain.question.entity.choice.Choice;
import com.seohamin.galae.domain.question.entity.perspective.Perspective;
import com.seohamin.galae.domain.question.entity.perspective.PerspectiveStatus;
import com.seohamin.galae.domain.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * 질문 테이블이 비어 있으면 기본 질문을 넣는다 (seed/questions.json)
 * 프론트 데모(src/questionData.js)의 질문·선택지·성경 해설이다.
 * 예시 참여 수·비율·의견은 실제 데이터가 아니라서 넣지 않는다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionSeeder implements ApplicationRunner {

    private final QuestionRepository questionRepository;
    private final JsonMapper jsonMapper;

    @Value("${galae.seed.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(final ApplicationArguments args) throws IOException {
        if (!enabled || questionRepository.count() > 0) {
            return;
        }

        final List<SeedQuestion> seeds;
        try (InputStream in = new ClassPathResource("seed/questions.json").getInputStream()) {
            seeds = jsonMapper.readValue(in, new TypeReference<>() {});
        }

        for (final SeedQuestion seed : seeds) {
            final Question question = Question.builder()
                    .id(seed.id())
                    .publishDate(LocalDate.parse(seed.date()))
                    .title(seed.title())
                    .body(seed.body())
                    .status(QuestionStatus.PUBLISHED)
                    .build();

            seed.choices().forEach(c -> question.addChoice(Choice.builder()
                    .choiceIndex(c.id())
                    .text(c.text())
                    .shortText(c.shortText())
                    .build()));

            for (int i = 0; i < seed.perspectives().size(); i++) {
                final SeedPerspective p = seed.perspectives().get(i);
                question.addPerspective(Perspective.builder()
                        .label(p.label())
                        .body(p.body())
                        .verse(p.verse())
                        .ref(p.ref())
                        .sortOrder(i)
                        .status(PerspectiveStatus.APPROVED)
                        .build());
            }

            questionRepository.save(question);
        }

        log.info("기본 질문 {}개를 넣었습니다.", seeds.size());
    }

    record SeedQuestion(String id, String date, String title, String body, List<SeedChoice> choices, List<SeedPerspective> perspectives) {}

    record SeedChoice(Integer id, String text, String shortText) {}

    record SeedPerspective(String label, String body, String verse, String ref) {}
}
