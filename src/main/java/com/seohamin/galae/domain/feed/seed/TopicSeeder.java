package com.seohamin.galae.domain.feed.seed;

import com.seohamin.galae.domain.feed.entity.Topic;
import com.seohamin.galae.domain.feed.entity.TopicSource;
import com.seohamin.galae.domain.feed.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 주제 테이블이 비어 있으면 생각 피드 주제를 넣는다 (seed/paradoxes.json = 프론트 content/paradoxes.json)
 * checked가 날짜면 검증을 마친 주제, null이면 보류
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TopicSeeder implements ApplicationRunner {

    private final TopicRepository topicRepository;
    private final JsonMapper jsonMapper;

    @Value("${galae.seed.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(final ApplicationArguments args) throws IOException {
        if (!enabled || topicRepository.count() > 0) {
            return;
        }

        final JsonNode root;
        try (InputStream in = new ClassPathResource("seed/paradoxes.json").getInputStream()) {
            root = jsonMapper.readTree(in);
        }

        for (final JsonNode node : root) {
            final List<String> cards = new ArrayList<>();
            node.get("cards").forEach(c -> cards.add(c.get("text").asString()));

            final List<TopicSource> sources = new ArrayList<>();
            node.get("sources").forEach(s -> sources.add(new TopicSource(
                    s.get("label").asString(),
                    s.hasNonNull("url") ? s.get("url").asString() : null
            )));

            final JsonNode checked = node.get("checked");
            final LocalDate checkedOn = checked != null && checked.isString() ? LocalDate.parse(checked.asString()) : null;

            topicRepository.save(Topic.builder()
                    .id(node.get("id").asString())
                    .category(node.get("category").asString())
                    .title(node.get("title").asString())
                    .cards(cards)
                    .sources(sources)
                    .checkedOn(checkedOn)
                    .build());
        }

        log.info("생각 피드 주제 {}개를 넣었습니다.", root.size());
    }
}
