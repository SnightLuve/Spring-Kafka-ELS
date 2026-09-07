package com.example.searchservice.config;

import com.example.searchservice.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
            if (!indexOps.exists()) {
                indexOps.createWithMapping();
                log.info("Created Elasticsearch index products");
            } else {
                indexOps.putMapping();
                log.info("Updated Elasticsearch mapping for products");
            }
        } catch (Exception e) {
            log.warn("Could not initialize products index. Elasticsearch may not be ready: {}", e.getMessage());
        }
    }
}
