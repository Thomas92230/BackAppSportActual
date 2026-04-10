package com.actuSport.infrastructure.external;

import com.actuSport.domain.model.News;
import com.actuSport.domain.model.Sport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MockNewsClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MockNewsClient.class);
    
    private final Random random = new Random();
    
    // Données de démonstration pour différents sports
    private static final List<MockNewsData> MOCK_NEWS = List.of(
        new MockNewsData("Victoire écrasante de l'équipe de France", 
                        "L'équipe de France remporte une victoire impressionnante contre son rival...", 
                        "football", "Reuters"),
        new MockNewsData("Nouveau record du monde en athlétisme", 
                        "Un athlète français bat le record du monde du 100m avec un temps incroyable...", 
                        "athletics", "ESPN"),
        new MockNewsData("Le tennisman français atteint la finale", 
                        "Après un match marathon, le joueur français se qualifie pour la finale du tournoi...", 
                        "tennis", "Tennis Magazine"),
        new MockNewsData("Transfert majeur dans le football", 
                        "Un club de premier plan annonce le transfert record d'une star internationale...", 
                        "football", "Sky Sports"),
        new MockNewsData("Le cycliste français remporte le Tour", 
                        "Une performance historique dans les Alpes lui permet de remporter le maillot jaune...", 
                        "cycling", "L'Équipe"),
        new MockNewsData("L'équipe de basket-ball se qualifie", 
                        "Victoire décisive pour l'équipe nationale qui se qualifie pour les prochains championnats...", 
                        "basketball", "Basket News"),
        new MockNewsData("Nouveau champion du monde de judo", 
                        "Le judoka français devient champion du monde après une finale spectaculaire...", 
                        "judo", "Judo Inside"),
        new MockNewsData("Record du monde en natation", 
                        "Une nageuse française bat le record du monde du 100m nage libre...", 
                        "swimming", "Swimming World"),
        new MockNewsData("Victoire en Formule 1", 
                        "Le pilote français remporte le Grand Prix dans une course dramatique...", 
                        "f1", "F1 News"),
        new MockNewsData("Le rugbyman français signe un nouveau contrat", 
                        "Le star du rugby français prolonge son contrat pour trois saisons supplémentaires...", 
                        "rugby", "Rugby Pass")
    );
    
    public List<News> getSportsNews(String sport) {
        logger.info("Générant des actualités de démonstration pour le sport: {}", sport);
        
        List<News> newsList = new ArrayList<>();
        
        // Filtrer et générer des actualités pour le sport demandé
        List<MockNewsData> filteredNews = MOCK_NEWS.stream()
                .filter(news -> news.sport().equals(sport) || news.sport().equals("football"))
                .limit(5)
                .toList();
        
        for (MockNewsData mockNews : filteredNews) {
            News news = convertToNews(mockNews, sport);
            newsList.add(news);
        }
        
        logger.info("Généré {} actualités de démonstration pour le sport: {}", newsList.size(), sport);
        return newsList;
    }
    
    private News convertToNews(MockNewsData mockNews, String sport) {
        News news = new News();
        
        news.setTitle(mockNews.title());
        news.setContent(mockNews.content() + " Cet article a été généré automatiquement à des fins de démonstration. " +
                      "Il sera remplacé par de vraies actualités lorsque la clé NewsAPI sera configurée.");
        news.setSummary(mockNews.content());
        news.setAuthor("Rédaction SportActual");
        news.setSource(mockNews.source());
        // Générer une URL d'image plus fiable avec Picsum Photos
        String seed = mockNews.title().toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .trim();
        news.setImageUrl("https://picsum.photos/seed/" + seed + "/400/200.jpg");
        news.setArticleUrl("https://example.com/news/" + System.currentTimeMillis());
        news.setPublishedAt(LocalDateTime.now().minusHours(random.nextInt(24)));
        news.setCreatedAt(LocalDateTime.now());
        
        // Ne pas créer d'objet Sport transient pour éviter l'erreur Hibernate
        // Le sport sera associé via le service ou sera null pour l'instant
        news.setSport(null);
        
        return news;
    }
    
    private record MockNewsData(String title, String content, String sport, String source) {}
}
