package com.learning.news_search;

import java.util.List;
import java.util.Map;

// A simple container for our clean data
public record SearchResponse(
        List<NewsArticle> articles,
        Map<String, Long> categoryCounts
) {}