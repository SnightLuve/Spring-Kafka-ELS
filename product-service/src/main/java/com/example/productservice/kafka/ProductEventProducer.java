package com.example.productservice.kafka;

import com.example.productservice.entity.Product;
import com.example.productservice.event.ProductSyncEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.product-sync}")
    private String productSyncTopic;

    public void sendCreateEvent(Product product) {
        sendMessage(product.getId(), fromProduct(ProductSyncEvent.ActionType.CREATE, product));
    }

    public void sendUpdateEvent(Product product) {
        sendMessage(product.getId(), fromProduct(ProductSyncEvent.ActionType.UPDATE, product));
    }

    public void sendDeleteEvent(Long productId) {
        ProductSyncEvent event = ProductSyncEvent.builder()
                .actionType(ProductSyncEvent.ActionType.DELETE)
                .productId(productId)
                .build();
        sendMessage(productId, event);
    }

    private ProductSyncEvent fromProduct(ProductSyncEvent.ActionType actionType, Product product) {
        return ProductSyncEvent.builder()
                .actionType(actionType)
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .brand(product.getBrand())
                .active(product.getActive())
                .build();
    }

    private void sendMessage(Long productId, ProductSyncEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(productSyncTopic, String.valueOf(productId), payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Published product event {} for product {}", event.getActionType(), productId);
                        } else {
                            log.error("Could not publish product event for product {}", productId, ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Could not serialize product event for product {}", productId, e);
        }
    }
}
