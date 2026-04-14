package com.actuSport.interfaces.rest;

import com.actuSport.application.service.NewsService;
import com.actuSport.application.service.SportDataSyncService;
import com.actuSport.domain.model.News;
import com.actuSport.domain.model.Sport;
import com.actuSport.domain.repository.NewsRepository;
import com.actuSport.interfaces.rest.dto.NewsRequest;
import com.actuSport.interfaces.rest.dto.NewsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
    
    @Autowired
    private NewsRepository newsRepository;
    
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
    
    // Endpoint de test simple pour vérifier que le contrôleur fonctionne
    @GetMapping("/test/article")
    public ResponseEntity<String> testArticleView() {
        String testHtml = """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Article Test</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
                    .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    h1 { color: #333; }
                    .meta { color: #666; font-size: 0.9em; margin-bottom: 20px; }
                    .content { line-height: 1.6; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Le club français remporte la Ligue des Champions</h1>
                    <div class="meta">Source: Test | Auteur: Test | Date: 14 avril 2026</div>
                    <div class="content">
                        Ceci est un test pour vérifier que l'affichage des articles fonctionne correctement.
                        Le contenu s'affiche normalement sans redirection vers des URLs d'exemples.
                    </div>
                    <a href="javascript:history.back()">« Retour</a>
                </div>
            </body>
            </html>
            """;
        
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(testHtml);
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
        
        // Enrichir les images avec le proxy ou des images de remplacement
        Page<NewsResponse> enrichedNews = newsPage.map(news -> {
            NewsResponse response = toResponse(news);
            if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                try {
                    String encodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                    response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                } catch (Exception e) {
                    logger.warn("Error encoding image URL for news {}: {}", news.getId(), news.getImageUrl(), e);
                    // Image de remplacement en cas d'erreur
                    response.setImageUrl(getFallbackImageUrl(news.getId()));
                }
            } else {
                // Image de remplacement si aucune image n'est disponible
                response.setImageUrl(getFallbackImageUrl(news.getId()));
            }
            return response;
        });
        
        return ResponseEntity.ok(enrichedNews);
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
        try {
            // Solution directe : accéder à la base de données sans passer par les services
            News news = newsRepository.findById(id).orElse(null);
            if (news == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Créer une page HTML complète avec le contenu de l'article
            String fullHtml = createFullArticleHtml(news);
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(fullHtml);
                    
        } catch (Exception e) {
            logger.error("Error accessing article {}: {}", id, e.getMessage());
            // Retourner une page d'erreur simple
            String errorHtml = String.format("""
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Article #%d</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
                        .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; text-align: center; }
                        h1 { color: #dc3545; }
                        .message { color: #666; margin: 20px 0; }
                        .back { display: inline-block; padding: 10px 20px; background: #007bff; color: white; text-decoration: none; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Article #%d</h1>
                        <p class="message">Cet article est accessible via l'API enhanced.</p>
                        <a href="/api/news/enhanced" class="back">Voir tous les articles</a>
                    </div>
                </body>
                </html>
                """, id, id);
            
            return ResponseEntity.status(500)
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }
    
    private boolean isExampleUrl(String url) {
        if (url == null) return false;
        return url.contains("example.com") || 
               url.contains("exemple") || 
               url.startsWith("http://example.") ||
               url.startsWith("https://example.") ||
               url.contains("placeholder") ||
               url.contains("test.") ||
               url.contains("demo.");
    }
    
    private String createFullArticleHtml(News news) {
        String imageUrl = news.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            // Utiliser une image par défaut si aucune image n'est disponible
            imageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800&h=400&fit=crop";
        }
        
        return String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
                    .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .article-image { width: 100%%; max-height: 400px; object-fit: cover; border-radius: 8px; margin-bottom: 20px; }
                    .article-title { font-size: 2.5em; color: #333; margin-bottom: 10px; line-height: 1.2; }
                    .article-meta { color: #666; font-size: 0.9em; margin-bottom: 20px; padding: 10px; background: #f8f9fa; border-radius: 5px; }
                    .article-content { font-size: 1.1em; line-height: 1.6; color: #444; white-space: pre-wrap; }
                    .back-button { display: inline-block; margin-top: 20px; padding: 10px 20px; background: #007bff; color: white; text-decoration: none; border-radius: 5px; }
                    .back-button:hover { background: #0056b3; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1 class="article-title">%s</h1>
                    <div class="article-meta">
                        <strong>Source:</strong> %s | 
                        <strong>Auteur:</strong> %s | 
                        <strong>Date:</strong> %s
                    </div>
                    <img src="%s" alt="%s" class="article-image" onerror="this.style.display='none'">
                    <div class="article-content">%s</div>
                    <a href="javascript:history.back()" class="back-button">« Retour aux articles</a>
                </div>
            </body>
            </html>
            """,
            escapeHtml(news.getTitle()),
            escapeHtml(news.getTitle()),
            escapeHtml(news.getSource() != null ? news.getSource() : "Source inconnue"),
            escapeHtml(news.getAuthor() != null ? news.getAuthor() : "Auteur inconnu"),
            news.getPublishedAt() != null ? news.getPublishedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm")) : "Date inconnue",
            imageUrl,
            escapeHtml(news.getTitle()),
            escapeHtml(news.getContent() != null ? news.getContent() : "Contenu non disponible")
        );
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#039;");
    }
    
    private boolean hasValidImageAndLink(News news) {
        // Vérifier que l'article a un contenu valide
        if (news.getContent() == null || news.getContent().trim().isEmpty()) {
            return false;
        }
        
        // Vérifier que l'article a un titre valide
        if (news.getTitle() == null || news.getTitle().trim().isEmpty()) {
            return false;
        }
        
        // Vérifier que l'article a une VRAIE image (pas d'image générée)
        boolean hasValidImage = false;
        if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
            // N'accepter que les vraies images (pas example.com et pas unsplash générées)
            if (!isExampleUrl(news.getImageUrl()) && !isGeneratedImage(news.getImageUrl())) {
                hasValidImage = true;
            }
        }
        
        // Vérifier que l'article a un lien valide
        boolean hasValidLink = false;
        if (news.getArticleUrl() != null && !news.getArticleUrl().isEmpty()) {
            if (!isExampleUrl(news.getArticleUrl())) {
                hasValidLink = true;
            }
        }
        
        return hasValidImage && hasValidLink;
    }
    
    private boolean isGeneratedImage(String imageUrl) {
        if (imageUrl == null) return false;
        // Détecter les images Unsplash générées automatiquement
        return imageUrl.contains("unsplash.com") && 
               (imageUrl.contains("photo-1571019613454-1cb2f99b2d8b") ||  // Image stade football
                imageUrl.contains("photo-1595435934249-5e7e2a0b5d98") ||  // Image tennis
                imageUrl.contains("photo-1517466787929-bc90951d0974") ||  // Image F1
                imageUrl.contains("photo-1612872087720-bb876e2e67d1") ||  // Image rugby
                imageUrl.contains("photo-1541252260730-0412e8e2108e") ||  // Image célébration
                imageUrl.contains("photo-1506197600528-5b297d02b8db") ||  // Image trophée
                imageUrl.contains("photo-1461896836934-ffe607ba8211") ||  // Image athlétisme
                imageUrl.contains("photo-1571902943202-507ec2618e8f") ||  // Image natation
                imageUrl.contains("photo-1540555700478-4be2892cef28"));    // Image cyclisme
    }
    
    private String extractDetectionKey(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "empty";
        }
        
        // Extraire les 3-4 premiers mots significatifs (noms propres, mots clés)
        String[] words = title.split("\\s+");
        java.util.List<String> keyWords = new java.util.ArrayList<>();
        
        for (String word : words) {
            word = word.replaceAll("[^a-zA-Z0-9àâäéèêëïîöôùûüç]", ""); // Nettoyer les caractères spéciaux
            if (word.length() > 2 && !isCommonWord(word)) {
                keyWords.add(word.toLowerCase());
                if (keyWords.size() >= 4) break; // Limiter à 4 mots clés
            }
        }
        
        // Si aucun mot clé significatif, utiliser les 2 premiers mots
        if (keyWords.isEmpty() && words.length >= 2) {
            return words[0].toLowerCase() + "_" + words[1].toLowerCase();
        }
        
        return String.join("_", keyWords);
    }
    
    private boolean isCommonWord(String word) {
        // Mots courants à ignorer pour la détection de doublons
        String[] commonWords = {"le", "la", "les", "de", "du", "des", "et", "est", "sont", "pour", "avec", "sur", "par", "dans", "une", "un", "aux", "plus", "pas", "que", "qui", "se", "son", "sa", "ses", "ce", "cet", "cette", "ces", "il", "elle", "ils", "elles", "on", "nous", "vous", "je", "tu", "me", "te", "lui", "leur", "y", "en", "à", "a"};
        return java.util.Arrays.asList(commonWords).contains(word.toLowerCase());
    }
    
    @GetMapping("/{id}/content")
    public ResponseEntity<String> getArticleContent(@PathVariable Long id) {
        return newsService.getNewsById(id)
                .map(news -> ResponseEntity.ok(news.getContent()))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Endpoint direct qui accède à la base de données pour afficher les articles
    @GetMapping("/{id}/view")
    public ResponseEntity<String> viewArticle(@PathVariable Long id) {
        try {
            // Accès direct à la base de données pour contourner les problèmes de service
            News news = newsRepository.findById(id).orElse(null);
            if (news == null) {
                String errorHtml = createErrorArticleHtml(id);
                return ResponseEntity.status(404)
                        .contentType(org.springframework.http.MediaType.TEXT_HTML)
                        .body(errorHtml);
            }
            
            if (news.getArticleUrl() != null && !news.getArticleUrl().isEmpty()) {
                // Vérifier si l'URL est une URL d'exemple
                if (isExampleUrl(news.getArticleUrl())) {
                    // Créer une page HTML complète avec le contenu de l'article
                    String fullHtml = createFullArticleHtml(news);
                    return ResponseEntity.ok()
                            .contentType(org.springframework.http.MediaType.TEXT_HTML)
                            .body(fullHtml);
                } else {
                    // Retourner l'URL de l'article original pour redirection
                    return ResponseEntity.status(302)
                            .header("Location", news.getArticleUrl())
                            .body("Redirection vers l'article complet");
                }
            } else {
                // Retourner le contenu complet si pas d'URL externe
                String fullHtml = createFullArticleHtml(news);
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.TEXT_HTML)
                        .body(fullHtml);
            }
        } catch (Exception e) {
            logger.error("Error accessing article {}: {}", id, e.getMessage());
            // Créer une page d'erreur conviviale
            String errorHtml = createErrorArticleHtml(id);
            return ResponseEntity.status(500)
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }
    
    private String createErrorArticleHtml(Long id) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Article non disponible</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
                    .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); text-align: center; }
                    .error-icon { font-size: 4em; color: #dc3545; margin-bottom: 20px; }
                    .error-title { font-size: 2em; color: #333; margin-bottom: 10px; }
                    .error-message { font-size: 1.1em; color: #666; margin-bottom: 30px; }
                    .back-button { display: inline-block; padding: 10px 20px; background: #007bff; color: white; text-decoration: none; border-radius: 5px; }
                    .back-button:hover { background: #0056b3; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="error-icon">!</div>
                    <h1 class="error-title">Article non disponible</h1>
                    <p class="error-message">L'article #%d n'est pas temporairement accessible. Veuillez réessayer plus tard.</p>
                    <a href="javascript:history.back()" class="back-button">« Retour aux articles</a>
                </div>
            </body>
            </html>
            """, id);
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
            
            // Télécharger l'image avec timeout et configuration améliorée
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000); // 10 secondes
            factory.setReadTimeout(15000);  // 15 secondes
            RestTemplate restTemplate = new RestTemplate(factory);
            
            // Ajouter des headers pour simuler un navigateur
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.add("Accept", "image/*,*/*;q=0.8");
            headers.add("Accept-Language", "en-US,en;q=0.5");
            headers.add("Accept-Encoding", "gzip, deflate");
            headers.add("Connection", "keep-alive");
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            logger.info("Attempting to download image from: {}", decodedUrl);
            org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
                decodedUrl, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                byte[].class
            );
            
            byte[] imageBytes = response.getBody();
            
            if (imageBytes != null && imageBytes.length > 0) {
                logger.info("Successfully downloaded image: {} bytes", imageBytes.length);
                
                // Détecter le type de contenu
                String contentType = detectContentType(decodedUrl);
                if (contentType == null) {
                    contentType = "image/jpeg"; // Default fallback
                }
                
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .header("Cache-Control", "public, max-age=3600") // Cache 1 heure
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                        .header("Access-Control-Allow-Headers", "Content-Type")
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
    
    private String getFallbackImageUrl(Long newsId) {
        // Générer une URL d'image de remplacement basée sur l'ID de l'actualité
        // Utiliser des images différentes selon l'ID pour plus de variété
        String[] imageSeeds = {
            "sports-news-" + (newsId % 10),
            "sport-article-" + (newsId % 15),
            "athletic-news-" + (newsId % 20),
            "sports-media-" + (newsId % 25)
        };
        
        String seed = imageSeeds[(int)(newsId % imageSeeds.length)];
        return "https://picsum.photos/seed/" + seed + "/400/200.jpg";
    }
    
    private String getSportSpecificImageUrl(String sportName, Long newsId, String title, String content) {
        // Système de filtrage avancé basé sur l'analyse sémantique précise
        if (sportName == null || sportName.trim().isEmpty()) {
            return getContentBasedImageUrl(newsId, title, content);
        }
        
        String sportLower = sportName.toLowerCase();
        String titleLower = title != null ? title.toLowerCase() : "";
        String contentLower = content != null ? content.toLowerCase() : "";
        String fullText = (title + " " + content).toLowerCase();
        
        // Analyse sémantique précise pour chaque sport
        switch (sportLower) {
            case "football":
            case "soccer":
                return getFootballImageUrl(newsId, titleLower, contentLower, fullText);
                
            case "basketball":
                return getBasketballImageUrl(newsId, titleLower, contentLower, fullText);
                
            case "tennis":
                return getTennisImageUrl(newsId, titleLower, contentLower, fullText);
                
            case "rugby":
                return getRugbyImageUrl(newsId, titleLower, contentLower, fullText);
                
            case "f1":
            case "formula 1":
                return getF1ImageUrl(newsId, titleLower, contentLower, fullText);
                
            case "swimming":
                return getSwimmingImageUrl(newsId, titleLower, contentLower, fullText);
                
            case "athletics":
            case "athlétisme":
                return getAthleticsImageUrl(newsId, titleLower, contentLower, fullText);
                
            case "judo":
                return getJudoImageUrl(newsId, titleLower, contentLower, fullText);
                
            case "cycling":
                return getCyclingImageUrl(newsId, titleLower, contentLower, fullText);
                
            default:
                return getContentBasedImageUrl(newsId, title, content);
        }
    }
    
    private String getFootballImageUrl(Long newsId, String title, String content, String fullText) {
        // Analyse précise pour le football avec détection améliorée
        if (containsAny(title, "ligue des champions", "champions league", "ldc", "c1", "europa", "ligue europa")) {
            return "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=200&fit=crop"; // Stade européen
        } else if (containsAny(title, "victoire", "win", "gagne", "remporte", "triomphe")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (containsAny(title, "finale", "final", "quarter", "demi", "quart")) {
            return "https://images.unsplash.com/photo-1506197600528-5b297d02b8db?w=400&h=200&fit=crop"; // Trophée
        } else if (containsAny(title, "club", "equipe", "team", "francais", "français")) {
            return "https://images.unsplash.com/photo-1579952363873-27d3bf5e9b81?w=400&h=200&fit=crop"; // Équipe
        } else if (containsAny(title, "match", "game", "rencontre", "derby", "classico")) {
            return "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=200&fit=crop"; // Stade
        } else if (containsAny(title, "joueur", "player", "transfert", "recrutement")) {
            return "https://images.unsplash.com/photo-1579952363873-27d3bf5e9b81?w=400&h=200&fit=crop"; // Équipe
        } else if (containsAny(title, "but", "goal", "marque", "score")) {
            return "https://images.unsplash.com/photo-1431329165308-7679c4105f9e?w=400&h=200&fit=crop"; // Action
        } else {
            return "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=200&fit=crop"; // Stade par défaut
        }
    }
    
    private String getBasketballImageUrl(Long newsId, String title, String content, String fullText) {
        // Analyse précise pour le basketball
        if (containsAny(title, "victoire", "win", "gagne", "remporte", "champion")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (containsAny(title, "nba", "playoffs", "finale", "final")) {
            return "https://images.unsplash.com/photo-1506197600528-5b297d02b8db?w=400&h=200&fit=crop"; // Trophée
        } else if (containsAny(title, "joueur", "player", "star", "mvp")) {
            return "https://images.unsplash.com/photo-1554068393-7a56f670c326?w=400&h=200&fit=crop"; // Joueur
        } else if (containsAny(title, "match", "game", "rencontre", "opposition")) {
            return "https://images.unsplash.com/photo-1554068393-7a56f670c326?w=400&h=200&fit=crop"; // Terrain
        } else if (containsAny(title, "dunk", "tir", "shot", "panier")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Action
        } else {
            return "https://images.unsplash.com/photo-1554068393-7a56f670c326?w=400&h=200&fit=crop"; // Terrain par défaut
        }
    }
    
    private String getTennisImageUrl(Long newsId, String title, String content, String fullText) {
        // Analyse précise pour le tennis
        if (containsAny(title, "victoire", "win", "gagne", "remporte", "titre")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (containsAny(title, "roland", "wimbledon", "us", "open", "grand")) {
            return "https://images.unsplash.com/photo-1506197600528-5b297d02b8db?w=400&h=200&fit=crop"; // Trophée
        } else if (containsAny(title, "nadal", "federer", "djokovic", "serena")) {
            return "https://images.unsplash.com/photo-1595435934249-5e7e2a0b5d98?w=400&h=200&fit=crop"; // Tennis
        } else if (containsAny(title, "match", "game", "finale", "demi")) {
            return "https://images.unsplash.com/photo-1595435934249-5e7e2a0b5d98?w=400&h=200&fit=crop"; // Court
        } else if (containsAny(title, "service", "ace", "smash", "coup")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Action
        } else {
            return "https://images.unsplash.com/photo-1595435934249-5e7e2a0b5d98?w=400&h=200&fit=crop"; // Court par défaut
        }
    }
    
    private String getRugbyImageUrl(Long newsId, String title, String content, String fullText) {
        // Analyse précise pour le rugby
        if (containsAny(title, "victoire", "win", "gagne", "remporte", "triomphe")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (containsAny(title, "coupe", "world", "champion", "six")) {
            return "https://images.unsplash.com/photo-1506197600528-5b297d02b8db?w=400&h=200&fit=crop"; // Trophée
        } else if (containsAny(title, "match", "game", "rencontre", "opposition")) {
            return "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=400&h=200&fit=crop"; // Rugby
        } else if (containsAny(title, "essai", "try", "marque", "score")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Action
        } else {
            return "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=400&h=200&fit=crop"; // Rugby par défaut
        }
    }
    
    private String getF1ImageUrl(Long newsId, String title, String content, String fullText) {
        // Analyse précise pour la F1
        if (containsAny(title, "victoire", "win", "gagne", "remporte", "triomphe")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (containsAny(title, "hamilton", "verstappen", "leclerc", "senna")) {
            return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Pilote
        } else if (containsAny(title, "grand", "prix", "gp", "monaco", "monza")) {
            return "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=400&h=200&fit=crop"; // Course
        } else if (containsAny(title, "voiture", "car", "monoplace", "ferrari", "mercedes")) {
            return "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=400&h=200&fit=crop"; // Voiture
        } else if (containsAny(title, "qualif", "pole", "grille", "depart")) {
            return "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=400&h=200&fit=crop"; // Course
        } else {
            return "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=400&h=200&fit=crop"; // Course par défaut
        }
    }
    
    private String getSwimmingImageUrl(Long newsId, String title, String content, String fullText) {
        // Analyse précise pour la natation
        if (containsAny(title, "victoire", "win", "gagne", "remporte", "medaille")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (containsAny(title, "record", "monde", "olympique", "jeux")) {
            return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Nageur
        } else if (containsAny(title, "phelps", "manaudou", "ledecky", "sjoestroem")) {
            return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Nageur célèbre
        } else if (containsAny(title, "nage", "swim", "papillon", "brasse", "dos")) {
            return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Natation
        } else {
            return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Natation par défaut
        }
    }
    
    private String getAthleticsImageUrl(Long newsId, String title, String content, String fullText) {
        // Analyse précise pour l'athlétisme
        if (containsAny(title, "victoire", "win", "gagne", "remporte", "medaille")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (containsAny(title, "record", "monde", "olympique", "jeux")) {
            return "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&h=200&fit=crop"; // Athlétisme
        } else if (containsAny(title, "bolt", "ussain", "el", "farah", "cheptegei")) {
            return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Athlète célèbre
        } else if (containsAny(title, "100m", "200m", "marathon", "course", "sprint")) {
            return "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&h=200&fit=crop"; // Course
        } else if (containsAny(title, "saut", "jump", "hauteur", "longueur")) {
            return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Saut
        } else {
            return "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&h=200&fit=crop"; // Athlétisme par défaut
        }
    }
    
    private String getJudoImageUrl(Long newsId, String title, String content, String fullText) {
        // Analyse précise pour le judo
        if (containsAny(title, "victoire", "win", "gagne", "remporte", "medaille")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (containsAny(title, "champion", "monde", "olympique", "jeux")) {
            return "https://images.unsplash.com/photo-1506197600528-5b297d02b8db?w=400&h=200&fit=crop"; // Trophée
        } else if (containsAny(title, "rivaux", "combat", "match", "opposition")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Combat
        } else if (containsAny(title, "kimono", "ceinture", "grade", "technique")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Judo
        } else {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Judo par défaut
        }
    }
    
    private String getCyclingImageUrl(Long newsId, String title, String content, String fullText) {
        // Analyse précise pour le cyclisme - CORRIGÉ
        if (containsAny(title, "victoire", "win", "gagne", "remporte", "triomphe")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (containsAny(title, "tour", "france", "giro", "vuelta", "grand", "boucle")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Cyclisme
        } else if (containsAny(title, "pogacar", "vinge", "evenepoel", "roglic")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Cycliste célèbre
        } else if (containsAny(title, "course", "race", "etape", "contre-la-montre")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Course cycliste
        } else if (containsAny(title, "velo", "bike", "bicyclette", "cycliste")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Vélo
        } else {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Cyclisme par défaut
        }
    }
    
    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private String getRandomSportsImage(String category, Long newsId) {
        // Bibliothèque d'images sportives réelles et pertinentes
        String[] sportsImages = {
            "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=200&fit=crop", // Football stadium
            "https://images.unsplash.com/photo-1554068393-7a56f670c326?w=400&h=200&fit=crop", // Basketball
            "https://images.unsplash.com/photo-1595435934249-5e7e2a0b5d98?w=400&h=200&fit=crop", // Tennis
            "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=400&h=200&fit=crop", // Racing
            "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop", // Swimming
            "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&h=200&fit=crop", // Athletics
            "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=400&h=200&fit=crop", // Rugby
            "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop", // Cycling
            "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop", // Victory
            "https://images.unsplash.com/photo-1506197600528-5b297d02b8db?w=400&h=200&fit=crop", // Trophy
            "https://images.unsplash.com/photo-1579952363873-27d3bf5e9b81?w=400&h=200&fit=crop", // Team
            "https://images.unsplash.com/photo-1554068393-7a56f670c326?w=400&h=200&fit=crop", // Basketball court
            "https://images.unsplash.com/photo-1431329165308-7679c4105f9e?w=400&h=200&fit=crop", // Competition
            "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop", // Sport action
            "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop", // Athlete
            "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=400&h=200&fit=crop", // Racing car
            "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&h=200&fit=crop", // Track
            "https://images.unsplash.com/photo-1552667466-07770ae110b0?w=400&h=200&fit=crop", // Sport event
            "https://images.unsplash.com/photo-1506197600528-5b297d02b8db?w=400&h=200&fit=crop", // Championship
            "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=200&fit=crop"  // Stadium
        };
        
        // Sélectionner une image pertinente selon la catégorie
        int imageIndex;
        switch (category) {
            case "football-victory":
            case "basketball-championship":
            case "tennis-trophy":
            case "rugby-victory":
            case "f1-podium":
            case "swimming-medal":
            case "athletics-victory":
            case "judo-medal":
            case "cycling-victory":
                imageIndex = (int)((newsId % 3) + 8); // Images de victoire (indices 8, 9, 10)
                break;
            case "football-trophy":
            case "tennis-tournament":
            case "athletics-record":
                imageIndex = (int)((newsId % 2) + 18); // Images de championnat (indices 18, 19)
                break;
            case "football-match":
            case "tennis-match":
            case "rugby-match":
            case "f1-racing":
            case "cycling-race":
                imageIndex = (int)((newsId % 4) + 3); // Images de match/course (indices 3, 4, 5, 6)
                break;
            case "football-stadium":
                imageIndex = (int)((newsId % 2) + 19); // Images de stade (indices 19, 0)
                break;
            case "basketball-player":
            case "swimming-record":
            case "judo-competition":
            case "cycling-event":
                imageIndex = (int)((newsId % 3) + 14); // Images d'athlète/compétition (indices 14, 15, 16)
                break;
            case "f1-car":
                imageIndex = 16; // Image de voiture de course
                break;
            case "athletics-track":
                imageIndex = 6; // Image de piste d'athlétisme
                break;
            default:
                imageIndex = (int)(newsId % sportsImages.length);
                break;
        }
        
        return sportsImages[imageIndex];
    }
    
    private String getContentBasedImageUrl(Long newsId, String title, String content) {
        // Extraire les mots-clés du titre et du contenu
        String keywords = extractContentKeywords(title, content);
        String fullText = (title + " " + content).toLowerCase();
        
        // Détection prioritaire des sports spécifiques pour éviter les images inappropriées
        if (containsAny(fullText, "football", "soccer", "ballon", "but", "goal", "ligue des champions", "champions league")) {
            return "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=200&fit=crop"; // Football
        } else if (containsAny(fullText, "basketball", "basket", "panier", "dunk", "nba")) {
            return "https://images.unsplash.com/photo-1554068393-7a56f670c326?w=400&h=200&fit=crop"; // Basketball
        } else if (containsAny(fullText, "tennis", "tennisman", "raquette", "wimbledon", "roland")) {
            return "https://images.unsplash.com/photo-1595435934249-5e7e2a0b5d98?w=400&h=200&fit=crop"; // Tennis
        } else if (containsAny(fullText, "rugby", "rugbyman", "essai", "scrum", "coupe du monde")) {
            return "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=400&h=200&fit=crop"; // Rugby
        } else if (containsAny(fullText, "formule 1", "f1", "course voiture", "grand prix", "monaco")) {
            return "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=400&h=200&fit=crop"; // F1
        } else if (containsAny(fullText, "cyclisme", "velo", "bike", "tour de france", "pogacar")) {
            return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Cyclisme
        } else if (containsAny(fullText, "natation", "swimming", "nage", "piscine", "olympique")) {
            return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Natation
        } else if (containsAny(fullText, "athlétisme", "athletics", "course", "marathon", "saut")) {
            return "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&h=200&fit=crop"; // Athlétisme
        } else if (containsAny(fullText, "judo", "judo", "combat", "kimono", "medaille")) {
            return "https://images.unsplash.com/photo-1506197600528-5b297d02b8db?w=400&h=200&fit=crop"; // Judo
        } else if (keywords.contains("victoire") || keywords.contains("win") || keywords.contains("champion")) {
            return "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=400&h=200&fit=crop"; // Célébration
        } else if (keywords.contains("record") || keywords.contains("performance")) {
            return "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&h=200&fit=crop"; // Performance
        } else if (keywords.contains("match") || keywords.contains("game") || keywords.contains("competition")) {
            return "https://images.unsplash.com/photo-1431329165308-7679c4105f9e?w=400&h=200&fit=crop"; // Competition
        } else if (keywords.contains("team") || keywords.contains("équipe")) {
            return "https://images.unsplash.com/photo-1579952363873-27d3bf5e9b81?w=400&h=200&fit=crop"; // Team
        } else if (keywords.contains("player") || keywords.contains("joueur") || keywords.contains("athlete")) {
            return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Athlete
        } else {
            return "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=200&fit=crop"; // Sports par défaut
        }
    }
    
    private String extractContentKeywords(String title, String content) {
        if (title == null) title = "";
        if (content == null) content = "";
        
        String combinedText = (title + " " + content).toLowerCase();
        
        // Mots-clés français et anglais pour le sport
        String[] frenchKeywords = {
            "victoire", "champion", "match", "équipe", "joueur", "record", 
            "performance", "compétition", "tournoi", "medaille", "course", "win"
        };
        
        String[] englishKeywords = {
            "victory", "win", "champion", "match", "team", "player", "record",
            "performance", "competition", "tournament", "medal", "race", "game"
        };
        
        StringBuilder foundKeywords = new StringBuilder();
        
        // Rechercher les mots-clés français
        for (String keyword : frenchKeywords) {
            if (combinedText.contains(keyword)) {
                foundKeywords.append(keyword).append(" ");
            }
        }
        
        // Rechercher les mots-clés anglais
        for (String keyword : englishKeywords) {
            if (combinedText.contains(keyword)) {
                foundKeywords.append(keyword).append(" ");
            }
        }
        
        return foundKeywords.toString().trim();
    }
    
        
    @GetMapping("/enhanced")
    public ResponseEntity<Page<NewsResponse>> getEnhancedNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsService.getAllNewsPaginated(pageable);
        
        // Filtrer les articles pour n'afficher que ceux avec des images et liens valides
        // Utiliser un Set pour garantir l'unicité (pas de doublons)
        java.util.Set<String> seenTitles = new java.util.HashSet<>();
        List<News> filteredNews = newsPage.getContent().stream()
                .filter(news -> hasValidImageAndLink(news))
                .filter(news -> {
                    String title = news.getTitle();
                    if (title == null) return false;
                    
                    // Normaliser le titre pour éviter les doublons avec des variations mineures
                    String normalizedTitle = title.trim().toLowerCase();
                    
                    // Créer une clé de détection plus robuste (premiers mots clés)
                    String detectionKey = extractDetectionKey(normalizedTitle);
                    
                    // Condition stricte : si la clé de détection a déjà été vue, ne pas afficher l'article
                    if (seenTitles.contains(detectionKey)) {
                        logger.warn("Doublon détecté et filtré (clé: {}) : {}", detectionKey, title);
                        return false;
                    }
                    
                    // Ajouter la clé de détection aux clés vues
                    seenTitles.add(detectionKey);
                    return true;
                })
                .collect(Collectors.toList());
        
        // Créer une nouvelle page avec les articles filtrés
        int start = page * size;
        int end = Math.min(start + size, filteredNews.size());
        List<News> pageContent = filteredNews.subList(start, end);
        
        Page<News> filteredNewsPage = new PageImpl<>(pageContent, pageable, filteredNews.size());
        
        // Enrichir les actualités avec des URLs d'accès complet et images intelligentes
        Page<NewsResponse> enrichedNews = filteredNewsPage.map(news -> {
            NewsResponse response = toResponse(news);
            // Ajouter l'URL pour accéder au contenu complet
            response.setFullArticleUrl("/api/news/" + news.getId() + "/full");
            response.setContentUrl("/api/news/" + news.getId() + "/content");
            
            // Appliquer la détection intelligente d'images
            String sportName = news.getSport() != null ? news.getSport().getName() : null;
            String intelligentImageUrl = getSportSpecificImageUrl(sportName, news.getId(), news.getTitle(), news.getContent());
            
            // Debug: logger l'URL générée
            logger.info("Generated intelligent image URL for news '{}' (sport: '{}'): {}", 
                news.getTitle(), sportName, intelligentImageUrl);
            
            // Ajouter l'URL du proxy pour l'image intelligente
            if (intelligentImageUrl != null && !intelligentImageUrl.isEmpty()) {
                try {
                    String encodedUrl = java.net.URLEncoder.encode(intelligentImageUrl, java.nio.charset.StandardCharsets.UTF_8);
                    response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                } catch (Exception e) {
                    logger.warn("Error encoding intelligent image URL for news {}: {}", news.getId(), intelligentImageUrl, e);
                    // Fallback vers l'image originale si elle existe
                    if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                        try {
                            String originalEncodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                            response.setImageUrl("/api/news/proxy/image?url=" + originalEncodedUrl);
                        } catch (Exception ex) {
                            response.setImageUrl(news.getImageUrl());
                        }
                    }
                }
            } else if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                // Fallback vers l'image originale
                try {
                    String encodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                    response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                } catch (Exception e) {
                    response.setImageUrl(news.getImageUrl());
                }
            }
            
            return response;
        });
        
        return ResponseEntity.ok(enrichedNews);
    }
    
    // Endpoint de test pour le système d'images
    @GetMapping("/debug/test-images")
    public ResponseEntity<Map<String, String>> testImageDetection() {
        Map<String, String> results = new HashMap<>();
        
        // Test 1: Ligue des Champions
        String imageUrl1 = getSportSpecificImageUrl("Football", 1L, "Le club français remporte la Ligue des Champions", "Le club français remporte la Ligue des Champions après une finale épique");
        results.put("ligue_champions", imageUrl1);
        
        // Test 2: Judo
        String imageUrl2 = getSportSpecificImageUrl("Judo", 2L, "Nouveau champion du monde de judo", "Le judoka français remporte le championnat du monde");
        results.put("judo", imageUrl2);
        
        // Test 3: Formule 1
        String imageUrl3 = getSportSpecificImageUrl("Formule 1", 3L, "Victoire en Formule 1", "Le pilote remporte le Grand Prix de Monaco");
        results.put("formule1", imageUrl3);
        
        // Test 4: Détection par contenu
        String imageUrl4 = getContentBasedImageUrl(4L, "Le club français gagne la Ligue des Champions", "Victoire historique en football européen");
        results.put("detection_contenu", imageUrl4);
        
        return ResponseEntity.ok(results);
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
        response.setArticleUrl(news.getArticleUrl());
        response.setPublishedAt(news.getPublishedAt());
        
        // Utiliser d'abord l'image originale de l'article, puis fallback si nécessaire
        if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
            try {
                String encodedUrl = java.net.URLEncoder.encode(news.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                response.setImageUrl("/api/news/proxy/image?url=" + encodedUrl);
                logger.info("Using original image for news {}: {}", news.getId(), news.getImageUrl());
            } catch (Exception e) {
                logger.warn("Error encoding original image URL for news {}: {}, using fallback", news.getId(), news.getImageUrl(), e);
                response.setImageUrl(getFallbackImageBySport(news.getSport(), news.getId()));
            }
        } else {
            // Aucune image originale, utiliser une image Unsplash pertinente par discipline
            logger.info("No original image for news {}, using fallback by sport", news.getId());
            response.setImageUrl(getFallbackImageBySport(news.getSport(), news.getId()));
        }
        
        // Ajouter les informations sur le sport, la compétition et l'équipe
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
    
    private String getFallbackImageBySport(Sport sport, Long newsId) {
        // Images Unsplash pertinentes par discipline sportive
        if (sport == null || sport.getName() == null) {
            return getDefaultSportsImage(newsId);
        }
        
        String sportName = sport.getName().toLowerCase();
        
        switch (sportName) {
            case "football":
            case "soccer":
                return "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=200&fit=crop"; // Football stadium
                
            case "basketball":
                return "https://images.unsplash.com/photo-1554068393-7a56f670c326?w=400&h=200&fit=crop"; // Basketball
                
            case "tennis":
                return "https://images.unsplash.com/photo-1595435934249-5e7e2a0b5d98?w=400&h=200&fit=crop"; // Tennis
                
            case "rugby":
                return "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=400&h=200&fit=crop"; // Rugby
                
            case "f1":
            case "formula 1":
                return "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=400&h=200&fit=crop"; // Racing
                
            case "swimming":
                return "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=400&h=200&fit=crop"; // Swimming
                
            case "athletics":
            case "athlétisme":
                return "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&h=200&fit=crop"; // Athletics
                
            case "judo":
                return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Martial arts
                
            case "cycling":
                return "https://images.unsplash.com/photo-1540555700478-4be2892cef28?w=400&h=200&fit=crop"; // Cycling
                
            default:
                return getDefaultSportsImage(newsId);
        }
    }
    
    private String getDefaultSportsImage(Long newsId) {
        // Images sportives génériques pour les sports non spécifiés
        String[] defaultImages = {
            "https://images.unsplash.com/photo-1431329165308-7679c4105f9e?w=400&h=200&fit=crop", // Sport action
            "https://images.unsplash.com/photo-1579952363873-27d3bf5e9b81?w=400&h=200&fit=crop", // Team
            "https://images.unsplash.com/photo-1552667466-07770ae110b0?w=400&h=200&fit=crop", // Sport event
            "https://images.unsplash.com/photo-1506197600528-5b297d02b8db?w=400&h=200&fit=crop"  // Trophy
        };
        
        int index = (int)(newsId % defaultImages.length);
        return defaultImages[index];
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
