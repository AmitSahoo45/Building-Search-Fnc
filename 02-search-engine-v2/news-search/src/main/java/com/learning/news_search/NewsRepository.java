package com.learning.news_search;

import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

@Repository
public interface NewsRepository extends ElasticsearchRepository<NewsArticle, String> {
    @Query("""
    {
      "multi_match": {
        "query": "?0",
        "fields": [ "headline^3", "shortDescription" ]
      }
    }
    """)
    Page<NewsArticle> searchByKeyword(String keyword, Pageable pageable);
}
