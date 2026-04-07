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
            Optional<News> existingNews = newsRepository.findByTitle(news.getTitle());
            
            if (existingNews.isEmpty()) {
                saveNews(news);
                logger.info("Saved new news: {}", news.getTitle());
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
}
