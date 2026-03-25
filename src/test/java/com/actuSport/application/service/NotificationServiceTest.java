package com.actuSport.application.service;

import com.actuSport.domain.model.Match;
import com.actuSport.domain.model.Sport;
import com.actuSport.domain.model.Team;
import com.actuSport.infrastructure.aws.AwsSnsService;
import com.actuSport.infrastructure.websocket.LiveMatchController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;
    
    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private LiveMatchController liveMatchController;

    @Mock
    private AwsSnsService awsSnsService;

    @InjectMocks
    private NotificationService notificationService;

    private Match match;

    @BeforeEach
    void setUp() {
        Sport sport = new Sport();
        sport.setCode("FOOT");

        Team homeTeam = new Team();
        homeTeam.setName("Team A");

        Team awayTeam = new Team();
        awayTeam.setName("Team B");

        match = new Match();
        match.setId(1L);
        match.setSport(sport);
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setHomeScore(0);
        match.setAwayScore(0);
    }

    @Test
    void notifyMatchStarted_ShouldSendNotifications() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        notificationService.notifyMatchStarted(match);

        verify(listOperations).leftPush(eq("notification:FOOT"), any(NotificationService.Notification.class));
        verify(redisTemplate).expire(eq("notification:FOOT"), eq(24L), eq(TimeUnit.HOURS));
        verify(liveMatchController).broadcastNotification(any(NotificationService.Notification.class));
        verify(awsSnsService).sendNotification(any(NotificationService.Notification.class));
    }

    @Test
    void notifyGoalScored_ShouldSendNotifications() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        notificationService.notifyGoalScored(match, "Team A", 1);

        verify(listOperations).leftPush(eq("notification:FOOT"), any(NotificationService.Notification.class));
        verify(liveMatchController).broadcastNotification(any(NotificationService.Notification.class));
        verify(awsSnsService).sendNotification(any(NotificationService.Notification.class));
    }

    @Test
    void subscribeUserToSport_ShouldAddToRedisSet() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        notificationService.subscribeUserToSport("user123", "FOOT");
        
        verify(setOperations).add("user_notifications:user123", "FOOT");
        verify(redisTemplate).expire("user_notifications:user123", 30L, TimeUnit.DAYS);
    }
    
    @Test
    void unsubscribeUserFromSport_ShouldRemoveFromRedisSet() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        notificationService.unsubscribeUserFromSport("user123", "FOOT");
        
        verify(setOperations).remove("user_notifications:user123", "FOOT");
    }
}
