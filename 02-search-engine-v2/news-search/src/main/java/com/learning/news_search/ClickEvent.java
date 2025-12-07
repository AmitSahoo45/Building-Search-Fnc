package com.learning.news_search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;


/**
 * Captures click events - when a user clicks on a search result.
 * This is crucial for CTR calculation and training the ranking model.
 */
@Document(indexName = "click_events")
public class ClickEvent {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String sessionId;

    @Field(type = FieldType.Text)
    private String query; // The query that led to this click

    @Field(type = FieldType.Keyword)
    private String articleId; // Which article was clicked

    @Field(type = FieldType.Integer)
    private Integer position; // Rank position in results (1-indexed)

    @Field(type = FieldType.Keyword)
    private String rankingVariant; // "A" or "B" - which ranking was used

    @Field(type = FieldType.Long)
    private Long timeToClickMs; // Time from search to click (engagement metric)

    @Field(type = FieldType.Date)
    private Instant timestamp;

    public ClickEvent() {
        this.timestamp = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ClickEvent event = new ClickEvent();

        public Builder sessionId(String sessionId) {
            event.sessionId = sessionId;
            return this;
        }

        public Builder query(String query) {
            event.query = query;
            return this;
        }

        public Builder articleId(String articleId) {
            event.articleId = articleId;
            return this;
        }

        public Builder position(Integer position) {
            event.position = position;
            return this;
        }

        public Builder rankingVariant(String rankingVariant) {
            event.rankingVariant = rankingVariant;
            return this;
        }

        public Builder timeToClickMs(Long timeToClickMs) {
            event.timeToClickMs = timeToClickMs;
            return this;
        }

        public ClickEvent build() {
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

    public String getArticleId() { return articleId; }
    public void setArticleId(String articleId) { this.articleId = articleId; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public String getRankingVariant() { return rankingVariant; }
    public void setRankingVariant(String rankingVariant) { this.rankingVariant = rankingVariant; }

    public Long getTimeToClickMs() { return timeToClickMs; }
    public void setTimeToClickMs(Long timeToClickMs) { this.timeToClickMs = timeToClickMs; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}