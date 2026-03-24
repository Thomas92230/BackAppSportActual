package com.actuSport.interfaces.rest;

import com.actuSport.domain.model.News;
import com.actuSport.application.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {
    
    @Autowired
    private NewsService newsService;
    
    @GetMapping
    public ResponseEntity<Page<News>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<News> news = newsService.getAllNewsPaginated(pageable);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<News> getNewsById(@PathVariable Long id) {
        Optional<News> news = newsService.getNewsById(id);
        return news.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/sport/{sportCode}")
    public ResponseEntity<List<News>> getNewsBySport(@PathVariable String sportCode) {
        List<News> news = newsService.getNewsBySport(sportCode);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/sport/{sportCode}/paginated")
    public ResponseEntity<Page<News>> getNewsBySportPaginated(
            @PathVariable String sportCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<News> news = newsService.getNewsBySportPaginated(sportCode, pageable);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<News>> getNewsByTeam(@PathVariable Long teamId) {
        List<News> news = newsService.getNewsByTeam(teamId);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<News>> getRecentNews(@RequestParam(defaultValue = "24") int hours) {
        List<News> news = newsService.getRecentNews(hours);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<News>> searchNews(@RequestParam String keyword) {
        List<News> news = newsService.searchNews(keyword);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/search/{sportCode}")
    public ResponseEntity<List<News>> searchNewsBySport(
            @PathVariable String sportCode, 
            @RequestParam String keyword) {
        List<News> news = newsService.searchNewsBySport(sportCode, keyword);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/source/{source}")
    public ResponseEntity<List<News>> getNewsBySource(@PathVariable String source) {
        List<News> news = newsService.getNewsBySource(source);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/daterange/{sportCode}")
    public ResponseEntity<List<News>> getNewsByDateRange(
            @PathVariable String sportCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        
        List<News> news = newsService.getNewsByDateRange(sportCode, start, end);
        return ResponseEntity.ok(news);
    }
    
    @PostMapping
    public ResponseEntity<News> createNews(@RequestBody News news) {
        News createdNews = newsService.saveNews(news);
        return ResponseEntity.ok(createdNews);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<News> updateNews(@PathVariable Long id, @RequestBody News newsDetails) {
        News updatedNews = newsService.updateNews(id, newsDetails);
        return updatedNews != null ? ResponseEntity.ok(updatedNews) 
                                  : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }
}
