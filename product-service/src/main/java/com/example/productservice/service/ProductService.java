package com.example.productservice.service;

import com.example.productservice.dto.ProductDTO;
import com.example.productservice.entity.Product;
import com.example.productservice.kafka.ProductEventProducer;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEventProducer eventProducer;

    @Transactional
    public Product createProduct(ProductDTO dto) {
        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .category(dto.getCategory())
                .brand(dto.getBrand())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        afterCommit(() -> eventProducer.sendCreateEvent(savedProduct));
        return savedProduct;
    }

    @Transactional
    public Product updateProduct(Long id, ProductDTO dto) {
        Product product = getProductById(id);
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setCategory(dto.getCategory());
        product.setBrand(dto.getBrand());

        Product updatedProduct = productRepository.save(product);
        afterCommit(() -> eventProducer.sendUpdateEvent(updatedProduct));
        return updatedProduct;
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        afterCommit(() -> eventProducer.sendDeleteEvent(id));
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public void reindexAll() {
        List<Product> products = productRepository.findAll();
        products.forEach(eventProducer::sendCreateEvent);
        log.info("Published {} product reindex events", products.size());
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
