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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

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
    private final Environment environment;
    
    private final List<String> SUPPORTED_SPORTS = Arrays.asList(
        "soccer", "basketball", "tennis", "hockey", 
        "rugby", "cycling", "f1", "judo", "swimming"
    );
    
    public SportDataSyncService(ApiSportsClient apiSportsClient, NewsApiClient newsApiClient,
                               MockNewsClient mockNewsClient, MatchService matchService, NewsService newsService, 
                               Environment environment) {
        this.apiSportsClient = apiSportsClient;
        this.newsApiClient = newsApiClient;
        this.mockNewsClient = mockNewsClient;
        this.matchService = matchService;
        this.newsService = newsService;
        this.environment = environment;
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
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeDataOnStartup() {
        logger.info("Application démarrée - Initialisation des données...");
        
        // Vérifier si nous sommes en mode développement/démonstration
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevMode = Arrays.asList(activeProfiles).contains("dev") || 
                          Arrays.asList(activeProfiles).contains("demo") ||
                          Arrays.asList(activeProfiles).contains("docker");
        
        if (isDevMode) {
            logger.info("Mode développement détecté - Création des articles de démonstration...");
            try {
                // Vérifier s'il y a déjà des articles dans la base
                long existingNewsCount = newsService.getAllNews().size();
                
                if (existingNewsCount == 0) {
                    logger.info("Aucun article trouvé - Création des articles de démonstration avec images valides");
                    createRecentTestArticles();
                    
                    // Ajouter quelques articles sportifs supplémentaires avec des images Picsum
                    createSportsDemoArticles();
                    
                    logger.info("Initialisation terminée - Articles de démonstration créés");
                } else {
                    logger.info("Articles existants trouvés ({}) - Initialisation des données non nécessaire", existingNewsCount);
                }
            } catch (Exception e) {
                logger.error("Erreur lors de l'initialisation des données", e);
            }
        } else {
            logger.info("Mode production - Pas d'initialisation automatique des données de démonstration");
        }
    }
    
    private void createSportsDemoArticles() {
        logger.info("Création d'articles sportifs de démonstration avec URLs Picsum");
        
        List<News> sportsArticles = Arrays.asList(
            createRecentArticle(
                "Victoire écrasante de l'équipe de France",
                "L'équipe de France remporte une victoire impressionnante contre son rival dans un match mémorable. Les joueurs ont montré une excellente coordination et ont dominé tout le match.",
                "L'équipe de France s'impose avec brio et consolide sa première place.",
                "Rédaction SportActual",
                "Reuters",
                "https://picsum.photos/seed/france-victory/400/200.jpg",
                "https://example.com/news/france-victory"
            ),
            createRecentArticle(
                "Le tennisman français atteint la finale",
                "Après un match marathon de 5 sets, le joueur français se qualifie pour la finale du tournoi Grand Chelem. Il a montré un mental d'acier et a su surmonter la fatigue.",
                "Un joueur français en finale après un match épique.",
                "Rédaction SportActual",
                "Tennis Magazine",
                "https://picsum.photos/seed/tennis-final/400/200.jpg",
                "https://example.com/news/tennis-final"
            ),
            createRecentArticle(
                "Transfert record pour le club parisien",
                "Le club de la capitale annonce le transfert le plus cher de son histoire. Un joueur international rejoint l'équipe pour renforcer leur effectif.",
                "Un transfert historique pour le club parisien.",
                "Rédaction SportActual",
                "L'Équipe",
                "https://picsum.photos/seed/transfer-record/400/200.jpg",
                "https://example.com/news/transfer-record"
            ),
            createRecentArticle(
                "Le cycliste français remporte l'étape",
                "Dans une étape de montagne éprouvante, le cycliste français a montré sa supériorité en attaquant dans les derniers kilomètres.",
                "Une victoire d'étape impressionnante pour le cycliste français.",
                "Rédaction SportActual",
                "Cycling News",
                "https://picsum.photos/seed/cycling-win/400/200.jpg",
                "https://example.com/news/cycling-win"
            ),
            createRecentArticle(
                "Nouveau record du monde en athlétisme",
                "L'athlète français a pulvérisé le record du monde du 100m lors d'un meeting international. Un chrono historique.",
                "Un record du monde historique pour l'athlète français.",
                "Rédaction SportActual",
                "Athletics Weekly",
                "https://picsum.photos/seed/world-record/400/200.jpg",
                "https://example.com/news/world-record"
            ),
            createRecentArticle(
                "Victoire en Formule 1",
                "Le pilote français remporte le Grand Prix dans une course dramatique. Une performance exceptionnelle.",
                "Une victoire spectaculaire en Formule 1.",
                "Rédaction SportActual",
                "F1 News",
                "https://picsum.photos/seed/f1-victory/400/200.jpg",
                "https://example.com/news/f1-victory"
            ),
            createRecentArticle(
                "Nouveau champion du monde de judo",
                "Le judoka français devient champion du monde après une finale spectaculaire. Une victoire qui couronne des années d'efforts.",
                "Le judoka français sacré champion du monde.",
                "Rédaction SportActual",
                "Judo Inside",
                "https://picsum.photos/seed/judo-champion/400/200.jpg",
                "https://example.com/news/judo-champion"
            ),
            createRecentArticle(
                "Le club français remporte la Ligue des Champions",
                "Une victoire historique en finale de la Ligue des Champions. Les joueurs ont montré un jeu exceptionnel tout au long de la compétition.",
                "Une victoire légendaire en Ligue des Champions.",
                "Rédaction SportActual",
                "ESPN",
                "https://picsum.photos/seed/champions-league/400/200.jpg",
                "https://example.com/news/champions-league"
            )
        );
        
        // Sauvegarder les articles un par un pour éviter les erreurs de doublons
        for (News article : sportsArticles) {
            try {
                newsService.saveNews(article);
                logger.info("Article sportif créé: {}", article.getTitle());
            } catch (Exception e) {
                logger.warn("Article déjà existant ou erreur lors de la création: {}", article.getTitle(), e);
            }
        }
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
                "https://picsum.photos/seed/champions-league-victory/400/200.jpg",
                "https://example.com/news/champions-league-victory"
            ),
            createRecentArticle(
                "Le tennisman français remporte Wimbledon",
                "Dans une finale spectaculaire, le joueur français s'impose en 5 sets et devient le premier français à remporter Wimbledon depuis 40 ans.",
                "Un triomphe historique pour le tennis français à Wimbledon.",
                "Rédaction SportActual", 
                "Tennis Magazine",
                "https://picsum.photos/seed/wimbledon-victory/400/200.jpg",
                "https://example.com/news/wimbledon-win"
            ),
            createRecentArticle(
                "Nouveau record du monde du 100m",
                "L'athlète français pulvérise le record du monde du 100m avec un chrono incroyable de 9.58s lors du meeting de Paris.",
                "Un record du monde historique pour l'athlète français.",
                "Rédaction SportActual",
                "Athletics Weekly", 
                "https://picsum.photos/seed/world-record-100m/400/200.jpg",
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
