package com.learning.news_search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsService service;

    public NewsController(NewsService service) {
        this.service = service;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Search Engine is Online!";
    }

    @PostMapping("/load")
    public String loadData() {
        service.loadDataFromJson();
        return "Data loading triggered! Check the console for details.";
    }

    @GetMapping("/search")
    public Page<NewsArticle> searchArticles(@RequestParam String keyword, Pageable pageable) {
        return service.searchArticles(keyword, pageable);
    }

    @GetMapping("/searchAgg")
    public SearchResponse searchWithAggregations(@RequestParam String keyword, Pageable pageable) {
        return service.searchWithAggregations(keyword, pageable);
    }

    @PostMapping("/click/{id}")
    public void incrementClickCount(@PathVariable String id) {
        service.incrementClickCount(id);
    }
}
