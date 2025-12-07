package com.learning.news_search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickEventRepository extends ElasticsearchRepository<ClickEvent, String> {
    
    List<ClickEvent> findByQuery(String query);
    
    List<ClickEvent> findByArticleId(String articleId);
    
    List<ClickEvent> findByRankingVariant(String variant);
    
    List<ClickEvent> findByTimestampBetween(Instant start, Instant end);
    
    long countByRankingVariant(String variant);
    
    long countByQueryAndArticleId(String query, String articleId);
}
