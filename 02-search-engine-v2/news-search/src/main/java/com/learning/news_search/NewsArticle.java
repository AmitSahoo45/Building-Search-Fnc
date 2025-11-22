package com.learning.news_search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

@Document(indexName = "news") // Note: By doing this, it creates an index called "news" in Elasticsearch
public class NewsArticle {
    @Id
    private String id;

    @Field(type = FieldType.Keyword) // Optimized for exact filtering (like "BUSINESS" or "SPORTS")
    private String category;

    @Field(type = FieldType.Text)
    private String headline;

    @Field(type = FieldType.Text)
    private String authors;

    @Field(type = FieldType.Keyword)
    private String link;

    @Field(type = FieldType.Text)
    private String short_description;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd")
    private LocalDate date;

    public NewsArticle() {
    }

    public NewsArticle(String category, String headline, String authors, String link, String short_description, LocalDate date) {
        this.category = category;
        this.headline = headline;
        this.authors = authors;
        this.link = link;
        this.short_description = short_description;
        this.date = date;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getShort_description() {
        return short_description;
    }

    public void setShort_description(String short_description) {
        this.short_description = short_description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
