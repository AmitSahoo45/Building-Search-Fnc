package com.learning.news_search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SearchEventRepository extends ElasticsearchRepository<SearchEvent, String> {
    
    List<SearchEvent> findByRankingVariant(String variant);
    
    List<SearchEvent> findByTimestampBetween(Instant start, Instant end);
    
    List<SearchEvent> findBySessionId(String sessionId);
    
    long countByRankingVariant(String variant);
}
