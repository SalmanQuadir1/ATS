package com.stie.controller;

import com.stie.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private NotificationService notificationService;

    @ModelAttribute("appNotifications")
    public List<NotificationService.AppNotification> getNotifications() {
        return notificationService.getNotifications();
    }

    @ModelAttribute("notificationCount")
    public long getNotificationCount() {
        return notificationService.getUnreadCount();
    }
}

