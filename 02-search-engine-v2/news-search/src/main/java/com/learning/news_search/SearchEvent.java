package com.learning.news_search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;

/**
 * Captures every search event for analysis.
 * This is the foundation for measuring CTR, no-click rate, time-to-click, etc.
 * */

@Document(indexName = "search_events")
public class SearchEvent {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String sessionId;

    @Field(type = FieldType.Text)
    private String query;

    @Field(type = FieldType.Keyword)
    private String categoryFilter; // If user filtered by category

    @Field(type = FieldType.Keyword)
    private List<String> resultIds; // Top results shown to user

    @Field(type = FieldType.Integer)
    private Integer resultCount;

    @Field(type = FieldType.Long)
    private Long responseTimeMs;

    @Field(type = FieldType.Keyword)
    private String rankingVariant; // "A" (old) or "B" (new) for A/B testing

    @Field(type = FieldType.Date)
    private Instant timestamp;

    public SearchEvent() {
        this.timestamp = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SearchEvent event = new SearchEvent();

        public Builder sessionId(String sessionId) {
            event.sessionId = sessionId;
            return this;
        }

        public Builder query(String query) {
            event.query = query;
            return this;
        }

        public Builder categoryFilter(String categoryFilter) {
            event.categoryFilter = categoryFilter;
            return this;
        }

        public Builder resultIds(List<String> resultIds) {
            event.resultIds = resultIds;
            return this;
        }

        public Builder resultCount(Integer resultCount) {
            event.resultCount = resultCount;
            return this;
        }

        public Builder responseTimeMs(Long responseTimeMs) {
            event.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder rankingVariant(String rankingVariant) {
            event.rankingVariant = rankingVariant;
            return this;
        }

        public SearchEvent build() {
            return event;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getCategoryFilter() { return categoryFilter; }
    public void setCategoryFilter(String categoryFilter) { this.categoryFilter = categoryFilter; }

    public List<String> getResultIds() { return resultIds; }
    public void setResultIds(List<String> resultIds) { this.resultIds = resultIds; }

    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }

    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public String getRankingVariant() { return rankingVariant; }
    public void setRankingVariant(String rankingVariant) { this.rankingVariant = rankingVariant; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}