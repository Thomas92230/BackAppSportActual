package com.actuSport.application.service;

import com.actuSport.domain.model.News;
import com.actuSport.domain.model.Sport;
import com.actuSport.domain.model.Team;
import com.actuSport.domain.repository.NewsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class NewsService {
    
    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);
    
    @Autowired
    private NewsRepository newsRepository;
    
    @Autowired
    private SportService sportService;
    
    @Autowired
    private TeamService teamService;
    
    public List<News> getAllNews() {
        return newsRepository.findAll();
    }
    
    public Optional<News> getNewsById(Long id) {
        return newsRepository.findById(id);
    }
    
    public List<News> getNewsBySport(String sportCode) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(newsRepository::findBySportOrderByPublishedAtDesc).orElse(List.of());
    }
    
    public List<News> getNewsByTeam(Long teamId) {
        Optional<Team> team = teamService.findById(teamId);
        return team.map(newsRepository::findByTeam).orElse(List.of());
    }
    
    public List<News> getRecentNews(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return newsRepository.findRecentNews(cutoff);
    }
    
    public List<News> searchNews(String keyword) {
        return newsRepository.findByKeyword(keyword);
    }
    
    public List<News> searchNewsBySport(String sportCode, String keyword) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(s -> newsRepository.findBySportAndKeyword(s, keyword)).orElse(List.of());
    }
    
    public Page<News> getNewsBySportPaginated(String sportCode, Pageable pageable) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(s -> newsRepository.findBySportOrderByPublishedAtDesc(s, pageable))
                   .orElse(Page.empty(pageable));
    }
    
    public Page<News> getAllNewsPaginated(Pageable pageable) {
        return newsRepository.findAllByOrderByPublishedAtDesc(pageable);
    }
    
    public News saveNews(News news) {
        if (news.getCreatedAt() == null) {
            news.setCreatedAt(LocalDateTime.now());
        }
        news.setUpdatedAt(LocalDateTime.now());
        
        return newsRepository.save(news);
    }
    
    public void updateNews(List<News> newsList) {
        newsList.forEach(news -> {
            try {
                List<News> existingNews = newsRepository.findAllByTitle(news.getTitle());
                
                if (existingNews.isEmpty()) {
                    saveNews(news);
                    logger.info("Saved new news: {}", news.getTitle());
                } else {
                    logger.debug("News already exists: {} (found {} duplicates)", news.getTitle(), existingNews.size());
                }
            } catch (Exception e) {
                logger.error("Error checking existing news for title: {}", news.getTitle(), e);
                // En cas d'erreur, on essaie quand même de sauvegarder
                saveNews(news);
            }
        });
    }
    
    public News updateNews(Long id, News newsDetails) {
        Optional<News> newsOpt = newsRepository.findById(id);
        if (newsOpt.isPresent()) {
            News news = newsOpt.get();
            news.setTitle(newsDetails.getTitle());
            news.setContent(newsDetails.getContent());
            news.setSummary(newsDetails.getSummary());
            news.setAuthor(newsDetails.getAuthor());
            news.setSource(newsDetails.getSource());
            news.setImageUrl(newsDetails.getImageUrl());
            news.setArticleUrl(newsDetails.getArticleUrl());
            news.setUpdatedAt(LocalDateTime.now());
            
            return newsRepository.save(news);
        }
        return null;
    }
    
    public List<News> getNewsBySource(String source) {
        return newsRepository.findBySource(source);
    }
    
    public List<News> getNewsByDateRange(String sportCode, LocalDateTime startDate, LocalDateTime endDate) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(s -> newsRepository.findBySportAndDateRange(s, startDate, endDate))
                  .orElse(List.of());
    }
    
    public List<News> advancedSearch(String keyword, String source, String author, String sportCode, String sortBy) {
        List<News> results = newsRepository.findAll();
        
        // Filtrer par mot-clé
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            results = results.stream()
                    .filter(news -> 
                        (news.getTitle() != null && news.getTitle().toLowerCase().contains(lowerKeyword)) ||
                        (news.getContent() != null && news.getContent().toLowerCase().contains(lowerKeyword)) ||
                        (news.getSummary() != null && news.getSummary().toLowerCase().contains(lowerKeyword)))
                    .collect(Collectors.toList());
        }
        
        // Filtrer par source
        if (source != null && !source.trim().isEmpty()) {
            results = results.stream()
                    .filter(news -> news.getSource() != null && news.getSource().toLowerCase().contains(source.toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Filtrer par auteur
        if (author != null && !author.trim().isEmpty()) {
            results = results.stream()
                    .filter(news -> news.getAuthor() != null && news.getAuthor().toLowerCase().contains(author.toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Filtrer par sport
        if (sportCode != null && !sportCode.trim().isEmpty()) {
            Optional<Sport> sport = sportService.findByCode(sportCode);
            if (sport.isPresent()) {
                results = results.stream()
                        .filter(news -> sport.equals(news.getSport()))
                        .collect(Collectors.toList());
            }
        }
        
        // Trier les résultats
        switch (sortBy) {
            case "publishedAt":
                results = results.stream()
                        .sorted((a, b) -> b.getPublishedAt().compareTo(a.getPublishedAt()))
                        .collect(Collectors.toList());
                break;
            case "title":
                results = results.stream()
                        .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                        .collect(Collectors.toList());
                break;
            case "source":
                results = results.stream()
                        .sorted((a, b) -> a.getSource().compareToIgnoreCase(b.getSource()))
                        .collect(Collectors.toList());
                break;
            default:
                // Garder l'ordre par défaut (publishedAt)
                break;
        }
        
        logger.info("Advanced search returned {} results (keyword={}, source={}, author={}, sport={}, sortBy={})", 
                   results.size(), keyword, source, author, sportCode, sortBy);
        return results;
    }
    
    public void deleteNews(Long id) {
        newsRepository.deleteById(id);
    }
    
    public void deleteAllNews() {
        logger.info("Deleting all news articles from database");
        newsRepository.deleteAll();
    }
    
    public List<News> getRecentValidSportsNews(int limit) {
        List<News> allRecentNews = getRecentNews(72); // Articles des 3 derniers jours
        
        // Filtrer les articles qui ont une image valide et un lien valide
        List<News> validNews = allRecentNews.stream()
                .filter(this::hasValidImageAndLink)
                .filter(news -> isSportsRelated(news))
                .collect(Collectors.toList());
        
        // Limiter au nombre demandé et trier par date de publication
        return validNews.stream()
                .sorted((a, b) -> b.getPublishedAt().compareTo(a.getPublishedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private boolean hasValidImageAndLink(News news) {
        // Vérifier que l'article a une image valide (vraie image de NewsAPI)
        boolean hasValidImage = news.getImageUrl() != null && 
                               !news.getImageUrl().trim().isEmpty() &&
                               !isExampleUrl(news.getImageUrl()) &&
                               !isGeneratedImage(news.getImageUrl()) &&
                               !isSubstitutionImage(news.getImageUrl());
        
        // Vérifier que l'article a un lien valide
        boolean hasValidLink = news.getArticleUrl() != null && 
                              !news.getArticleUrl().trim().isEmpty() &&
                              !isExampleUrl(news.getArticleUrl());
        
        // Vérifier que l'article a un contenu et titre valides
        boolean hasValidContent = news.getTitle() != null && 
                                !news.getTitle().trim().isEmpty() &&
                                news.getContent() != null && 
                                !news.getContent().trim().isEmpty();
        
        return hasValidImage && hasValidLink && hasValidContent;
    }
    
    private boolean isExampleUrl(String url) {
        if (url == null) return false;
        return url.contains("example.com") || 
               url.contains("exemple") || 
               url.startsWith("http://example.") ||
               url.startsWith("https://example.") ||
               url.contains("placeholder") ||
               url.contains("test.") ||
               url.contains("demo.") ||
               url.contains("picsum.photos") ||
               url.contains("via.placeholder.com");
    }
    
    private boolean isGeneratedImage(String imageUrl) {
        if (imageUrl == null) return false;
        return imageUrl.contains("unsplash.com") && 
               (imageUrl.contains("photo-1571019613454-1cb2f99b2d8b") ||
                imageUrl.contains("photo-1595435934249-5e7e2a0b5d98") ||
                imageUrl.contains("photo-1517466787929-bc90951d0974") ||
                imageUrl.contains("photo-1612872087720-bb876e2e67d1") ||
                imageUrl.contains("photo-1541252260730-0412e8e2108e") ||
                imageUrl.contains("photo-1506197600528-5b297d02b8db") ||
                imageUrl.contains("photo-1461896836934-ffe607ba8211") ||
                imageUrl.contains("photo-1571902943202-507ec2618e8f") ||
                imageUrl.contains("photo-1540555700478-4be2892cef28")) ||
               imageUrl.contains("via.placeholder.com");
    }
    
    private boolean isSubstitutionImage(String imageUrl) {
        if (imageUrl == null) return false;
        return imageUrl.contains("picsum.photos") ||
               imageUrl.contains("placeholder.com") ||
               imageUrl.contains("example.com") ||
               (imageUrl.contains("seed/") && imageUrl.contains("picsum"));
    }
    
    private boolean isSportsRelated(News news) {
        if (news == null || (news.getTitle() == null && news.getContent() == null && news.getSummary() == null)) {
            return false;
        }
        
        String searchText = "";
        if (news.getTitle() != null) searchText += news.getTitle().toLowerCase() + " ";
        if (news.getContent() != null) searchText += news.getContent().toLowerCase() + " ";
        if (news.getSummary() != null) searchText += news.getSummary().toLowerCase() + " ";
        
        // Liste des mots-clés sportifs
        String[] sportsKeywords = {
            "football", "soccer", "basketball", "tennis", "hockey", "rugby", 
            "cycling", "cyclisme", "f1", "formula 1", "judo", "swimming", "natation",
            "athlétisme", "athletics", "golf", "volley", "boxe", "combat", "sport",
            "match", "équipe", "team", "player", "joueur", "coach", "entraîneur",
            "champion", "victoire", "victory", "défaite", "defeat", "score", "but",
            "goal", "tournament", "tournoi", "league", "ligue", "cup", "coupe",
            "olympique", "olympic", "world cup", "coupe du monde", "euro", "champions league",
            "nba", "nfl", "mlb", "nhl", "premier league", "laliga", "serie a", "bundesliga"
        };
        
        for (String keyword : sportsKeywords) {
            if (searchText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
}
