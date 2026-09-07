package com.example.searchservice.kafka;

import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.event.ProductSyncEvent;
import com.example.searchservice.repository.ProductSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {

    private final ProductSearchRepository productSearchRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic.product-sync}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleProductSync(String jsonMessage) {
        try {
            ProductSyncEvent event = objectMapper.readValue(jsonMessage, ProductSyncEvent.class);
            log.info("Received product event {} for product {}", event.getActionType(), event.getProductId());

            switch (event.getActionType()) {
                case CREATE, UPDATE -> productSearchRepository.save(toDocument(event));
                case DELETE -> productSearchRepository.deleteById(event.getProductId());
            }
        } catch (Exception e) {
            log.error("Could not handle product sync message: {}", jsonMessage, e);
        }
    }

    private ProductDocument toDocument(ProductSyncEvent event) {
        return ProductDocument.builder()
                .id(event.getProductId())
                .name(event.getName())
                .description(event.getDescription())
                .price(event.getPrice())
                .category(event.getCategory())
                .brand(event.getBrand())
                .active(event.getActive())
                .build();
    }
}
