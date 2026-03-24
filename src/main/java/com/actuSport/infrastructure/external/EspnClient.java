package com.actuSport.infrastructure.external;

import com.actuSport.domain.model.Match;
import com.actuSport.domain.model.News;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class EspnClient {
    
    @Value("${external.api.espn.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    
    public EspnClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<Match> getLiveMatches(String sport) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/scoreboard")
                .queryParam("sport", sport)
                .queryParam("limit", 100)
                .build()
                .toUriString();
        
        EspnResponse response = restTemplate.getForObject(url, EspnResponse.class);
        
        return response != null ? response.getMatches() : List.of();
    }
    
    public List<News> getSportsNews(String sport) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/news")
                .queryParam("sport", sport)
                .queryParam("limit", 50)
                .build()
                .toUriString();
        
        EspnNewsResponse response = restTemplate.getForObject(url, EspnNewsResponse.class);
        
        return response != null ? response.getNews() : List.of();
    }
    
    public static class EspnResponse {
        @JsonProperty("scoreboard")
        private List<Match> matches;
        
        public List<Match> getMatches() {
            return matches;
        }
        
        public void setMatches(List<Match> matches) {
            this.matches = matches;
        }
    }
    
    public static class EspnNewsResponse {
        @JsonProperty("articles")
        private List<News> news;
        
        public List<News> getNews() {
            return news;
        }
        
        public void setNews(List<News> news) {
            this.news = news;
        }
    }
}
