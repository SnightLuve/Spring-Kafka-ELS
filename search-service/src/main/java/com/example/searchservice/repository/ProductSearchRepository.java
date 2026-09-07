package com.example.searchservice.repository;

import com.example.searchservice.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    List<ProductDocument> findByName(String name);

    List<ProductDocument> findByCategory(String category);

    List<ProductDocument> findByBrandAndActiveTrue(String brand);
}
