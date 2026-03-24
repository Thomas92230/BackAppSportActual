package com.actuSport.infrastructure.aws;

import com.actuSport.application.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
public class AwsSnsService {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsSnsService.class);
    
    @Value("${aws.sns.topic-arn}")
    private String topicArn;
    
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    
    public AwsSnsService(SnsClient snsClient, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }
    
    public void sendNotification(NotificationService.Notification notification) {
        try {
            String message = objectMapper.writeValueAsString(notification);
            
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .subject("SportActual - " + notification.getType())
                    .messageGroupId("sport-notifications")
                    .build();
            
            PublishResponse response = snsClient.publish(request);
            
            logger.info("SNS notification sent successfully. MessageId: {}", response.messageId());
            
        } catch (Exception e) {
            logger.error("Failed to send SNS notification", e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }
    
    public void sendSimpleNotification(String subject, String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .subject(subject)
                    .build();
            
            PublishResponse response = snsClient.publish(request);
            
            logger.info("Simple SNS notification sent. MessageId: {}", response.messageId());
            
        } catch (Exception e) {
            logger.error("Failed to send simple SNS notification", e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }
}
