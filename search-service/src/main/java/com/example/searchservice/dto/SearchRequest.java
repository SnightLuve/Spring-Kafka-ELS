package com.example.searchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    private String keyword;
    private String category;
    private String brand;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;

    public String cacheKey() {
        return String.join("|",
                value(keyword),
                value(category),
                value(brand),
                value(minPrice),
                value(maxPrice),
                String.valueOf(page),
                String.valueOf(size));
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
