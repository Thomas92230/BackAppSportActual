package com.actuSport.application.service;

import com.actuSport.domain.model.News;
import com.actuSport.domain.model.Sport;
import com.actuSport.domain.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private SportService sportService;

    @InjectMocks
    private NewsService newsService;

    private News news;
    private Sport sport;

    @BeforeEach
    void setUp() {
        sport = new Sport();
        sport.setCode("FOOT");
        sport.setName("Football");

        news = new News();
        news.setId(1L);
        news.setTitle("Test News");
        news.setSport(sport);
        news.setPublishedAt(LocalDateTime.now());
    }

    @Test
    void getNewsBySport_ShouldReturnNewsList() {
        when(sportService.findByCode("FOOT")).thenReturn(Optional.of(sport));
        when(newsRepository.findBySportOrderByPublishedAtDesc(sport)).thenReturn(Collections.singletonList(news));

        List<News> result = newsService.getNewsBySport("FOOT");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test News", result.get(0).getTitle());
    }

    @Test
    void getNewsBySportPaginated_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<News> page = new PageImpl<>(Collections.singletonList(news));

        when(sportService.findByCode("FOOT")).thenReturn(Optional.of(sport));
        when(newsRepository.findBySportOrderByPublishedAtDesc(sport, pageable)).thenReturn(page);

        Page<News> result = newsService.getNewsBySportPaginated("FOOT", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(newsRepository).findBySportOrderByPublishedAtDesc(sport, pageable);
    }

    @Test
    void saveNews_ShouldSaveAndReturnNews() {
        when(newsRepository.save(any(News.class))).thenReturn(news);

        News savedNews = newsService.saveNews(news);

        assertNotNull(savedNews);
        assertEquals("Test News", savedNews.getTitle());
        verify(newsRepository).save(news);
    }

    @Test
    void updateNewsList_ShouldSaveOnlyNewNews() {
        News existingNews = new News();
        existingNews.setTitle("Existing News");
        
        News newNews = new News();
        newNews.setTitle("New News");

        when(newsRepository.findByTitle("Existing News")).thenReturn(Optional.of(existingNews));
        when(newsRepository.findByTitle("New News")).thenReturn(Optional.empty());
        when(newsRepository.save(newNews)).thenReturn(newNews);

        newsService.updateNews(List.of(existingNews, newNews));

        verify(newsRepository, never()).save(existingNews);
        verify(newsRepository, times(1)).save(newNews);
    }
}
