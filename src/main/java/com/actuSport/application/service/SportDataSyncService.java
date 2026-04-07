package com.actuSport.application.service;

import com.actuSport.domain.model.Match;
import com.actuSport.domain.model.News;
import com.actuSport.infrastructure.external.ApiSportsClient;
import com.actuSport.infrastructure.external.NewsApiClient;
import com.actuSport.infrastructure.external.MockNewsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class SportDataSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SportDataSyncService.class);
    
    private final ApiSportsClient apiSportsClient;
    private final NewsApiClient newsApiClient;
    private final MockNewsClient mockNewsClient;
    private final MatchService matchService;
    private final NewsService newsService;
    
    private final List<String> SUPPORTED_SPORTS = Arrays.asList(
        "soccer", "basketball", "tennis", "hockey", 
        "rugby", "cycling", "f1", "judo", "swimming"
    );
    
    public SportDataSyncService(ApiSportsClient apiSportsClient, NewsApiClient newsApiClient,
                               MockNewsClient mockNewsClient, MatchService matchService, NewsService newsService) {
        this.apiSportsClient = apiSportsClient;
        this.newsApiClient = newsApiClient;
        this.mockNewsClient = mockNewsClient;
        this.matchService = matchService;
        this.newsService = newsService;
    }
    
    @Scheduled(fixedRate = 600000) // Toutes les 10 minutes
    public void syncLiveMatches() {
        SUPPORTED_SPORTS.parallelStream().forEach(sport -> {
            try {
                syncLiveMatchesForSport(sport);
            } catch (Exception e) {
                logger.error("Error syncing live matches for sport: {}", sport, e);
            }
        });
    }
    
    @Scheduled(fixedRate = 3600000) // Toutes les heures
    public void syncNews() {
        SUPPORTED_SPORTS.parallelStream().forEach(sport -> {
            try {
                syncNewsForSport(sport);
            } catch (Exception e) {
                logger.error("Error syncing news for sport: {}", sport, e);
            }
        });
    }
    
    public void syncLiveMatchesForSport(String sport) {
        List<Match> apiSportsMatches = apiSportsClient.getLiveMatches(sport);
        matchService.updateMatches(apiSportsMatches);
        logger.info("Synced {} live matches for sport: {}", apiSportsMatches.size(), sport);
    }

    public void syncNewsForSport(String sport) {
        // Utilisation de NewsAPI pour les vraies actualités sportives
        List<News> news = newsApiClient.getSportsNews(sport);
        
        if (news != null && !news.isEmpty()) {
            newsService.updateNews(news);
            logger.info("Synced {} news articles from NewsAPI for sport: {}", news.size(), sport);
        } else {
            logger.warn("No news found on NewsAPI for sport: {}", sport);
        }
    }
    
    public void createRecentTestArticles() {
        logger.info("Creating recent test articles for demonstration");
        
        // Créer quelques articles récents avec des dates actuelles
        List<News> recentArticles = Arrays.asList(
            createRecentArticle(
                "Victoire historique en Ligue des Champions",
                "Le club français remporte la Ligue des Champions après une finale épique contre les géants européens. Une performance légendaire qui marquera l'histoire.",
                "Un club français triomphe en Ligue des Champions dans un match mémorable.",
                "Rédaction SportActual",
                "L'Équipe",
                "https://via.placeholder.com/400x200/059669/FFFFFF?text=Champions+League",
                "https://example.com/news/champions-league-victory"
            ),
            createRecentArticle(
                "Le tennisman français remporte Wimbledon",
                "Dans une finale spectaculaire, le joueur français s'impose en 5 sets et devient le premier français à remporter Wimbledon depuis 40 ans.",
                "Un triomphe historique pour le tennis français à Wimbledon.",
                "Rédaction SportActual", 
                "Tennis Magazine",
                "https://via.placeholder.com/400x200/10B981/FFFFFF?text=Wimbledon+Victory",
                "https://example.com/news/wimbledon-win"
            ),
            createRecentArticle(
                "Nouveau record du monde du 100m",
                "L'athlète français pulvérise le record du monde du 100m avec un chrono incroyable de 9.58s lors du meeting de Paris.",
                "Un record du monde historique pour l'athlète français.",
                "Rédaction SportActual",
                "Athletics Weekly", 
                "https://via.placeholder.com/400x200/EF4444/FFFFFF?text=World+Record",
                "https://example.com/news/world-record-100m"
            )
        );
        
        // Sauvegarder les articles
        for (News article : recentArticles) {
            try {
                newsService.saveNews(article);
                logger.info("Created recent article: {}", article.getTitle());
            } catch (Exception e) {
                logger.error("Error creating recent article: {}", article.getTitle(), e);
            }
        }
    }
    
    private News createRecentArticle(String title, String content, String summary, 
                                   String author, String source, String imageUrl, String articleUrl) {
        News news = new News();
        news.setTitle(title);
        news.setContent(content);
        news.setSummary(summary);
        news.setAuthor(author);
        news.setSource(source);
        news.setImageUrl(imageUrl);
        news.setArticleUrl(articleUrl);
        news.setPublishedAt(LocalDateTime.now()); // Date actuelle
        news.setCreatedAt(LocalDateTime.now());
        return news;
    }
}
