package com.learning.news_search;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsService service;

    public NewsController(NewsService service) {
        this.service = service;
    }

    @PostMapping("/load")
    public String loadData() {
        service.loadDataFromJson();
        return "Data loading triggered! Check the console for details.";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Search Engine is Online!";
    }
}
