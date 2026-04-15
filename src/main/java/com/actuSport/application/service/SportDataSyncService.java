package com.actuSport.application.service;

import com.actuSport.domain.model.Match;
import com.actuSport.domain.model.News;
import com.actuSport.infrastructure.external.ApiSportsClient;
import com.actuSport.infrastructure.external.NewsApiClient;
import com.actuSport.infrastructure.external.MockNewsClient;
import com.actuSport.infrastructure.external.MultiSourceNewsClient;
import com.actuSport.infrastructure.external.OptimizedNewsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class SportDataSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SportDataSyncService.class);
    
    private final ApiSportsClient apiSportsClient;
    private final NewsApiClient newsApiClient;
    private final MockNewsClient mockNewsClient;
    private final MultiSourceNewsClient multiSourceNewsClient;
    private final OptimizedNewsClient optimizedNewsClient;
    private final MatchService matchService;
    private final NewsService newsService;
    private final Environment environment;
    
    // Injecter les clés API depuis application.properties / .env
    @Value("${external.api.newsapi.api-key:}")
    private String newsApiKey;
    
    @Value("${external.api.guardian.api-key:}")
    private String guardianApiKey;
    
    @Value("${external.api.worldnews.api-key:}")
    private String worldNewsApiKey;
    
    private final List<String> SUPPORTED_SPORTS = Arrays.asList(
        "soccer", "basketball", "tennis", "hockey", 
        "rugby", "cycling", "f1", "judo", "swimming"
    );
    
    public SportDataSyncService(ApiSportsClient apiSportsClient, NewsApiClient newsApiClient,
                               MockNewsClient mockNewsClient, MultiSourceNewsClient multiSourceNewsClient,
                               OptimizedNewsClient optimizedNewsClient, MatchService matchService, NewsService newsService, 
                               Environment environment) {
        this.apiSportsClient = apiSportsClient;
        this.newsApiClient = newsApiClient;
        this.mockNewsClient = mockNewsClient;
        this.multiSourceNewsClient = multiSourceNewsClient;
        this.optimizedNewsClient = optimizedNewsClient;
        this.matchService = matchService;
        this.newsService = newsService;
        this.environment = environment;
    }
    
    // @Scheduled(fixedRate = 600000) // Toutes les 10 minutes - DÉSACTIVÉ TEMPORAIREMENT
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
        
        try {
            // Vérifier si nous sommes en mode développement/démonstration
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isDevMode = Arrays.asList(activeProfiles).contains("dev") || 
                              Arrays.asList(activeProfiles).contains("demo") ||
                              Arrays.asList(activeProfiles).contains("docker");
            
            if (isDevMode) {
                logger.info("Mode développement détecté - Création des articles de démonstration...");
                
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
            } else {
                logger.info("Mode production - Pas d'initialisation automatique des données de démonstration");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'initialisation des données", e);
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
                "https://www.lequipe.fr/Football/Actualites/1048485-victoire-historique-de-l-equipe-de-france.html"
            ),
            createRecentArticle(
                "Le tennisman français atteint la finale",
                "Après un match marathon de 5 sets, le joueur français se qualifie pour la finale du tournoi Grand Chelem. Il a montré un mental d'acier et a su surmonter la fatigue.",
                "Un joueur français en finale après un match épique.",
                "Rédaction SportActual",
                "Tennis Magazine",
                "https://picsum.photos/seed/tennis-final/400/200.jpg",
                "https://www.fft.fr/actualite/roland-garros/joueur-francais-atteint-finale-grand-chelem.html"
            ),
            createRecentArticle(
                "Transfert record pour le club parisien",
                "Le club de la capitale annonce le transfert le plus cher de son histoire. Un joueur international rejoint l'équipe pour renforcer leur effectif.",
                "Un transfert historique pour le club parisien.",
                "Rédaction SportActual",
                "L'Équipe",
                "https://picsum.photos/seed/transfer-record/400/200.jpg",
                "https://www.lequipe.fr/Football/Transferts/1048487-transfert-record-psg.html"
            ),
            createRecentArticle(
                "Le cycliste français remporte l'étape",
                "Dans une étape de montagne éprouvante, le cycliste français a montré sa supériorité en attaquant dans les derniers kilomètres.",
                "Une victoire d'étape impressionnante pour le cycliste français.",
                "Rédaction SportActual",
                "Cycling News",
                "https://picsum.photos/seed/cycling-win/400/200.jpg",
                "https://www.lequipe.fr/Cyclisme/Actualites/1048486-victoire-etape-tour-de-france.html"
            ),
            createRecentArticle(
                "Nouveau record du monde en athlétisme",
                "L'athlète français a pulvérisé le record du monde du 100m lors d'un meeting international. Un chrono historique.",
                "Un record du monde historique pour l'athlète français.",
                "Rédaction SportActual",
                "Athletics Weekly",
                "https://picsum.photos/seed/world-record/400/200.jpg",
                "https://www.lequipe.fr/Athletisme/Actualites/1048488-record-du-monde-100m.html"
            ),
            createRecentArticle(
                "Victoire en Formule 1",
                "Le pilote français remporte le Grand Prix dans une course dramatique. Une performance exceptionnelle.",
                "Une victoire spectaculaire en Formule 1.",
                "Rédaction SportActual",
                "F1 News",
                "https://picsum.photos/seed/f1-victory/400/200.jpg",
                "https://www.lequipe.fr/Formule-1/Actualites/1048489-victoire-grand-prix-f1.html"
            ),
            createRecentArticle(
                "Nouveau champion du monde de judo",
                "Le judoka français devient champion du monde après une finale spectaculaire. Une victoire qui couronne des années d'efforts.",
                "Le judoka français sacré champion du monde.",
                "Rédaction SportActual",
                "Judo Inside",
                "https://picsum.photos/seed/judo-champion/400/200.jpg",
                "https://www.lequipe.fr/Judo/Actualites/1048490-judoka-francais-champion-monde.html"
            ),
            createRecentArticle(
                "Le club français remporte la Ligue des Champions",
                "Une victoire historique en finale de la Ligue des Champions. Les joueurs ont montré un jeu exceptionnel tout au long de la compétition.",
                "Une victoire légendaire en Ligue des Champions.",
                "Rédaction SportActual",
                "ESPN",
                "https://picsum.photos/seed/champions-league/400/200.jpg",
                "https://www.lequipe.fr/Football/Ligue-des-Champions/Actualites/1048491-victoire-ligue-des-champions.html"
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
    
    @Scheduled(fixedRate = 14400000) // Toutes les 4 heures (batch processing)
    public void syncNews() {
        logger.info("Starting batch news sync - fetching 20 articles per sport from optimized sources");
        
        SUPPORTED_SPORTS.parallelStream().forEach(sport -> {
            try {
                syncNewsBatchForSport(sport);
            } catch (Exception e) {
                logger.error("Error syncing batch news for sport: {}", sport, e);
            }
        });
        
        logger.info("Batch news sync completed");
    }
    
    public void syncLiveMatchesForSport(String sport) {
        List<Match> apiSportsMatches = apiSportsClient.getLiveMatches(sport);
        matchService.updateMatches(apiSportsMatches);
        logger.info("Synced {} live matches for sport: {}", apiSportsMatches.size(), sport);
    }

    public void syncNewsForSport(String sport) {
        // Utilisation du client multi-sources pour éviter le rate limiting
        // Chaque source fournit environ 1/3 des articles demandés
        List<News> news = multiSourceNewsClient.getSportsNewsFromMultipleSources(sport, 30);
        
        if (news != null && !news.isEmpty()) {
            newsService.updateNews(news);
            logger.info("Synced {} news articles from multiple sources for sport: {}", news.size(), sport);
        } else {
            logger.warn("No news found from multiple sources for sport: {}", sport);
        }
    }
    
    /**
     * Synchronisation batch optimisée avec les vraies API gratuites
     * Récupère 20 articles par sport toutes les 4 heures
     */
    public void syncNewsBatchForSport(String sport) {
        // Utilisation du client optimisé avec TheSportsDB, API-Sports, World News API et Google News RSS
        List<News> news = optimizedNewsClient.getSportsNewsBatch(sport);
        
        if (news != null && !news.isEmpty()) {
            newsService.updateNews(news);
            logger.info("Batch synced {} real articles for sport: {} (from optimized sources)", news.size(), sport);
        } else {
            logger.warn("No real articles found from optimized sources for sport: {}", sport);
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
                "https://www.lequipe.fr/Football/Ligue-des-Champions/Actualites/1048492-victoire-historique-ligue-champions.html"
            ),
            createRecentArticle(
                "Le tennisman français remporte Wimbledon",
                "Dans une finale spectaculaire, le joueur français s'impose en 5 sets et devient le premier français à remporter Wimbledon depuis 40 ans.",
                "Un triomphe historique pour le tennis français à Wimbledon.",
                "Rédaction SportActual", 
                "Tennis Magazine",
                "https://picsum.photos/seed/wimbledon-victory/400/200.jpg",
                "https://www.fft.fr/actualite/roland-garros/joueur-francais-remporte-wimbledon.html"
            ),
            createRecentArticle(
                "Nouveau record du monde du 100m",
                "L'athlète français pulvérise le record du monde du 100m avec un chrono incroyable de 9.58s lors du meeting de Paris.",
                "Un record du monde historique pour l'athlète français.",
                "Rédaction SportActual",
                "Athletics Weekly", 
                "https://picsum.photos/seed/world-record-100m/400/200.jpg",
                "https://www.lequipe.fr/Athletisme/Actualites/1048493-record-du-monde-100m-athletisme.html"
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
    
    /**
     * Logique de rotation intelligente des sources API
     * Tente les sources les unes après les autres
     */
    public int syncNewsWithFallback(String sport) {
        int count = 0;
        
        logger.info("Démarrage de la synchronisation intelligente pour le sport: {}", sport);

        // 1. Tenter The Guardian (Source très fiable et gratuite)
        if (guardianApiKey != null && !guardianApiKey.isEmpty()) {
            logger.info("Tentative de synchro via The Guardian pour: {}", sport);
            count = syncFromGuardian(sport);
            if (count > 0) {
                logger.info("The Guardian a fourni {} articles pour {}", count, sport);
                return count;
            }
        }

        // 2. Si The Guardian n'a rien donné, tenter World News API
        if (worldNewsApiKey != null && !worldNewsApiKey.isEmpty()) {
            logger.warn("The Guardian n'a rien renvoyé, tentative via World News API...");
            count = syncFromWorldNews(sport);
            if (count > 0) {
                logger.info("World News API a fourni {} articles pour {}", count, sport);
                return count;
            }
        }

        // 3. Si World News API n'a rien donné, tenter NewsAPI
        if (newsApiKey != null && !newsApiKey.isEmpty()) {
            logger.warn("World News API n'a rien renvoyé, tentative via NewsAPI...");
            count = syncFromNewsAPI(sport);
            if (count > 0) {
                logger.info("NewsAPI a fourni {} articles pour {}", count, sport);
                return count;
            }
        }

        // 4. Tenter ESPN API web publique (gratuite)
        logger.warn("NewsAPI n'a rien renvoyé, tentative via ESPN API web publique...");
        count = syncFromESPN(sport);
        if (count > 0) {
            logger.info("ESPN API a fourni {} articles pour {}", count, sport);
            return count;
        }

        // 5. Fallback vers RSS français
        logger.warn("ESPN n'a rien renvoyé, tentative via RSS médias sportifs français...");
        count = syncFromFrenchRSS(sport);
        if (count > 0) {
            logger.info("RSS français a fourni {} articles pour {}", count, sport);
            return count;
        }

        // 6. Dernier recours : Google News RSS
        logger.warn("RSS français n'a rien renvoyé, tentative via Google News RSS...");
        count = syncFromGoogleNews(sport);
        if (count > 0) {
            logger.info("Google News RSS a fourni {} articles pour {}", count, sport);
            return count;
        }

        logger.error("Toutes les sources ont échoué pour le sport: {}", sport);
        return 0;
    }

    private int syncFromGuardian(String sport) {
        try {
            List<News> articles = optimizedNewsClient.getSportsNewsBatch(sport);
            int savedCount = 0;
            for (News article : articles) {
                if (article.getSource().equals("The Guardian")) {
                    try {
                        newsService.saveNews(article);
                        savedCount++;
                    } catch (Exception e) {
                        logger.warn("Erreur lors de la sauvegarde d'un article Guardian: {}", e.getMessage());
                    }
                }
            }
            return savedCount;
        } catch (Exception e) {
            logger.warn("Erreur lors de la synchronisation Guardian: {}", e.getMessage());
            return 0;
        }
    }

    private int syncFromWorldNews(String sport) {
        try {
            List<News> articles = optimizedNewsClient.getSportsNewsBatch(sport);
            int savedCount = 0;
            for (News article : articles) {
                if (article.getSource().equals("World News API")) {
                    try {
                        newsService.saveNews(article);
                        savedCount++;
                    } catch (Exception e) {
                        logger.warn("Erreur lors de la sauvegarde d'un article World News: {}", e.getMessage());
                    }
                }
            }
            return savedCount;
        } catch (Exception e) {
            logger.warn("Erreur lors de la synchronisation World News API: {}", e.getMessage());
            return 0;
        }
    }

    private int syncFromNewsAPI(String sport) {
        try {
            List<News> articles = optimizedNewsClient.getSportsNewsBatch(sport);
            int savedCount = 0;
            for (News article : articles) {
                if (article.getSource().equals("NewsAPI")) {
                    try {
                        newsService.saveNews(article);
                        savedCount++;
                    } catch (Exception e) {
                        logger.warn("Erreur lors de la sauvegarde d'un article NewsAPI: {}", e.getMessage());
                    }
                }
            }
            return savedCount;
        } catch (Exception e) {
            logger.warn("Erreur lors de la synchronisation NewsAPI: {}", e.getMessage());
            return 0;
        }
    }

    private int syncFromESPN(String sport) {
        try {
            List<News> articles = optimizedNewsClient.getSportsNewsBatch(sport);
            int savedCount = 0;
            for (News article : articles) {
                if (article.getSource().equals("ESPN")) {
                    try {
                        newsService.saveNews(article);
                        savedCount++;
                    } catch (Exception e) {
                        logger.warn("Erreur lors de la sauvegarde d'un article ESPN: {}", e.getMessage());
                    }
                }
            }
            return savedCount;
        } catch (Exception e) {
            logger.warn("Erreur lors de la synchronisation ESPN: {}", e.getMessage());
            return 0;
        }
    }

    private int syncFromFrenchRSS(String sport) {
        try {
            List<News> articles = optimizedNewsClient.getSportsNewsBatch(sport);
            int savedCount = 0;
            for (News article : articles) {
                if (article.getSource().equals("L'Équipe") || article.getSource().equals("RMC Sport") 
                    || article.getSource().equals("France Bleu") || article.getSource().equals("FFT")) {
                    try {
                        newsService.saveNews(article);
                        savedCount++;
                    } catch (Exception e) {
                        logger.warn("Erreur lors de la sauvegarde d'un article RSS français: {}", e.getMessage());
                    }
                }
            }
            return savedCount;
        } catch (Exception e) {
            logger.warn("Erreur lors de la synchronisation RSS français: {}", e.getMessage());
            return 0;
        }
    }

    private int syncFromGoogleNews(String sport) {
        try {
            List<News> articles = optimizedNewsClient.getSportsNewsBatch(sport);
            int savedCount = 0;
            for (News article : articles) {
                if (article.getSource().equals("Google News")) {
                    try {
                        newsService.saveNews(article);
                        savedCount++;
                    } catch (Exception e) {
                        logger.warn("Erreur lors de la sauvegarde d'un article Google News: {}", e.getMessage());
                    }
                }
            }
            return savedCount;
        } catch (Exception e) {
            logger.warn("Erreur lors de la synchronisation Google News: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Synchronisation automatique planifiée - Le Graal !
     */
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures à la minute 0
    public void scheduledSmartSync() {
        logger.info("Démarrage de la synchronisation automatique planifiée");
        String[] sports = {"soccer", "basketball", "tennis", "hockey", "rugby", "cycling", "f1", "judo", "swimming"};
        for (String sport : sports) {
            try {
                int count = syncNewsWithFallback(sport);
                logger.info("Sync automatique terminée pour {}: {} articles", sport, count);
            } catch (Exception e) {
                logger.error("Erreur lors du sync automatique pour {}: {}", sport, e.getMessage());
            }
        }
        logger.info("Synchronisation automatique planifiée terminée");
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
