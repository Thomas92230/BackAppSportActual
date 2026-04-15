package com.actuSport.infrastructure.external;

import com.actuSport.domain.model.News;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class MultiSourceNewsClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiSourceNewsClient.class);
    
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    
    // NewsAPI - limitée mais fiable
    @Value("${external.api.newsapi.base-url:https://newsapi.org/v2}")
    private String newsApiBaseUrl;
    
    @Value("${external.api.newsapi.api-key}")
    private String newsApiKey;
    
    // The Guardian API - gratuite et généreuse
    @Value("${external.api.guardian.base-url:https://content.guardianapis.com}")
    private String guardianApiBaseUrl;
    
    @Value("${external.api.guardian.api-key:}")
    private String guardianApiKey;
    
    // ESPN API - pour les sports spécifiques
    @Value("${external.api.espn.base-url:https://site.web.api.espn.com}")
    private String espnApiBaseUrl;
    
    // Mapping des sports français vers les termes de recherche
    private static final java.util.Map<String, String> SPORT_KEYWORDS = java.util.Map.of(
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
    
    public MultiSourceNewsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.executorService = Executors.newFixedThreadPool(3); // 3 threads pour 3 sources
    }
    
    /**
     * Récupère les articles sportifs depuis plusieurs vraies sources API
     * Utilise NewsAPI et Guardian API uniquement (pas de mock/simulation)
     */
    public List<News> getSportsNewsFromMultipleSources(String sport, int totalArticles) {
        try {
            String keywords = SPORT_KEYWORDS.getOrDefault(sport, sport);
            
            logger.info("Fetching {} real articles for sport '{}' from NewsAPI and Guardian API", totalArticles, sport);
            
            // Répartition intelligente : plus d'articles de NewsAPI si Guardian n'est pas configuré
            final int newsApiArticles;
            final int guardianArticles;
            
            if (guardianApiKey != null && !guardianApiKey.isEmpty() && !guardianApiKey.equals("demo-key")) {
                // Si Guardian API est configurée, diviser les articles
                newsApiArticles = Math.max(10, totalArticles / 2);
                guardianArticles = Math.max(5, totalArticles / 2);
            } else {
                newsApiArticles = totalArticles;
                guardianArticles = 0;
            }
            
            // Lancer les sources en parallèle
            List<CompletableFuture<List<News>>> futures = new ArrayList<>();
            
            // Toujours utiliser NewsAPI (source principale)
            futures.add(CompletableFuture.supplyAsync(() -> 
                fetchFromNewsAPI(keywords, sport, newsApiArticles), executorService));
            
            // Utiliser Guardian API seulement si configurée
            if (guardianArticles > 0) {
                futures.add(CompletableFuture.supplyAsync(() -> 
                    fetchFromGuardian(keywords, sport, guardianArticles), executorService));
            }
            
            // Attendre que toutes les sources répondent
            List<News> allNews = new ArrayList<>();
            
            for (CompletableFuture<List<News>> future : futures) {
                try {
                    List<News> sourceNews = future.get();
                    allNews.addAll(sourceNews);
                    logger.info("Source contributed {} articles", sourceNews.size());
                } catch (Exception e) {
                    logger.warn("A source failed for sport '{}': {}", sport, e.getMessage());
                }
            }
            
            // Éliminer les doublons basés sur le titre
            List<News> uniqueNews = removeDuplicates(allNews);
            
            logger.info("Successfully fetched {} unique real articles for sport '{}' (total before dedup: {})", 
                       uniqueNews.size(), allNews.size());
            
            return uniqueNews;
            
        } catch (Exception e) {
            logger.error("Error fetching real news from multiple sources for sport: {}", sport, e);
            return List.of();
        }
    }
    
    /**
     * Source 1: NewsAPI (limitée mais fiable)
     */
    private List<News> fetchFromNewsAPI(String keywords, String sport, int pageSize) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(newsApiBaseUrl + "/everything")
                    .queryParam("q", keywords + " AND (sports OR sport)")
                    .queryParam("language", "fr")
                    .queryParam("sortBy", "publishedAt")
                    .queryParam("pageSize", pageSize)
                    .queryParam("apiKey", newsApiKey)
                    .build()
                    .toUriString();
            
            NewsApiResponse response = restTemplate.getForObject(url, NewsApiResponse.class);
            return processNewsApiResponse(response, sport, "NewsAPI");
            
        } catch (Exception e) {
            logger.warn("Error fetching from NewsAPI: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Source 2: The Guardian API (gratuite et généreuse)
     */
    private List<News> fetchFromGuardian(String keywords, String sport, int pageSize) {
        try {
            if (guardianApiKey == null || guardianApiKey.isEmpty()) {
                logger.warn("Guardian API key not configured, skipping Guardian source");
                return List.of();
            }
            
            String url = UriComponentsBuilder.fromHttpUrl(guardianApiBaseUrl + "/search")
                    .queryParam("q", keywords + " AND (sport OR sports)")
                    .queryParam("section", "sport")
                    .queryParam("order-by", "newest")
                    .queryParam("page-size", pageSize)
                    .queryParam("show-fields", "headline,byline,thumbnail,bodyText,webUrl")
                    .queryParam("api-key", guardianApiKey)
                    .build()
                    .toUriString();
            
            GuardianApiResponse response = restTemplate.getForObject(url, GuardianApiResponse.class);
            return processGuardianResponse(response, sport);
            
        } catch (Exception e) {
            logger.warn("Error fetching from Guardian API: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Source 3: ESPN RSS Feed (gratuit et fiable)
     */
    private List<News> fetchFromESPN(String keywords, String sport, int pageSize) {
        try {
            // Utiliser le RSS feed ESPN qui est gratuit et accessible
            String rssUrl = "https://www.espn.com/espn/rss/" + convertSportToESPN(sport) + "/news";
            
            // Pour l'instant, retourner une liste vide car ESPN RSS nécessite parsing XML
            // On pourrait ajouter un parser RSS plus tard si nécessaire
            logger.info("ESPN RSS feed for sport '{}' would be processed here", sport);
            return List.of();
            
        } catch (Exception e) {
            logger.warn("Error fetching from ESPN RSS: {}", e.getMessage());
            return List.of();
        }
    }
    
    private List<News> processNewsApiResponse(NewsApiResponse response, String sport, String source) {
        List<News> news = new ArrayList<>();
        if (response != null && response.getArticles() != null) {
            for (NewsApiClient.Article article : response.getArticles()) {
                news.add(convertNewsApiArticleToNews(article, sport, source));
            }
        }
        return news;
    }
    
    private List<News> processGuardianResponse(GuardianApiResponse response, String sport) {
        List<News> news = new ArrayList<>();
        if (response != null && response.getResponse() != null && response.getResponse().getResults() != null) {
            for (GuardianArticle article : response.getResponse().getResults()) {
                news.add(convertGuardianArticleToNews(article, sport));
            }
        }
        return news;
    }
    
    private News convertNewsApiArticleToNews(NewsApiClient.Article article, String sport, String source) {
        News news = new News();
        news.setTitle(article.getTitle());
        news.setContent(article.getContent() != null ? article.getContent() : article.getDescription());
        news.setSummary(article.getDescription());
        news.setAuthor(article.getAuthor());
        news.setSource(source + " - " + (article.getSource() != null ? article.getSource().getName() : "Unknown"));
        news.setImageUrl(article.getUrlToImage());
        news.setArticleUrl(article.getUrl());
        news.setPublishedAt(parsePublishedAt(article.getPublishedAt()));
        news.setCreatedAt(LocalDateTime.now());
        return news;
    }
    
    private News convertGuardianArticleToNews(GuardianArticle article, String sport) {
        News news = new News();
        news.setTitle(article.getHeadline());
        news.setContent(article.getBodyText());
        news.setSummary(article.getHeadline());
        news.setAuthor(article.getByline());
        news.setSource("The Guardian");
        news.setImageUrl(article.getThumbnail());
        news.setArticleUrl(article.getWebUrl());
        news.setPublishedAt(LocalDateTime.now()); // Guardian n'a pas de date claire
        news.setCreatedAt(LocalDateTime.now());
        return news;
    }
    
    private LocalDateTime parsePublishedAt(String publishedAt) {
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    private String convertSportToESPN(String sport) {
        switch (sport.toLowerCase()) {
            case "soccer": return "soccer";
            case "basketball": return "basketball";
            case "tennis": return "tennis";
            case "hockey": return "hockey";
            case "rugby": return "rugby";
            case "f1": return "f1";
            default: return sport;
        }
    }
    
    private List<News> removeDuplicates(List<News> allNews) {
        return allNews.stream()
                .distinct()
                .collect(Collectors.toList());
    }
    
    // Classes pour les réponses API
    public static class NewsApiResponse {
        @JsonProperty("articles")
        private List<NewsApiClient.Article> articles;
        
        public List<NewsApiClient.Article> getArticles() { return articles; }
        public void setArticles(List<NewsApiClient.Article> articles) { this.articles = articles; }
    }
    
    public static class GuardianApiResponse {
        @JsonProperty("response")
        private GuardianResponse response;
        
        public GuardianResponse getResponse() { return response; }
        public void setResponse(GuardianResponse response) { this.response = response; }
    }
    
    public static class GuardianResponse {
        @JsonProperty("results")
        private List<GuardianArticle> results;
        
        public List<GuardianArticle> getResults() { return results; }
        public void setResults(List<GuardianArticle> results) { this.results = results; }
    }
    
    public static class GuardianArticle {
        @JsonProperty("headline")
        private String headline;
        
        @JsonProperty("byline")
        private String byline;
        
        @JsonProperty("thumbnail")
        private String thumbnail;
        
        @JsonProperty("bodyText")
        private String bodyText;
        
        @JsonProperty("webUrl")
        private String webUrl;
        
        public String getHeadline() { return headline; }
        public void setHeadline(String headline) { this.headline = headline; }
        
        public String getByline() { return byline; }
        public void setByline(String byline) { this.byline = byline; }
        
        public String getThumbnail() { return thumbnail; }
        public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
        
        public String getBodyText() { return bodyText; }
        public void setBodyText(String bodyText) { this.bodyText = bodyText; }
        
        public String getWebUrl() { return webUrl; }
        public void setWebUrl(String webUrl) { this.webUrl = webUrl; }
    }
}
