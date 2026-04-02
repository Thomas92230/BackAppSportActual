package com.actuSport.infrastructure.websocket;

import com.actuSport.domain.model.Match;
import com.actuSport.application.service.MatchService;
import com.actuSport.application.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class LiveMatchController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    @Lazy
    private MatchService matchService;
    
    @Autowired
    @Lazy
    private NotificationService notificationService;
    
    @MessageMapping("/subscribe/matches")
    public void subscribeToMatches(@Payload String sportCode) {
        List<Match> liveMatches = matchService.getLiveMatchesBySport(sportCode);
        
        messagingTemplate.convertAndSend("/topic/matches/" + sportCode, liveMatches);
    }
    
    public void broadcastMatchUpdate(Match match) {
        String destination = "/topic/matches/" + match.getSport().getCode();
        messagingTemplate.convertAndSend(destination, match);
    }
    
    public void broadcastLiveMatches(String sportCode, List<Match> matches) {
        String destination = "/topic/matches/" + sportCode;
        messagingTemplate.convertAndSend(destination, matches);
    }
    
    public void broadcastScoreUpdate(Match match) {
        String destination = "/topic/score/" + match.getSport().getCode() + "/" + match.getId();
        messagingTemplate.convertAndSend(destination, match);
    }
    
    public void broadcastNotification(NotificationService.Notification notification) {
        String destination = "/topic/notifications/" + notification.getSportCode();
        messagingTemplate.convertAndSend(destination, notification);
    }
}
