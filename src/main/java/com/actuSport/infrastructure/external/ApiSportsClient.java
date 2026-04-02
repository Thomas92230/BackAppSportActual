package com.actuSport.infrastructure.external;

import com.actuSport.domain.model.Match;
import com.actuSport.domain.model.News;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApiSportsClient {
    
    @Value("${external.api.sports.key}")
    private String apiKey;
    
    @Value("${external.api.sports.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    
    public ApiSportsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<Match> getLiveMatches(String sport) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/fixtures")
                .queryParam("live", "all")
                .build().toUriString();
        ApiSportsResponse response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers()), ApiSportsResponse.class).getBody();
        return response != null ? response.getMatches() : List.of();
    }

    public List<Match> getMatchesByDate(String sport, LocalDateTime date) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/fixtures")
                .queryParam("date", date.toLocalDate())
                .build().toUriString();
        ApiSportsResponse response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers()), ApiSportsResponse.class).getBody();
        return response != null ? response.getMatches() : List.of();
    }

    public List<News> getSportsNews(String sport) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/news")
                .queryParam("sport", sport)
                .build().toUriString();
        ApiSportsNewsResponse response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers()), ApiSportsNewsResponse.class).getBody();
        return response != null ? response.getNews() : List.of();
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("x-apisports-key", apiKey);
        return h;
    }
    
    public static class ApiSportsResponse {
        @JsonProperty("response")
        private List<Match> matches;
        
        public List<Match> getMatches() {
            return matches;
        }
        
        public void setMatches(List<Match> matches) {
            this.matches = matches;
        }
    }
    
    public static class ApiSportsNewsResponse {
        @JsonProperty("response")
        private List<News> news;
        
        public List<News> getNews() {
            return news;
        }
        
        public void setNews(List<News> news) {
            this.news = news;
        }
    }
}
