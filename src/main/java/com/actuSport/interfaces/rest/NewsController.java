package com.actuSport.interfaces.rest;

import com.actuSport.application.service.NewsService;
import com.actuSport.application.service.SportDataSyncService;
import com.actuSport.domain.model.News;
import com.actuSport.interfaces.rest.dto.NewsRequest;
import com.actuSport.interfaces.rest.dto.NewsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {
    
    private static final Logger logger = LoggerFactory.getLogger(NewsController.class);
    
    @Autowired
    private NewsService newsService;
    
    @Autowired
    private SportDataSyncService syncService;
    
    @GetMapping("/sync")
    public ResponseEntity<String> forceSyncNews() {
        syncService.syncNews();
        return ResponseEntity.ok("Sync initiated for all sports news from NewsAPI");
    }
    
    @GetMapping("/sync/recent-test")
    public ResponseEntity<String> createRecentTestArticles() {
        syncService.createRecentTestArticles();
        return ResponseEntity.ok("Recent test articles created successfully");
    }
    
    @DeleteMapping("/all")
    public ResponseEntity<String> deleteAllNews() {
        try {
            newsService.deleteAllNews();
            return ResponseEntity.ok("All news articles deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting all news articles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting news articles");
        }
    }
    
    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsService.getAllNewsPaginated(pageable);
        return ResponseEntity.ok(newsPage.map(this::toResponse));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        return newsService.getNewsById(id)
                .map(news -> ResponseEntity.ok(toResponse(news)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/sport/{sportCode}")
    public ResponseEntity<List<NewsResponse>> getNewsBySport(@PathVariable String sportCode) {
        List<News> newsList = newsService.getNewsBySport(sportCode);
        return ResponseEntity.ok(newsList.stream().map(this::toResponse).collect(Collectors.toList()));
    }
    
    @GetMapping("/sport/{sportCode}/paginated")
    public ResponseEntity<Page<NewsResponse>> getNewsBySportPaginated(
            @PathVariable String sportCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsService.getNewsBySportPaginated(sportCode, pageable);
        return ResponseEntity.ok(newsPage.map(this::toResponse));
    }
    
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<NewsResponse>> getNewsByTeam(@PathVariable Long teamId) {
        List<News> newsList = newsService.getNewsByTeam(teamId);
        return ResponseEntity.ok(newsList.stream().map(this::toResponse).collect(Collectors.toList()));
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<NewsResponse>> getRecentNews(@RequestParam(defaultValue = "24") int hours) {
        List<News> newsList = newsService.getRecentNews(hours);
        return ResponseEntity.ok(newsList.stream().map(this::toResponse).collect(Collectors.toList()));
    }
    
    @GetMapping("/recent/enhanced")
    public ResponseEntity<List<NewsResponse>> getRecentNewsEnhanced(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit) {
        List<News> newsList = newsService.getRecentNews(hours);
        
        // Limiter le nombre de résultats
        List<News> limitedNews = newsList.stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        // Enrichir les articles avec des URLs d'accès complet et images optimisées
        List<NewsResponse> enrichedNews = limitedNews.stream().map(news -> {
            NewsResponse response = toResponse(news);
            
            // Ajouter l'URL pour accéder au contenu complet
            response.setFullArticleUrl("/api/news/" + news.getId() + "/full");
            response.setContentUrl("/api/news/" + news.getId() + "/content");
            
            // Ajouter l'URL du proxy pour l'image avec gestion d'erreur
            if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                try {
                    String encodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                    response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                } catch (Exception e) {
                    logger.warn("Error encoding image URL for news {}: {}", news.getId(), news.getImageUrl(), e);
                    response.setImageUrl(news.getImageUrl()); // Fallback to original URL
                }
            }
            
            return response;
        }).collect(Collectors.toList());
        
        logger.info("Returning {} recent news articles from the last {} hours", enrichedNews.size(), hours);
        return ResponseEntity.ok(enrichedNews);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<NewsResponse>> searchNews(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean enhanced) {
        
        List<News> newsList = newsService.searchNews(keyword);
        
        // Limiter les résultats
        List<News> limitedNews = newsList.stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        if (enhanced) {
            // Retourner des résultats enrichis avec images proxy
            List<NewsResponse> enrichedNews = limitedNews.stream().map(news -> {
                NewsResponse response = toResponse(news);
                response.setFullArticleUrl("/api/news/" + news.getId() + "/full");
                response.setContentUrl("/api/news/" + news.getId() + "/content");
                
                if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                    try {
                        String encodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                        response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                    } catch (Exception e) {
                        logger.warn("Error encoding image URL for news {}: {}", news.getId(), news.getImageUrl(), e);
                        response.setImageUrl(news.getImageUrl());
                    }
                }
                return response;
            }).collect(Collectors.toList());
            
            logger.info("Enhanced search for '{}' returned {} results", keyword, enrichedNews.size());
            return ResponseEntity.ok(enrichedNews);
        } else {
            logger.info("Basic search for '{}' returned {} results", keyword, limitedNews.size());
            return ResponseEntity.ok(limitedNews.stream().map(this::toResponse).collect(Collectors.toList()));
        }
    }
    
    @GetMapping("/search/{sportCode}")
    public ResponseEntity<List<NewsResponse>> searchNewsBySport(
            @PathVariable String sportCode, 
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean enhanced) {
        
        List<News> newsList = newsService.searchNewsBySport(sportCode, keyword);
        
        // Limiter les résultats
        List<News> limitedNews = newsList.stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        if (enhanced) {
            // Retourner des résultats enrichis
            List<NewsResponse> enrichedNews = limitedNews.stream().map(news -> {
                NewsResponse response = toResponse(news);
                response.setFullArticleUrl("/api/news/" + news.getId() + "/full");
                response.setContentUrl("/api/news/" + news.getId() + "/content");
                
                if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                    try {
                        String encodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                        response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                    } catch (Exception e) {
                        logger.warn("Error encoding image URL for news {}: {}", news.getId(), news.getImageUrl(), e);
                        response.setImageUrl(news.getImageUrl());
                    }
                }
                return response;
            }).collect(Collectors.toList());
            
            logger.info("Enhanced search for '{}' in sport '{}' returned {} results", keyword, sportCode, enrichedNews.size());
            return ResponseEntity.ok(enrichedNews);
        } else {
            logger.info("Basic search for '{}' in sport '{}' returned {} results", keyword, sportCode, limitedNews.size());
            return ResponseEntity.ok(limitedNews.stream().map(this::toResponse).collect(Collectors.toList()));
        }
    }
    
    @GetMapping("/search/advanced")
    public ResponseEntity<List<NewsResponse>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String sportCode,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean enhanced) {
        
        List<News> newsList = newsService.advancedSearch(keyword, source, author, sportCode, sortBy);
        
        // Limiter les résultats
        List<News> limitedNews = newsList.stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        if (enhanced) {
            // Retourner des résultats enrichis
            List<NewsResponse> enrichedNews = limitedNews.stream().map(news -> {
                NewsResponse response = toResponse(news);
                response.setFullArticleUrl("/api/news/" + news.getId() + "/full");
                response.setContentUrl("/api/news/" + news.getId() + "/content");
                
                if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                    try {
                        String encodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                        response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                    } catch (Exception e) {
                        logger.warn("Error encoding image URL for news {}: {}", news.getId(), news.getImageUrl(), e);
                        response.setImageUrl(news.getImageUrl());
                    }
                }
                return response;
            }).collect(Collectors.toList());
            
            logger.info("Advanced search returned {} results", enrichedNews.size());
            return ResponseEntity.ok(enrichedNews);
        } else {
            logger.info("Advanced search returned {} results", limitedNews.size());
            return ResponseEntity.ok(limitedNews.stream().map(this::toResponse).collect(Collectors.toList()));
        }
    }
    
    @GetMapping("/source/{source}")
    public ResponseEntity<List<NewsResponse>> getNewsBySource(@PathVariable String source) {
        List<News> newsList = newsService.getNewsBySource(source);
        return ResponseEntity.ok(newsList.stream().map(this::toResponse).collect(Collectors.toList()));
    }
    
    @GetMapping("/daterange/{sportCode}")
    public ResponseEntity<List<NewsResponse>> getNewsByDateRange(
            @PathVariable String sportCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        List<News> newsList = newsService.getNewsByDateRange(sportCode, startDate, endDate);
        return ResponseEntity.ok(newsList.stream().map(this::toResponse).collect(Collectors.toList()));
    }
    
    @PostMapping
    public ResponseEntity<NewsResponse> createNews(@RequestBody NewsRequest request) {
        News news = toEntity(request);
        News createdNews = newsService.saveNews(news);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(createdNews));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<NewsResponse> updateNews(@PathVariable Long id, @RequestBody NewsRequest request) {
        News newsDetails = toEntity(request);
        News updatedNews = newsService.updateNews(id, newsDetails);
        return updatedNews != null ? ResponseEntity.ok(toResponse(updatedNews)) 
                                  : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/full")
    public ResponseEntity<String> getFullArticle(@PathVariable Long id) {
        return newsService.getNewsById(id)
                .map(news -> {
                    if (news.getArticleUrl() != null && !news.getArticleUrl().isEmpty()) {
                        // Retourner l'URL de l'article original pour redirection
                        return ResponseEntity.status(302)
                                .header("Location", news.getArticleUrl())
                                .body("Redirection vers l'article complet");
                    } else {
                        // Retourner le contenu complet si pas d'URL externe
                        return ResponseEntity.ok(news.getContent());
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/content")
    public ResponseEntity<String> getArticleContent(@PathVariable Long id) {
        return newsService.getNewsById(id)
                .map(news -> ResponseEntity.ok(news.getContent()))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/proxy/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            // Valider l'URL pour des raisons de sécurité
            if (url == null || url.trim().isEmpty()) {
                logger.warn("Empty URL provided for image proxy");
                return ResponseEntity.badRequest().build();
            }
            
            // Décoder l'URL si elle est encodée
            String decodedUrl = java.net.URLDecoder.decode(url, java.nio.charset.StandardCharsets.UTF_8);
            logger.info("Proxying image: {}", decodedUrl);
            
            // Valider que l'URL est bien formée
            if (!isValidImageUrl(decodedUrl)) {
                logger.warn("Invalid image URL format: {}", decodedUrl);
                return ResponseEntity.badRequest().build();
            }
            
            // Télécharger l'image avec timeout
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(10000);
            RestTemplate restTemplate = new RestTemplate(factory);
            
            byte[] imageBytes = restTemplate.getForObject(decodedUrl, byte[].class);
            
            if (imageBytes != null && imageBytes.length > 0) {
                // Détecter le type de contenu
                String contentType = detectContentType(decodedUrl);
                
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .header("Cache-Control", "public, max-age=3600") // Cache 1 heure
                        .header("Access-Control-Allow-Origin", "*")
                        .body(imageBytes);
            }
            
            logger.warn("No image data received for URL: {}", decodedUrl);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            logger.error("URL decoding error for: {}", url, e);
            return ResponseEntity.badRequest().build();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.error("Timeout or connection error accessing image: {}", url, e);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
        } catch (Exception e) {
            logger.error("Error proxying image: {}", url, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private boolean isValidImageUrl(String url) {
        try {
            java.net.URL uri = new java.net.URL(url);
            String protocol = uri.getProtocol();
            String host = uri.getHost();
            
            // Vérifier le protocole et l'hôte
            return ("http".equals(protocol) || "https".equals(protocol)) 
                   && host != null && !host.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    private String detectContentType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".png")) {
            return "image/png";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerUrl.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerUrl.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            return "image/jpeg"; // Par défaut
        }
    }
    
    @GetMapping("/enhanced")
    public ResponseEntity<Page<NewsResponse>> getEnhancedNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsService.getAllNewsPaginated(pageable);
        
        // Enrichir les actualités avec des URLs d'accès complet
        Page<NewsResponse> enrichedNews = newsPage.map(news -> {
            NewsResponse response = toResponse(news);
            // Ajouter l'URL pour accéder au contenu complet
            response.setFullArticleUrl("/api/news/" + news.getId() + "/full");
            response.setContentUrl("/api/news/" + news.getId() + "/content");
            
            // Ajouter l'URL du proxy pour l'image
            if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                try {
                    String encodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                    response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                } catch (Exception e) {
                    logger.warn("Error encoding image URL for news {}: {}", news.getId(), news.getImageUrl(), e);
                    response.setImageUrl(news.getImageUrl()); // Fallback to original URL
                }
            }
            
            return response;
        });
        
        return ResponseEntity.ok(enrichedNews);
    }
    
    @GetMapping("/sport/{sportCode}/enhanced")
    public ResponseEntity<Page<NewsResponse>> getEnhancedNewsBySport(
            @PathVariable String sportCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsService.getNewsBySportPaginated(sportCode, pageable);
        
        // Enrichir les actualités avec des URLs d'accès complet
        Page<NewsResponse> enrichedNews = newsPage.map(news -> {
            NewsResponse response = toResponse(news);
            // Ajouter l'URL pour accéder au contenu complet
            response.setFullArticleUrl("/api/news/" + news.getId() + "/full");
            response.setContentUrl("/api/news/" + news.getId() + "/content");
            
            // Ajouter l'URL du proxy pour l'image
            if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                try {
                    String encodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                    response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                } catch (Exception e) {
                    logger.warn("Error encoding image URL for news {}: {}", news.getId(), news.getImageUrl(), e);
                    response.setImageUrl(news.getImageUrl()); // Fallback to original URL
                }
            }
            
            return response;
        });
        
        return ResponseEntity.ok(enrichedNews);
    }

    private NewsResponse toResponse(News news) {
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setContent(news.getContent());
        response.setSummary(news.getSummary());
        response.setAuthor(news.getAuthor());
        response.setSource(news.getSource());
        response.setImageUrl(news.getImageUrl());
        response.setArticleUrl(news.getArticleUrl());
        response.setPublishedAt(news.getPublishedAt());
        
        if (news.getSport() != null) {
            response.setSportName(news.getSport().getName());
        }
        if (news.getTeam() != null) {
            response.setTeamName(news.getTeam().getName());
        }
        if (news.getCompetition() != null) {
            response.setCompetitionName(news.getCompetition().getName());
        }
        
        return response;
    }

    private News toEntity(NewsRequest request) {
        News news = new News();
        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        news.setSummary(request.getSummary());
        news.setAuthor(request.getAuthor());
        news.setSource(request.getSource());
        news.setImageUrl(request.getImageUrl());
        news.setArticleUrl(request.getArticleUrl());
        news.setPublishedAt(request.getPublishedAt());
        
        return news;
    }
}
