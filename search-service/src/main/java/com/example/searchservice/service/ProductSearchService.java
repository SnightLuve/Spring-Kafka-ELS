package com.example.searchservice.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Cacheable(value = "search-results", key = "#request.cacheKey()")
    public List<ProductDocument> search(SearchRequest request) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            boolQueryBuilder.must(Query.of(q -> q
                    .multiMatch(mm -> mm
                            .query(request.getKeyword())
                            .fields("name^3", "description")
                            .fuzziness("AUTO")
                    )
            ));
        }

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t.field("category").value(request.getCategory()))
            ));
        }

        if (request.getBrand() != null && !request.getBrand().isBlank()) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t.field("brand").value(request.getBrand()))
            ));
        }

        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .range(r -> r.number(n -> {
                        n.field("price");
                        if (request.getMinPrice() != null) {
                            n.gte(request.getMinPrice().doubleValue());
                        }
                        if (request.getMaxPrice() != null) {
                            n.lte(request.getMaxPrice().doubleValue());
                        }
                        return n;
                    }))
            ));
        }

        boolQueryBuilder.filter(Query.of(q -> q
                .term(t -> t.field("active").value(true))
        ));

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
                .withPageable(PageRequest.of(request.getPage(), request.getSize()))
                .build();

        SearchHits<ProductDocument> searchHits =
                elasticsearchOperations.search(searchQuery, ProductDocument.class);

        log.info("Search returned {} results", searchHits.getTotalHits());
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }

    public List<ProductDocument> suggest(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q
                        .bool(b -> b
                                .should(
                                        Query.of(sq -> sq.matchPhrasePrefix(mp -> mp
                                                .field("name")
                                                .query(keyword)
                                        )),
                                        Query.of(sq -> sq.matchPhrasePrefix(mp -> mp
                                                .field("description")
                                                .query(keyword)
                                        ))
                                )
                                .filter(Query.of(fq -> fq
                                        .term(t -> t.field("active").value(true))
                                ))
                        )
                ))
                .withPageable(PageRequest.of(0, 5))
                .build();

        SearchHits<ProductDocument> searchHits =
                elasticsearchOperations.search(searchQuery, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
