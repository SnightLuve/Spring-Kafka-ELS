package com.example.searchservice.controller;

import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.dto.SearchRequest;
import com.example.searchservice.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService searchService;

    @GetMapping
    public ResponseEntity<List<ProductDocument>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        SearchRequest request = SearchRequest.builder()
                .keyword(keyword)
                .category(category)
                .brand(brand)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(searchService.search(request));
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<ProductDocument>> suggest(@RequestParam String q) {
        return ResponseEntity.ok(searchService.suggest(q));
    }
}
