package com.salvatore.gymapp.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PushNotificationPayload {

    private NotificationData notification;

    @Getter
    @AllArgsConstructor
    public static class NotificationData {
        private String title;
        private String body;
        private String icon;
        private String badge;
        private Data data;
        private Boolean silent;
        private int[] vibrate;
    }

    @Getter
    @AllArgsConstructor
    public static class Data {
        private String url;
    }
}