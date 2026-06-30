package com.stie.controller;

import com.stie.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${app.base-url:}")
    private String appBaseUrl;

    @ModelAttribute("appNotifications")
    public List<NotificationService.AppNotification> getNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return notificationService.getNotificationsForUser(auth.getName());
        }
        return Collections.emptyList();
    }

    @ModelAttribute("uploadUrlPrefix")
    public String getUploadUrlPrefix() {
        return com.stie.config.AppConstants.FilePaths.UPLOAD_URL_PREFIX;
    }

    @ModelAttribute("baseUrl")
    public String getBaseUrl() {
        return appBaseUrl;
    }

    @ModelAttribute("notificationCount")
    public long getNotificationCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return notificationService.getUnreadCountForUser(auth.getName());
        }
        return 0;
    }
}

