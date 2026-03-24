package com.actuSport.application.service;

import com.actuSport.domain.model.Match;
import com.actuSport.domain.model.News;
import com.actuSport.infrastructure.websocket.LiveMatchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final String NOTIFICATION_PREFIX = "notification:";
    private static final String USER_NOTIFICATIONS_PREFIX = "user_notifications:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private LiveMatchController liveMatchController;
    
    @Autowired
    private AwsSnsService awsSnsService;
    
    public void notifyMatchStarted(Match match) {
        String message = String.format("🚀 Match commencé : %s vs %s", 
            match.getHomeTeam().getName(), match.getAwayTeam().getName());
        
        sendNotification("MATCH_STARTED", message, match.getSport().getCode(), match.getId());
        
        logger.info("Match started notification sent for match: {}", match.getId());
    }
    
    public void notifyGoalScored(Match match, String teamName, Integer score) {
        String message = String.format("⚽ BUT ! %s marque ! Score: %d-%d", 
            teamName, match.getHomeScore(), match.getAwayScore());
        
        sendNotification("GOAL_SCORED", message, match.getSport().getCode(), match.getId());
        
        logger.info("Goal notification sent for match: {}, team: {}", match.getId(), teamName);
    }
    
    public void notifyMatchEnded(Match match) {
        String message = String.format("🏁 Match terminé : %s %d-%d %s", 
            match.getHomeTeam().getName(), match.getHomeScore(), 
            match.getAwayScore(), match.getAwayTeam().getName());
        
        sendNotification("MATCH_ENDED", message, match.getSport().getCode(), match.getId());
        
        logger.info("Match ended notification sent for match: {}", match.getId());
    }
    
    public void notifyBreakingNews(News news) {
        String message = String.format("📰 Dernière minute : %s", news.getTitle());
        
        sendNotification("BREAKING_NEWS", message, news.getSport().getCode(), null);
        
        logger.info("Breaking news notification sent for news: {}", news.getId());
    }
    
    public void notifyLiveScoreUpdate(Match match) {
        String message = String.format("📊 Score mis à jour : %s %d-%d %s (%s)", 
            match.getHomeTeam().getName(), match.getHomeScore(), 
            match.getAwayScore(), match.getAwayTeam().getName(), 
            match.getCurrentMinute());
        
        sendNotification("SCORE_UPDATE", message, match.getSport().getCode(), match.getId());
    }
    
    private void sendNotification(String type, String message, String sportCode, Long matchId) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setMessage(message);
        notification.setSportCode(sportCode);
        notification.setMatchId(matchId);
        notification.setTimestamp(LocalDateTime.now());
        
        String notificationKey = NOTIFICATION_PREFIX + sportCode;
        redisTemplate.opsForList().leftPush(notificationKey, notification);
        redisTemplate.expire(notificationKey, 24, TimeUnit.HOURS);
        
        liveMatchController.broadcastNotification(notification);
        
        try {
            awsSnsService.sendNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to send AWS SNS notification", e);
        }
    }
    
    public void subscribeUserToSport(String userId, String sportCode) {
        String key = USER_NOTIFICATIONS_PREFIX + userId;
        redisTemplate.opsForSet().add(key, sportCode);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }
    
    public void unsubscribeUserFromSport(String userId, String sportCode) {
        String key = USER_NOTIFICATIONS_PREFIX + userId;
        redisTemplate.opsForSet().remove(key, sportCode);
    }
    
    public static class Notification {
        private String type;
        private String message;
        private String sportCode;
        private Long matchId;
        private LocalDateTime timestamp;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getSportCode() {
            return sportCode;
        }
        
        public void setSportCode(String sportCode) {
            this.sportCode = sportCode;
        }
        
        public Long getMatchId() {
            return matchId;
        }
        
        public void setMatchId(Long matchId) {
            this.matchId = matchId;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}
