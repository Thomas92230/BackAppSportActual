package com.actuSport.interfaces.rest;

import com.actuSport.application.service.NewsService;
import com.actuSport.domain.model.News;
import com.actuSport.infrastructure.security.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsController.class)
@AutoConfigureMockMvc(addFilters = false)
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllNews_ShouldReturnPage() throws Exception {
        News news = new News();
        news.setId(1L);
        news.setTitle("Test News");
        Page<News> page = new PageImpl<>(Collections.singletonList(news));

        when(newsService.getAllNewsPaginated(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/news")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test News"));
    }

    @Test
    void getNewsById_ShouldReturnNews() throws Exception {
        News news = new News();
        news.setId(1L);
        news.setTitle("Test News");

        when(newsService.getNewsById(1L)).thenReturn(Optional.of(news));

        mockMvc.perform(get("/api/news/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test News"));
    }
}
