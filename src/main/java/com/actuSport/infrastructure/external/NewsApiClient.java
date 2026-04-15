package com.actuSport.infrastructure.external;

import com.actuSport.domain.model.News;
import com.actuSport.domain.model.Sport;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NewsApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(NewsApiClient.class);
    
    @Value("${external.api.newsapi.base-url:https://newsapi.org/v2}")
    private String baseUrl;
    
    @Value("${external.api.newsapi.api-key}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    
    // Mapping des sports français vers les termes de recherche NewsAPI
    private static final Map<String, String> SPORT_KEYWORDS = Map.of(
        "soccer", "football OR soccer",
        "basketball", "basketball OR NBA",
        "tennis", "tennis",
        "hockey", "hockey OR NHL",
        "rugby", "rugby",
        "cycling", "cycling OR tour de france",
        "f1", "formula 1 OR F1",
        "judo", "judo",
        "swimming", "swimming"
    );
    
    // Classes imbriquées pour la réponse NewsAPI
    public static class Source {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("name")
        private String name;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public static class Article {
        @JsonProperty("source")
        private Source source;
        
        @JsonProperty("author")
        private String author;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("url")
        private String url;
        
        @JsonProperty("urlToImage")
        private String urlToImage;
        
        @JsonProperty("publishedAt")
        private String publishedAt;
        
        @JsonProperty("content")
        private String content;
        
        // Getters and setters
        public Source getSource() { return source; }
        public void setSource(Source source) { this.source = source; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getUrlToImage() { return urlToImage; }
        public void setUrlToImage(String urlToImage) { this.urlToImage = urlToImage; }
        
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    public static class NewsApiResponse {
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("totalResults")
        private Integer totalResults;
        
        @JsonProperty("articles")
        private List<Article> articles;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getTotalResults() { return totalResults; }
        public void setTotalResults(Integer totalResults) { this.totalResults = totalResults; }
        
        public List<Article> getArticles() { return articles; }
        public void setArticles(List<Article> articles) { this.articles = articles; }
    }
    
    public NewsApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<News> getSportsNews(String sport) {
        try {
            String keywords = SPORT_KEYWORDS.getOrDefault(sport, sport);
            List<News> allNews = new ArrayList<>();
            
            // Vérifier d'abord si nous sommes en rate limiting
            if (isRateLimited()) {
                logger.warn("NewsAPI rate limiting detected, skipping sync for sport: {}", sport);
                return List.of();
            }
            
            // Stratégie optimisée: Un seul appel API par sport pour éviter le rate limiting
            List<News> sportsNews = fetchNewsWithKeywords(keywords + " AND (sports OR sport)", sport, 100);
            allNews.addAll(sportsNews);
            
            // Éliminer les doublons basés sur le titre
            List<News> uniqueNews = removeDuplicates(allNews);
            
            logger.info("Successfully fetched {} unique news articles for sport: {} (total before dedup: {})", 
                       uniqueNews.size(), sport, allNews.size());
            return uniqueNews;
            
        } catch (Exception e) {
            logger.error("Error fetching news from NewsAPI for sport: {}", sport, e);
        }
        
        return List.of();
    }
    
    private boolean isRateLimited() {
        try {
            // Test simple pour vérifier si l'API est en rate limiting
            String testUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/everything")
                    .queryParam("q", "test")
                    .queryParam("pageSize", "1")
                    .queryParam("apiKey", apiKey)
                    .build()
                    .toUriString();
            
            String response = restTemplate.getForObject(testUrl, String.class);
            if (response != null && response.contains("rateLimited")) {
                logger.warn("NewsAPI rate limiting detected");
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.warn("Error checking NewsAPI rate limit status: {}", e.getMessage());
            // Si l'erreur mentionne "too many requests", c'est du rate limiting
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("too many requests")) {
                return true;
            }
            return false;
        }
    }
    
    private List<News> fetchNewsWithKeywords(String keywords, String sport, int pageSize) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/everything")
                    .queryParam("q", keywords)
                    .queryParam("language", "fr")
                    .queryParam("sortBy", "publishedAt")
                    .queryParam("pageSize", pageSize)
                    .queryParam("apiKey", apiKey)
                    .build()
                    .toUriString();
            
            return processNewsResponse(restTemplate.getForObject(url, NewsApiResponse.class), sport);
        } catch (Exception e) {
            logger.warn("Error fetching news with keywords: {}", keywords, e);
            return List.of();
        }
    }
    
    private List<News> fetchNewsInEnglish(String keywords, String sport, int pageSize) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/everything")
                    .queryParam("q", keywords + " AND (sports OR sport)")
                    .queryParam("language", "en")
                    .queryParam("sortBy", "publishedAt")
                    .queryParam("pageSize", pageSize)
                    .queryParam("apiKey", apiKey)
                    .build()
                    .toUriString();
            
            return processNewsResponse(restTemplate.getForObject(url, NewsApiResponse.class), sport);
        } catch (Exception e) {
            logger.warn("Error fetching English news for sport: {}", sport, e);
            return List.of();
        }
    }
    
    private List<News> fetchFromSportSources(String sport, int pageSize) {
        try {
            // Sources sportives populaires
            String[] sportSources = {"espn", "bbc-sport", "sky-sports", "the-sport-brew", "four-four-two", "talksport"};
            
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/top-headlines")
                    .queryParam("sources", String.join(",", sportSources))
                    .queryParam("pageSize", pageSize)
                    .queryParam("apiKey", apiKey)
                    .build()
                    .toUriString();
            
            return processNewsResponse(restTemplate.getForObject(url, NewsApiResponse.class), sport);
        } catch (Exception e) {
            logger.warn("Error fetching news from sport sources for sport: {}", sport, e);
            return List.of();
        }
    }
    
    private List<News> processNewsResponse(NewsApiResponse response, String sport) {
        if (response != null && response.getArticles() != null) {
            List<News> newsList = new ArrayList<>();
            
            for (Article article : response.getArticles()) {
                if (article.getTitle() != null && !article.getTitle().contains("[Removed]") 
                    && !article.getTitle().trim().isEmpty()) {
                    News news = convertToNews(article, sport);
                    newsList.add(news);
                }
            }
            
            return newsList;
        }
        return List.of();
    }
    
    private List<News> removeDuplicates(List<News> newsList) {
        return newsList.stream()
                .collect(Collectors.toMap(
                    News::getTitle, 
                    news -> news, 
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }
    
    private News convertToNews(Article article, String sport) {
        News news = new News();
        
        news.setTitle(article.getTitle());
        news.setContent(article.getContent() != null ? article.getContent() : article.getDescription());
        news.setSummary(article.getDescription());
        news.setAuthor(article.getAuthor());
        news.setSource(article.getSource() != null ? article.getSource().getName() : "Unknown");
        
        // Utiliser uniquement les vraies images de NewsAPI
        String imageUrl = article.getUrlToImage();
        logger.info("Processing article: {} - NewsAPI imageUrl: {}", article.getTitle(), imageUrl);
        
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            // Si pas d'image, ne pas créer d'image de substitution
            logger.warn("No image provided by NewsAPI for article: {} - skipping", article.getTitle());
            news.setImageUrl(null); // Garder null si pas d'image
        } else {
            logger.info("Using NewsAPI image: {}", imageUrl);
            news.setImageUrl(imageUrl);
        }
        
        news.setArticleUrl(article.getUrl());
        news.setPublishedAt(parsePublishedAt(article.getPublishedAt()));
        news.setCreatedAt(LocalDateTime.now());
        
        // Ne pas créer d'objet Sport transient pour éviter l'erreur Hibernate
        // Le sport sera associé via le service ou sera null pour l'instant
        news.setSport(null);
        
        return news;
    }
    
        
    private LocalDateTime parsePublishedAt(String publishedAt) {
        try {
            // NewsAPI retourne les dates au format ISO 8601: "2024-04-07T10:30:00Z"
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            logger.warn("Failed to parse published date: {}", publishedAt, e);
            return LocalDateTime.now();
        }
    }
}
