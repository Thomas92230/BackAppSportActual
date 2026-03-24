package com.actuSport.application.service;

import com.actuSport.domain.model.Match;
import com.actuSport.domain.model.News;
import com.actuSport.domain.model.Sport;
import com.actuSport.infrastructure.external.ApiSportsClient;
import com.actuSport.infrastructure.external.EspnClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SportDataSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SportDataSyncService.class);
    
    private final ApiSportsClient apiSportsClient;
    private final EspnClient espnClient;
    private final MatchService matchService;
    private final NewsService newsService;
    private final SportService sportService;
    
    private final List<String> SUPPORTED_SPORTS = Arrays.asList(
        "football", "basketball", "tennis", "hockey", 
        "rugby", "cycling", "formula1", "judo", "swimming"
    );
    
    public SportDataSyncService(ApiSportsClient apiSportsClient, EspnClient espnClient,
                               MatchService matchService, NewsService newsService,
                               SportService sportService) {
        this.apiSportsClient = apiSportsClient;
        this.espnClient = espnClient;
        this.matchService = matchService;
        this.newsService = newsService;
        this.sportService = sportService;
    }
    
    @Scheduled(fixedRate = 30000)
    public void syncLiveMatches() {
        SUPPORTED_SPORTS.parallelStream().forEach(sport -> {
            try {
                syncLiveMatchesForSport(sport);
            } catch (Exception e) {
                logger.error("Error syncing live matches for sport: {}", sport, e);
            }
        });
    }
    
    @Scheduled(fixedRate = 300000)
    public void syncNews() {
        SUPPORTED_SPORTS.parallelStream().forEach(sport -> {
            try {
                syncNewsForSport(sport);
            } catch (Exception e) {
                logger.error("Error syncing news for sport: {}", sport, e);
            }
        });
    }
    
    @Async
    public CompletableFuture<Void> syncAllData() {
        return CompletableFuture.runAsync(() -> {
            syncLiveMatches();
            syncNews();
        });
    }
    
    private void syncLiveMatchesForSport(String sport) {
        List<Match> apiSportsMatches = apiSportsClient.getLiveMatches(sport);
        List<Match> espnMatches = espnClient.getLiveMatches(sport);
        
        matchService.updateMatches(apiSportsMatches);
        matchService.updateMatches(espnMatches);
        
        logger.info("Synced {} live matches for sport: {}", 
                   apiSportsMatches.size() + espnMatches.size(), sport);
    }
    
    private void syncNewsForSport(String sport) {
        List<News> apiSportsNews = apiSportsClient.getSportsNews(sport);
        List<News> espnNews = espnClient.getSportsNews(sport);
        
        newsService.updateNews(apiSportsNews);
        newsService.updateNews(espnNews);
        
        logger.info("Synced {} news articles for sport: {}", 
                   apiSportsNews.size() + espnNews.size(), sport);
    }
}
